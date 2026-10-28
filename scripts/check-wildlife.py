#!/usr/bin/env python3
"""在一次性 Forge 1.20.1 服务端验证野外生成与掉落发行 JAR；不启动客户端、不下载依赖。"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import shutil
import signal
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parent.parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--geckolib', type=Path, required=True)
    args = parser.parse_args()
    library_root = ROOT / '.local/package-test/1.20.1'
    eula = library_root / 'eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text().splitlines():
        raise SystemExit('需要已确认 EULA 的本地独立测试安装 .local/package-test/1.20.1')
    gecko = args.geckolib.resolve(strict=True)
    props = dict(line.split('=', 1) for line in (ROOT / 'mod.properties').read_text().splitlines() if '=' in line)
    artifact = ROOT / f"versions/1.20.1/build/libs/pinkkicore-forge-1.20.1-{props['mod_version']}.jar"
    probe = ROOT / 'versions/1.20.1/build/libs/wildlife-probe.jar'
    for path in (artifact, probe):
        if not path.is_file():
            raise SystemExit(f'先执行 ./scripts/mod.sh 1.20.1 build wildlifeProbeJar -PwildlifeProbe：缺少 {path.name}')
    run = ROOT / '.local/wildlife-tests' / str(time.time_ns())
    mods = run / 'mods'
    mods.mkdir(parents=True)
    (run / 'libraries').symlink_to(library_root / 'libraries', target_is_directory=True)
    shutil.copy2(eula, run / 'eula.txt')
    for path in (artifact, probe, gecko):
        shutil.copy2(path, mods / path.name)
    (run / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25587\nlevel-name=wildlife-test-world\n'
        'level-seed=12345\nview-distance=2\nsimulation-distance=2\nmax-players=1\n'
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","features":false,"lakes":false,"structure_overrides":[]}\nlevel-type=minecraft:flat\ngenerate-structures=false\ndifficulty=normal\nenable-rcon=false\nenable-query=false\nonline-mode=false\n')
    java = ROOT / '.local/jdk-17/Contents/Home/bin/java'
    forge_args = run / 'libraries/net/minecraftforge/forge/1.20.1-47.4.28/unix_args.txt'
    process = subprocess.Popen([str(java), '-Xms256M', '-Xmx1536M', '-Djava.awt.headless=true',
        '@' + str(forge_args), '--nogui'], cwd=run, stdin=subprocess.PIPE,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1, start_new_session=True)
    lines = queue.Queue()
    def read():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)
    threading.Thread(target=read, daemon=True).start()
    ready = passed = stopped = sent = errors = False
    external_auth_unavailable = False
    deadline = time.monotonic() + 240
    try:
        with (run / 'console.log').open('w') as log:
            while time.monotonic() < deadline:
                try:
                    line = lines.get(timeout=1)
                except queue.Empty:
                    continue
                if line is None:
                    break
                log.write(line)
                log.flush()
                ready |= 'Done (' in line
                passed |= 'WILDLIFE_PROBE PASS' in line
                stopped |= 'Stopping server' in line
                # This loopback/offline probe does not authenticate players. Keep the exact
                # optional Mojang public-key fetch failure visible, separate from mod errors.
                auth_error = ('[Yggdrasil Key Fetcher/ERROR] [mojang/YggdrasilServicesKeyInfo]: '
                              'Failed to request yggdrasil public key') in line
                external_auth_unavailable |= auth_error
                errors |= ('/ERROR]' in line and not auth_error) or 'WILDLIFE_PROBE FAIL' in line
                if 'WILDLIFE' in line:
                    print(line.strip(), flush=True)
                if (passed or 'WILDLIFE_PROBE FAIL' in line) and not sent:
                    process.stdin.write('stop\n')
                    process.stdin.flush()
                    sent = True
            else:
                raise TimeoutError(f'测试超时，日志：{run / "console.log"}')
        code = process.wait(timeout=30)
        result = dict(passed=code == 0 and ready and passed and stopped and not errors,
            exit_code=code, ready=ready, probe_pass=passed, stopped=stopped, errors=errors,
            external_auth_unavailable=external_auth_unavailable, geckolib=gecko.name, artifact_sha256=hashlib.sha256(artifact.read_bytes()).hexdigest())
        (run / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result), run.relative_to(ROOT))
        if not result['passed']:
            raise SystemExit(1)
    finally:
        if process.poll() is None:
            try:
                process.stdin.write('stop\n'); process.stdin.flush()
                process.wait(timeout=20)
            except (OSError, subprocess.TimeoutExpired):
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait(timeout=10)
        process.stdin.close()
        process.stdout.close()


if __name__ == '__main__':
    main()

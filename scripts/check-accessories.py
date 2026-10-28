#!/usr/bin/env python3
"""在一次性 Forge 1.20.1 服务端验证 Curios 饰品发行 JAR；不启动客户端、不下载依赖。"""
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
    parser.add_argument('--extra-mod', type=Path, action='append', default=[], help='Optional local mod JAR for isolated coexistence checks')
    args = parser.parse_args()
    library_root = ROOT / '.local/package-test/1.20.1'
    eula = library_root / 'eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text().splitlines():
        raise SystemExit('需要已确认 EULA 的本地独立测试安装 .local/package-test/1.20.1')
    gecko = args.geckolib.resolve(strict=True)
    props = dict(line.split('=', 1) for line in (ROOT / 'mod.properties').read_text().splitlines() if '=' in line)
    artifact = ROOT / f"versions/1.20.1/build/libs/pinkkicore-forge-1.20.1-{props['mod_version']}.jar"
    probe = ROOT / 'versions/1.20.1/build/libs/accessory-probe.jar'
    for path in (artifact, probe):
        if not path.is_file():
            raise SystemExit(f'先执行 ./scripts/mod.sh 1.20.1 build accessoryProbeJar -PaccessoryProbe：缺少 {path.name}')
    run = ROOT / '.local/accessory-tests' / str(time.time_ns())
    mods = run / 'mods'
    mods.mkdir(parents=True)
    (run / 'libraries').symlink_to(library_root / 'libraries', target_is_directory=True)
    shutil.copy2(eula, run / 'eula.txt')
    for path in (artifact, probe, gecko, *(path.resolve(strict=True) for path in args.extra_mod)):
        shutil.copy2(path, mods / path.name)
    (run / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25587\nlevel-name=accessory-test-world\n'
        'level-seed=12345\nview-distance=2\nsimulation-distance=2\nmax-players=1\n'
        'difficulty=hard\nenable-rcon=false\nenable-query=false\nonline-mode=false\n')
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
                passed |= 'ACCESSORY_PROBE PASS' in line
                stopped |= 'Stopping server' in line
                errors |= '/ERROR]' in line or 'ACCESSORY_PROBE FAIL' in line
                if 'ACCESSORY' in line:
                    print(line.strip(), flush=True)
                if (passed or 'ACCESSORY_PROBE FAIL' in line) and not sent:
                    process.stdin.write('stop\n')
                    process.stdin.flush()
                    sent = True
            else:
                raise TimeoutError(f'测试超时，日志：{run / "console.log"}')
        code = process.wait(timeout=30)
        result = dict(passed=code == 0 and ready and passed and stopped and not errors,
            exit_code=code, ready=ready, probe_pass=passed, stopped=stopped, errors=errors,
            geckolib=gecko.name, extra_mods=[path.name for path in args.extra_mod], artifact_sha256=hashlib.sha256((mods / artifact.name).read_bytes()).hexdigest())
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

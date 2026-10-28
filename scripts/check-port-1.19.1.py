#!/usr/bin/env python3
"""Run a reobfuscated 1.19.1 probe in a fresh loopback-only release-JAR server."""
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
PROBES = {'bingwa': 'BINGWA', 'workbench': 'WORKBENCH', 'guaba-effect': 'GUABA_EFFECT',
          'pinkki': 'PINKKI', 'guide': 'GUIDE', 'thunder-guaba': 'THUNDER_GUABA',
          'pink-ore': 'PINK_ORE', 'accessory': 'ACCESSORY', 'tetra': 'TETRA'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('probe', choices=PROBES)
    parser.add_argument('--geckolib', type=Path, required=True)
    parser.add_argument('--extra-mod', type=Path, action='append', default=[])
    parser.add_argument('--npc', choices=['pinkki', 'guaba_yingrong'], default='pinkki')
    args = parser.parse_args()
    installation = ROOT / '.local/package-test/1.19.1'
    eula = ROOT / 'versions/1.19.1/run/server/eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text().splitlines():
        raise SystemExit('需要已有的 1.19.1 独立测试 EULA 确认。')
    properties = dict(line.split('=', 1) for line in (ROOT / 'mod.properties').read_text().splitlines() if '=' in line)
    artifact = ROOT / f"versions/1.19.1/build/libs/pinkkicore-forge-1.19.1-{properties['mod_version']}.jar"
    probe = ROOT / f'versions/1.19.1/build/libs/{args.probe}-probe.jar'
    run = ROOT / '.local/porting-1.19.1/runtime' / f'{args.probe}-{time.time_ns()}'
    mods = run / 'mods'
    mods.mkdir(parents=True)
    (run / 'libraries').symlink_to(installation / 'libraries', target_is_directory=True)
    shutil.copy2(eula, run / 'eula.txt')
    inputs = [artifact, probe, args.geckolib.resolve(strict=True), *[p.resolve(strict=True) for p in args.extra_mod]]
    for source in inputs:
        shutil.copy2(source, mods / source.name)
    (run / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25589\nlevel-name=probe-world\nlevel-seed=12345\n'
        'view-distance=2\nsimulation-distance=2\nmax-players=1\ndifficulty=normal\n'
        'enable-rcon=false\nenable-query=false\nonline-mode=false\n')
    command = [str(ROOT / '.local/jdk-17/Contents/Home/bin/java'), '-Xms256M', '-Xmx1536M',
               '-Djava.awt.headless=true', '-Dsun.net.client.defaultConnectTimeout=5000',
               '-Dsun.net.client.defaultReadTimeout=10000', f'-Dpinkkicore.test.npc={args.npc}',
               '@libraries/net/minecraftforge/forge/1.19.1-42.0.9/unix_args.txt', '--nogui']
    process = subprocess.Popen(command, cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, bufsize=1, start_new_session=True)
    lines = queue.Queue()
    def read():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)
    threading.Thread(target=read, daemon=True).start()
    prefix = PROBES[args.probe] + '_PROBE'
    ready = passed = stopped = sent = failed = False
    errors = []
    deadline = time.monotonic() + 300
    try:
        with (run / 'console.log').open('w') as log:
            while time.monotonic() < deadline:
                try:
                    line = lines.get(timeout=1)
                except queue.Empty:
                    continue
                if line is None:
                    break
                log.write(line); log.flush()
                ready |= 'Done (' in line
                passed |= prefix + ' PASS' in line
                failed |= '_PROBE FAIL' in line
                stopped |= 'Stopping server' in line
                if '/ERROR]' in line:
                    errors.append(line.strip())
                if prefix in line or ' PASS' in line or '/ERROR]' in line:
                    print(line.strip(), flush=True)
                if (passed or failed) and not sent:
                    process.stdin.write('stop\n'); process.stdin.flush(); sent = True
            else:
                raise TimeoutError(f'Probe timeout: {run}')
        code = process.wait(timeout=30)
        result = dict(passed=code == 0 and ready and passed and stopped and not failed and not errors,
                      exit_code=code, ready=ready, probe_pass=passed, stopped=stopped, errors=errors,
                      npc=args.npc, inputs={p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs})
        (run / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result), run.relative_to(ROOT), flush=True)
        if not result['passed']:
            raise SystemExit(1)
    finally:
        if process.poll() is None:
            try:
                process.stdin.write('stop\n'); process.stdin.flush(); process.wait(timeout=20)
            except (OSError, subprocess.TimeoutExpired):
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL); process.wait(timeout=10)
        process.stdin.close(); process.stdout.close()


if __name__ == '__main__':
    main()

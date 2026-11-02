#!/usr/bin/env python3
"""Run a 1.21.1 probe in a fresh loopback-only release-JAR server."""
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
    parser.add_argument('--extra-mod', type=Path, action='append', default=[])
    parser.add_argument('--host-baseline', action='store_true', help='Run only supplied host mods, record their existing errors, stop when ready')
    parser.add_argument('--known-host-errors', type=Path, help='result.json from a matching host-only baseline')
    parser.add_argument('--neo-version', help='Installed runtime version; defaults to the configured NeoForge SDK')
    parser.add_argument('--installation', type=Path, help='Isolated server installation containing libraries/')
    parser.add_argument('--java-home', type=Path, default=None)
    args = parser.parse_args()
    if args.neo_version is None:
        target_properties = dict(line.split('=', 1) for line in (ROOT / 'versions/1.21.1/gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
        args.neo_version = target_properties['neo_version']
    installation = (args.installation or ROOT / f'.local/package-test/1.21.1-{args.neo_version}').resolve(strict=True)
    eula = ROOT / 'versions/1.21.1/run/server/eula.txt'
    if not eula.is_file() or 'eula=true' not in eula.read_text().splitlines():
        raise SystemExit('需要已有的 1.21.1 独立测试 EULA 确认。')
    properties = dict(line.split('=', 1) for line in (ROOT / 'mod.properties').read_text().splitlines() if '=' in line)
    artifact = ROOT / f"versions/1.21.1/build/libs/pinkkicore-neoforge-1.21.1-{properties['mod_version']}.jar"
    probe = ROOT / 'versions/1.21.1/build/libs/port-1.21.1-probe.jar'
    run = ROOT / '.local/porting-1.21.1/runtime' / f'probe-{time.time_ns()}'
    mods = run / 'mods'
    mods.mkdir(parents=True)
    (run / 'libraries').symlink_to(installation / 'libraries', target_is_directory=True)
    shutil.copy2(eula, run / 'eula.txt')
    inputs = ([] if args.host_baseline else [artifact, probe]) + [args.geckolib.resolve(strict=True), *[p.resolve(strict=True) for p in args.extra_mod]]
    hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    known_errors = set()
    if args.known_host_errors:
        baseline = json.loads(args.known_host_errors.read_text())
        assert baseline['host_baseline'] and baseline['ready'] and baseline['stopped'] and baseline['exit_code'] == 0
        assert all(hashes.get(name) == digest for name, digest in baseline['inputs'].items()), 'Host baseline JAR hashes differ'
        known_errors = {line.split(']: ', 1)[-1] for line in baseline['errors']}

    for source in inputs:
        shutil.copy2(source, mods / source.name)
    (run / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25589\nlevel-name=probe-world\nlevel-seed=12345\n'
        'view-distance=2\nsimulation-distance=2\nmax-players=1\ndifficulty=normal\n'
        'enable-rcon=false\nenable-query=false\nonline-mode=false\n')
    java_home = args.java_home or (Path(os.environ['JAVA21_HOME']) if 'JAVA21_HOME' in os.environ else None)
    if java_home is None:
        local = ROOT / '.local/jdk-21/Contents/Home'
        if (local / 'bin/java').is_file():
            java_home = local
        else:
            java_home = Path(subprocess.check_output(['/usr/libexec/java_home', '-v', '21'], text=True).strip())
    command = [str(java_home / 'bin/java'), '-Xms256M', '-Xmx2G',
               '-Djava.awt.headless=true', '-Dsun.net.client.defaultConnectTimeout=5000',
               '-Dsun.net.client.defaultReadTimeout=10000',
               f'@libraries/net/neoforged/neoforge/{args.neo_version}/unix_args.txt', '--nogui']
    process = subprocess.Popen(command, cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, bufsize=1, start_new_session=True)
    lines = queue.Queue()
    def read():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)
    threading.Thread(target=read, daemon=True).start()
    prefix = 'PORT_PROBE'
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
                if (passed or failed or args.host_baseline and ready) and not sent:
                    process.stdin.write('stop\n'); process.stdin.flush(); sent = True
            else:
                raise TimeoutError(f'Probe timeout: {run}')
        code = process.wait(timeout=30)
        unexpected = [line for line in errors if line.split(']: ', 1)[-1] not in known_errors]
        success = code == 0 and ready and stopped and not failed and (args.host_baseline or passed and not unexpected)
        result = dict(passed=success, host_baseline=args.host_baseline, unexpected_errors=unexpected,
                      exit_code=code, ready=ready, probe_pass=passed, stopped=stopped, errors=errors,
                      inputs=hashes)
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

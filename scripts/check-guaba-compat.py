#!/usr/bin/env python3
"""固定 1.20.1 发行版本的瓜巴兼容无头探针；不下载文件，不启动客户端。"""
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
import zipfile

ROOT = Path(__file__).resolve().parent.parent
GROUPS = {
    'silentgear': ['silent-gear-1.20.1-3.6.7.jar', 'silent-lib-1.20.1-8.0.0.jar'],
    'tconstruct': ['TConstruct-1.20.1-3.11.2.166.jar', 'Mantle-1.20.1-1.11.104.jar'],
    'tetra': ['tetra-1.20.1-6.17.0.jar', 'mutil-1.20.1-6.3.0.jar'],
}
CASES = {'none': [], **{key: [key] for key in GROUPS}, 'all': list(GROUPS)}


def compile_probe(research, libraries, java_home):
    # Compile only against renamed server classes and the selected Forge baseline.
    jars = [path for path in libraries.rglob('*.jar')
            if '47.1.0' not in str(path)
            and ('/net/minecraft/server/' not in path.as_posix() or path.name.endswith('-srg.jar'))]
    jars.extend(research.glob('*.jar'))
    classes = research / 'probe-classes'
    classes.mkdir()
    subprocess.run([
        str(java_home / 'bin/javac'), '-proc:none', '--release', '17',
        '-cp', os.pathsep.join(map(str, jars)), '-d', str(classes),
        str(ROOT / 'scripts/tests/compat_probe/GuabaCompatProbe.java'),
    ], check=True)
    probe = research / 'compat-probe.jar'
    with zipfile.ZipFile(probe, 'w') as archive:
        for path in classes.rglob('*.class'):
            archive.write(path, path.relative_to(classes))
        archive.writestr('META-INF/mods.toml', '''modLoader="javafml"
loaderVersion="[47,)"
license="MIT"
[[mods]]
modId="pinkkicore_compat_probe"
version="1.0.0"
displayName="Guaba compatibility test probe"
''')
    return probe


def stop(process):
    if process.poll() is None:
        try:
            process.stdin.write('stop\n')
            process.stdin.flush()
            process.wait(timeout=15)
        except (OSError, subprocess.TimeoutExpired):
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait(timeout=10)
    process.stdin.close()
    process.stdout.close()


def check(name, research, libraries, java_home, artifact, probe, geckolib, eula):
    run = research / 'runtime' / name
    run.mkdir(parents=True)
    (run / 'libraries').symlink_to(libraries, target_is_directory=True)
    mods = run / 'mods'
    mods.mkdir()
    shutil.copy2(eula, run / 'eula.txt')
    dependencies = [research / filename for group in CASES[name] for filename in GROUPS[group]]
    for source in [artifact, probe, geckolib, *dependencies]:
        shutil.copy2(source, mods / source.name)
    (run / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25579\nlevel-name=compat-test-world\n'
        'level-seed=12345\nview-distance=2\nsimulation-distance=2\nmax-players=1\n'
        'enable-rcon=false\nenable-query=false\nonline-mode=false\n')
    forge_args = run / 'libraries/net/minecraftforge/forge/1.20.1-47.4.28/unix_args.txt'
    process = subprocess.Popen([
        str(java_home / 'bin/java'), '-Xms256M', '-Xmx1536M', '-Djava.awt.headless=true',
        '-Dsun.net.client.defaultConnectTimeout=5000', '-Dsun.net.client.defaultReadTimeout=10000',
        '@' + str(forge_args), '--nogui',
    ], cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
       text=True, bufsize=1, start_new_session=True)
    lines = queue.Queue()

    def read():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)

    threading.Thread(target=read, daemon=True).start()
    ready = passed = stopped = sent = errors = auth_key_unavailable = False
    log = run / 'console.log'
    deadline = time.monotonic() + 180
    try:
        with log.open('w') as output:
            while time.monotonic() < deadline:
                try:
                    line = lines.get(timeout=1)
                except queue.Empty:
                    continue
                if line is None:
                    break
                output.write(line)
                output.flush()
                ready |= 'Done (' in line
                passed |= 'GUABA_COMPAT_PROBE PASS' in line
                stopped |= 'Stopping server' in line
                # This offline test does not exercise authenticated multiplayer. Record the
                # unrelated background key fetch separately; all other ERROR lines fail.
                auth_failure = ('Yggdrasil Key Fetcher/ERROR]' in line
                                and '[mojang/YggdrasilServicesKeyInfo]' in line
                                and 'Failed to request yggdrasil public key' in line)
                auth_key_unavailable |= auth_failure
                errors |= ('/ERROR]' in line and not auth_failure) or 'GUABA_COMPAT_PROBE FAIL' in line
                if 'GUABA_COMPAT_PROBE' in line or 'GUABA ' in line:
                    print(name, line.strip(), flush=True)
                if (passed or 'GUABA_COMPAT_PROBE FAIL' in line) and not sent:
                    process.stdin.write('stop\n')
                    process.stdin.flush()
                    sent = True
            else:
                raise TimeoutError(f'{name}: {log}')
        code = process.wait(timeout=30)
        return {
            'case': name, 'passed': code == 0 and ready and passed and stopped and not errors,
            'exit_code': code, 'ready': ready, 'probe_pass': passed, 'stopped': stopped,
            'errors': errors, 'auth_key_unavailable': auth_key_unavailable, 'log': str(log.relative_to(ROOT)),
            'artifact_sha256': hashlib.sha256(artifact.read_bytes()).hexdigest(),
        }
    finally:
        stop(process)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--libraries', type=Path, required=True, help='已安装的 Forge 47.4.28 libraries 目录')
    parser.add_argument('--dependencies', type=Path, required=True, help='存放 dependencies.json 中六个原版 JAR 的目录')
    parser.add_argument('--geckolib', type=Path, required=True, help='匹配 1.20.1 Forge 的 GeckoLib 4.4.9 JAR')
    parser.add_argument('--java-home', type=Path, required=True, help='JDK 17 目录')
    parser.add_argument('--cases', default=','.join(CASES))
    args = parser.parse_args()
    selected = args.cases.split(',')
    if not selected or len(selected) != len(set(selected)) or any(name not in CASES for name in selected):
        parser.error('Unknown or duplicate case')
    java_home = args.java_home.resolve()
    version = subprocess.check_output([str(java_home / 'bin/java'), '-version'], stderr=subprocess.STDOUT, text=True)
    if 'version "17.' not in version:
        parser.error('JDK 17 required')
    eula = ROOT / 'versions/1.20.1/run/server/eula.txt'
    if not eula.exists() or 'eula=true' not in eula.read_text().splitlines():
        parser.error('Read and accept the Minecraft EULA in the local test server first')
    research = ROOT / '.local/guaba-compat-tests' / str(time.time_ns())
    research.mkdir(parents=True)
    manifest = json.loads((ROOT / 'scripts/tests/compat_probe/dependencies.json').read_text())
    for entry in manifest:
        path = args.dependencies / entry['file']
        if hashlib.sha512(path.read_bytes()).hexdigest() != entry['sha512']:
            raise ValueError(f'Hash mismatch: {path}')
        shutil.copy2(path, research / path.name)
    libraries = args.libraries.resolve()
    probe = compile_probe(research, libraries, java_home)
    props = dict(line.split('=', 1) for line in (ROOT / 'mod.properties').read_text().splitlines()
                 if '=' in line and not line.startswith('#'))
    artifact = ROOT / 'versions/1.20.1/build/libs' / f"{props['mod_id']}-forge-1.20.1-{props['mod_version']}.jar"
    # Use one immutable artifact across the matrix even if another local build runs.
    snapshot = research / artifact.name
    before = hashlib.sha256(artifact.read_bytes()).hexdigest()
    shutil.copy2(artifact, snapshot)
    if hashlib.sha256(snapshot.read_bytes()).hexdigest() != before:
        raise ValueError('Artifact changed while copying; retry the test')
    artifact = snapshot
    results = []
    for name in selected:
        result = check(name, research, libraries, java_home, artifact, probe, args.geckolib.resolve(), eula)
        results.append(result)
        (research / 'matrix-results.json').write_text(json.dumps(results, indent=2) + '\n')
        print(json.dumps(result), flush=True)
    if not all(result['passed'] for result in results):
        raise SystemExit('Compatibility probe failed; inspect the saved logs')


if __name__ == '__main__':
    main()

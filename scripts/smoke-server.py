#!/usr/bin/env python3
"""Start a development server, require mod load + server ready, then stop cleanly."""
import argparse
import os
from pathlib import Path
import queue
import signal
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parent.parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("minecraft", choices=["1.20.1", "1.21.1", "1.19.1"])
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    run_dir = ROOT / "versions" / args.minecraft / "run/server"
    eula = run_dir / "eula.txt"
    if not eula.exists() or "eula=true" not in eula.read_text().splitlines():
        raise SystemExit("Read Minecraft EULA and set eula=true in the local server directory first.")
    log_dir = ROOT / ".local"
    log_dir.mkdir(exist_ok=True)
    log_path = log_dir / f"smoke-server-{args.minecraft}.log"
    process = subprocess.Popen(
        # Dedicated servers do not render client assets. Skip that download-only task.
        [str(ROOT / "scripts/mod.sh"), args.minecraft, "runServer", "-x", "downloadAssets",
         "--project-cache-dir", str(log_dir / "smoke-gradle" / args.minecraft), "--console=plain"],
        cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, bufsize=1, start_new_session=True,
    )
    lines = queue.Queue()

    def read_output():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)

    threading.Thread(target=read_output, daemon=True).start()
    loaded = ready = stopped = stop_sent = False
    deadline = time.monotonic() + args.timeout
    try:
        with log_path.open("w") as log:
            while time.monotonic() < deadline:
                try:
                    line = lines.get(timeout=1)
                except queue.Empty:
                    continue
                if line is None:
                    break
                print(line, end="", flush=True)
                log.write(line)
                log.flush()
                loaded |= "[pinkkicore] Mod loaded successfully." in line
                ready |= 'Done (' in line and 'For help, type' in line
                stopped |= 'Stopping server' in line
                if loaded and ready and process.stdin and not stop_sent:
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    stop_sent = True
            else:
                raise TimeoutError(f"Server smoke test timed out: {log_path}")
        code = process.wait(timeout=30)
        if code != 0 or not (loaded and ready and stopped):
            raise RuntimeError(f"Smoke failed: exit={code}, loaded={loaded}, ready={ready}, stopped={stopped}. {log_path}")
        print(f"PASS {args.minecraft}: mod loaded, server ready, clean stop. {log_path}")
    finally:
        if process.stdin and not process.stdin.closed:
            process.stdin.close()
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)


if __name__ == "__main__":
    main()

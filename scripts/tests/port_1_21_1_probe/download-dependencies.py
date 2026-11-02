#!/usr/bin/env python3
"""Fetch the pinned headless test dependencies and verify their publisher SHA512."""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[3]

def main():
    manifest = json.loads(Path(__file__).with_name('dependencies.json').read_text())
    output = ROOT / '.local/porting-1.21.1/downloads'
    output.mkdir(parents=True, exist_ok=True)
    for value in manifest.values():
        destination = output / value['file']
        if not destination.is_file():
            data = urllib.request.urlopen(value['url'], timeout=120).read()
            if hashlib.sha512(data).hexdigest() != value['sha512']:
                raise ValueError('Download hash mismatch: ' + value['file'])
            destination.write_bytes(data)
        if hashlib.sha512(destination.read_bytes()).hexdigest() != value['sha512']:
            raise ValueError('Local hash mismatch: ' + value['file'])
        print('OK ' + value['file'])

if __name__ == '__main__':
    main()

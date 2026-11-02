#!/usr/bin/env python3
"""Check declared dependency intervals against captured modpack versions; no game or network."""
import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
EXPECTED_LOADERS = {'1.19.1': 'forge', '1.20.1': 'forge', '1.21.1': 'neoforge'}

def properties(version):
    return dict(line.split('=', 1) for line in (ROOT / 'versions' / version / 'gradle.properties').read_text().splitlines()
                if '=' in line and not line.startswith('#'))

def numeric_version(value):
    """Only compare stable numeric Maven versions; build metadata does not change their floor."""
    base = value.split('+', 1)[0]
    if not re.fullmatch(r'\d+(?:\.\d+)*', base):
        raise ValueError('Version requires Maven-specific evaluation: ' + value)
    pieces = [int(p) for p in base.split('.')]
    while pieces and pieces[-1] == 0:
        pieces.pop()
    return tuple(pieces)

def contains(interval, version):
    match = re.fullmatch(r'([\[(])([^,]*),([^\])]*)([\])])', interval)
    if not match:
        raise ValueError('Unsupported interval: ' + interval)
    value = numeric_version(version)
    left, low, high, right = match.groups()
    return (not low or value > numeric_version(low) or left == '[' and value == numeric_version(low)) and (
        not high or value < numeric_version(high) or right == ']' and value == numeric_version(high))

def inspect(sample, config):
    mc = sample['minecraft']
    if mc not in EXPECTED_LOADERS or sample['loader'] != EXPECTED_LOADERS[mc] or sample.get('excluded_reason'):
        return {'status': 'excluded', 'sample': sample['id'], 'reason': sample.get('excluded_reason', 'Exact Minecraft/loader mismatch')}
    loader = sample['loader']
    lower = config.get('neo_min_version' if loader == 'neoforge' else 'forge_min_version', config.get('forge_version'))
    upper = '21.2' if loader == 'neoforge' else str(int(lower.split('.')[0]) + 1)
    conflicts = []
    if not contains(f'[{lower},{upper})', sample['loader_version']):
        conflicts.append(f'{loader} {sample["loader_version"]} below/above [{lower},{upper})')
    for mod, version in sample['mods'].items():
        interval = config.get(mod + '_version_range')
        if interval and not contains(interval, version):
            conflicts.append(f'{mod} {version} outside {interval}')
    return {'status': 'conflict' if conflicts else 'accepted', 'sample': sample['id'], 'conflicts': conflicts,
            'needs_geckolib': 'geckolib' not in sample['mods'],
            'boundary': 'Declared numeric version intervals only; not full modpack runtime compatibility'}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--samples', type=Path, default=ROOT / 'docs/compatibility/modpack-samples.json')
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    data = json.loads(args.samples.read_text())
    results = [inspect(s, properties(s['minecraft'])) for s in data['samples'] if s['minecraft'] in EXPECTED_LOADERS]
    for row in results:
        print(f'{row["status"]}: {row["sample"]}' + ('; ' + '; '.join(row['conflicts']) if row.get('conflicts') else ''))
    if args.output:
        args.output.write_text(json.dumps(results, ensure_ascii=False, indent=2) + '\n')
    print('Missing GeckoLib means an external prerequisite must be installed; absent optional dependencies do not impose a version gate.')
    if args.check and any(r['status'] == 'conflict' for r in results):
        raise SystemExit(1)

if __name__ == '__main__':
    main()

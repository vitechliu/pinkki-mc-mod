#!/usr/bin/env python3
"""Verify release plugin JEI symbols against real published API JARs, without a game client."""
import argparse
import io
from pathlib import Path
import struct
import zipfile

ROOT = Path(__file__).resolve().parent.parent
PREFIX = 'io/github/vitech/pinkkicore/forge/client/jei/'


def read_class(raw):
    stream = io.BytesIO(raw)
    def u1(): return struct.unpack('>B', stream.read(1))[0]
    def u2(): return struct.unpack('>H', stream.read(2))[0]
    def u4(): return struct.unpack('>I', stream.read(4))[0]
    assert u4() == 0xCAFEBABE
    u2(); u2()
    pool = [None] * u2()
    i = 1
    while i < len(pool):
        tag = u1()
        if tag == 1: value = stream.read(u2()).decode('utf-8', errors='replace')
        elif tag in (3, 4): value = stream.read(4)
        elif tag in (5, 6):
            value = stream.read(8); pool[i] = (tag, value); i += 2; continue
        elif tag in (7, 8, 16, 19, 20): value = u2()
        elif tag in (9, 10, 11, 12, 17, 18): value = (u2(), u2())
        elif tag == 15: value = (u1(), u2())
        else: raise AssertionError(f'Unknown constant pool tag {tag}')
        pool[i] = (tag, value); i += 1
    def utf(index): return pool[index][1]
    def class_name(index): return utf(pool[index][1]) if index else None
    def skip_attributes():
        for _ in range(u2()):
            u2(); stream.read(u4())
    u2(); name = class_name(u2()); parent = class_name(u2())
    parents = [class_name(u2()) for _ in range(u2())]
    if parent: parents.append(parent)
    members = {'field': set(), 'method': set()}
    for kind in ('field', 'method'):
        for _ in range(u2()):
            u2(); member, descriptor = utf(u2()), utf(u2())
            members[kind].add((member, descriptor)); skip_attributes()
    references = []
    for entry in pool:
        if entry and entry[0] in (9, 10, 11):
            owner, name_type = entry[1]
            member, descriptor = pool[name_type][1]
            references.append(('field' if entry[0] == 9 else 'method', class_name(owner), utf(member), utf(descriptor)))
    return dict(name=name, parents=parents, members=members, references=references,
                classes=[class_name(index) for index, entry in enumerate(pool) if entry and entry[0] == 7])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api', type=Path, action='append', required=True, help='JEI release/API JAR; repeat for common + Forge APIs')
    parser.add_argument("--minecraft", choices=["1.20.1", "1.19.1"], default="1.20.1")
    args = parser.parse_args()
    props = dict(line.split('=', 1) for line in (ROOT/'mod.properties').read_text().splitlines() if '=' in line)
    jar = ROOT/f"versions/{args.minecraft}/build/libs/pinkkicore-forge-{args.minecraft}-{props['mod_version']}.jar"
    api = {}
    for path in args.api:
        with zipfile.ZipFile(path) as archive:
            api.update({name[:-6]: read_class(archive.read(name)) for name in archive.namelist() if name.startswith('mezz/jei/api/') and name.endswith('.class')})
    assert api, 'No JEI API classes supplied'
    def resolves(kind, owner, name, descriptor, visited=None):
        visited = set() if visited is None else visited
        if owner in visited or owner not in api: return False
        visited.add(owner)
        entry = api[owner]
        return (name, descriptor) in entry['members'][kind] or any(resolves(kind, parent, name, descriptor, visited) for parent in entry['parents'])
    checked = 0
    with zipfile.ZipFile(jar) as archive:
        plugins = [name for name in archive.namelist() if name.startswith(PREFIX) and name.endswith('.class')]
        assert plugins, 'Missing JEI integration'
        assert b'Lmezz/jei/api/JeiPlugin;' in archive.read(PREFIX+'GuabaJeiPlugin.class'), 'Missing JEI discovery annotation'
        for path in plugins:
            cls = read_class(archive.read(path))
            for owner in cls['classes']:
                if owner.startswith('mezz/jei/'):
                    assert owner in api, f'Missing JEI class {owner}'
            for kind, owner, name, descriptor in cls['references']:
                if owner.startswith('mezz/jei/'):
                    assert resolves(kind, owner, name, descriptor), f'Missing JEI {kind} {owner}.{name}{descriptor}'
                    checked += 1
        for path in archive.namelist():
            if path.endswith('.class') and not path.startswith(PREFIX):
                assert b'mezz/jei/' not in archive.read(path), f'Optional JEI API leaked outside isolated plugin: {path}'
        assert not any(name.startswith('mezz/jei/') for name in archive.namelist()), 'JEI must remain external'
    print(f'PASS: {len(plugins)} isolated plugin classes; {checked} JEI field/method links resolved against '+', '.join(path.name for path in args.api))
    print('Static linkage only; does not prove client rendering, recipe lookup interaction or JER coexistence.')


if __name__ == '__main__':
    main()

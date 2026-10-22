#!/usr/bin/env python3
"""Check the review assets, editor round-trip coordinates and animation references."""
import base64
import json
import math
from pathlib import Path
import struct

HERE = Path(__file__).resolve().parent


def load(name):
    return json.loads((HERE/name).read_text())


def close(a, b):
    assert len(a) == len(b)
    assert all(math.isclose(x, y, abs_tol=1e-6) for x, y in zip(a, b)), (a, b)


geo = load('bingwa.geo.json')['minecraft:geometry'][0]
bb = load('bingwa.bbmodel')
animations = load('bingwa.animation.json')['animations']
review = load('review-data.json')
assert review['geometry'] == geo and review['animations'] == animations
names = [b['name'] for b in geo['bones']]
assert len(names) == len(set(names)) == 20
assert names[0] == 'root'
known = set()
for bone in geo['bones']:
    if 'parent' in bone:
        assert bone['parent'] in known, 'Parent must precede child'
    known.add(bone['name'])
    assert all(math.isfinite(x) for x in bone['pivot'])
    for cube in bone.get('cubes', []):
        assert len(cube['size']) == 3 and all(x > 0 for x in cube['size'])
        for face in cube['uv'].values():
            assert len(face['uv']) == len(face['uv_size']) == 2
            for i, bound in enumerate([geo['description']['texture_width'], geo['description']['texture_height']]):
                assert 0 <= face['uv'][i] < face['uv'][i]+face['uv_size'][i] <= bound

elements = {e['uuid']: e for e in bb['elements']}
assert len(elements) == len(bb['elements']) == 75
groups = {}


def walk(node):
    assert node['name'] not in groups
    groups[node['name']] = node
    for child in node['children']:
        if isinstance(child, dict):
            walk(child)
        else:
            assert child in elements


for node in bb['outliner']:
    walk(node)
for bone in geo['bones']:
    group = groups[bone['name']]
    pivot = group['origin']
    close([-pivot[0], pivot[1], pivot[2]], bone['pivot'])
    cubes = [elements[x] for x in group['children'] if isinstance(x, str)]
    assert len(cubes) == len(bone.get('cubes', []))
    for element, cube in zip(cubes, bone.get('cubes', [])):
        size = [b-a for a, b in zip(element['from'], element['to'])]
        close(size, cube['size'])
        close([-(element['from'][0]+size[0]), element['from'][1], element['from'][2]], cube['origin'])
        if 'rotation' in cube:
            r = element['rotation']
            close([-r[0], -r[1], r[2]], cube['rotation'])
        for key, face in element['faces'].items():
            u, v, U, V = face['uv']
            if key in ['up', 'down']:
                u, v, U, V = U, V, u, v
            close([u, v], cube['uv'][key]['uv'])
            close([U-u, V-v], cube['uv'][key]['uv_size'])

assert len(animations) == len(bb['animations']) == 7
for name, animation in animations.items():
    assert animation['animation_length'] > 0
    for bone_name, channels in animation['bones'].items():
        assert bone_name in known
        for kind, frames in channels.items():
            assert kind in ['position', 'rotation', 'scale']
            for t, vector in frames.items():
                assert 0 <= float(t) <= animation['animation_length']
                assert len(vector) == 3 and all(math.isfinite(x) for x in vector)
    editor = next(x for x in bb['animations'] if x['name'] == name)
    for animator in editor['animators'].values():
        for frame in animator['keyframes']:
            actual = [float(frame['data_points'][0][axis]) for axis in ['x', 'y', 'z']]
            # BB 4.10 values are converted on load by BB5, then inverted on Gecko export.
            frames = animation['bones'][animator['name']][frame['channel']]
            expected = next(v for t, v in frames.items() if float(t) == frame['time'])
            close(actual, expected)

png = (HERE/'textures/bingwa.png').read_bytes()
assert png.startswith(b'\x89PNG\r\n\x1a\n')
assert struct.unpack('>II', png[16:24]) == (256, 256)
assert base64.b64decode(bb['textures'][0]['source'].split(',', 1)[1]) == png
assert review['texture'] == bb['textures'][0]['source']
html = (HERE/'review.html').read_text()
assert '__BINGWA_DATA__' not in html and len(html.encode()) < 1024*1024
assert not any(x in html for x in ['fetch(', 'XMLHttpRequest', 'WebSocket', '<!doctype', '<html>'])
print('PASS: 75 cubes, 20 bones, 7 animations; UVs, texture, Blockbench round-trip and reviewer assets consistent')

#!/usr/bin/env python3
"""Validate the editable projects and the exact bundled and reviewed assets."""
import base64
import json
from math import isclose, isfinite
from pathlib import Path
import re

from PIL import Image

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parents[1] / 'versions/1.20.1/src/main/resources/assets/pinkkicore'
PAYLOAD = json.loads(re.search(r'id="galgame-payload" type="application/json">(.*?)</script>',
                               (HERE / 'review.html').read_text()).group(1))


def read(path):
    return json.loads(path.read_text())


def check_texture(name, path, project):
    with Image.open(path) as texture:
        assert texture.mode == 'RGBA' and texture.size == (128, 128)
        texture.verify()
    for uri in (project['textures'][0]['source'], PAYLOAD[name]['texture']):
        assert base64.b64decode(uri.split(',', 1)[1], validate=True) == path.read_bytes()


def vectors_equal(left, right):
    return len(left) == len(right) and all(isfinite(a) and isclose(a, b, abs_tol=1e-8) for a, b in zip(left, right))


def check_mob(name):
    geometry, = read(ASSETS / f'geo/{name}.geo.json')['minecraft:geometry']
    animations = read(ASSETS / f'animations/{name}.animation.json')['animations']
    project = read(HERE / f'{name}.bbmodel')
    assert geometry == PAYLOAD[name]['geometry']
    assert animations == PAYLOAD[name]['animations']
    check_texture(name, ASSETS / f'textures/entity/{name}.png', project)
    elements = {element['uuid']: element for element in project['elements']}
    groups = {}

    def collect(group, parent):
        assert group['name'] not in groups
        groups[group['name']] = (parent, group)
        for child in group['children']:
            if isinstance(child, dict):
                collect(child, group['name'])

    for group in project['outliner']:
        collect(group, None)
    assert len(groups) == len(geometry['bones'])
    known = set()
    for bone in geometry['bones']:
        name = bone['name']
        parent, group = groups[name]
        assert name not in known and parent == bone.get('parent') and (parent is None or parent in known)
        known.add(name)
        px, py, pz = bone['pivot']
        assert vectors_equal([-px, py, pz], group['origin'])
        cubes = [elements[child] for child in group['children'] if isinstance(child, str)]
        for cube, element in zip(bone['cubes'], cubes, strict=True):
            x, y, z = cube['origin']
            w, h, d = cube['size']
            assert min(w, h, d) > 0
            assert vectors_equal([-x-w, y, z], element['from'])
            assert vectors_equal([-x, y+h, z+d], element['to'])
            for face, uv in cube['uv'].items():
                u, v = uv['uv']
                w, h = uv['uv_size']
                expected = [u+w, v+h, u, v] if face in ('up', 'down') else [u, v, u+w, v+h]
                assert all(0 <= coordinate <= 128 for coordinate in expected)
                assert element['faces'][face]['uv'] == expected

    assert {animation['name'] for animation in project['animations']} == animations.keys()
    for animation in project['animations']:
        source = animations[animation['name']]
        assert animation['length'] == source['animation_length']
        assert animation['loop'] == ('loop' if source['loop'] else 'once')
        assert animation['animators'].keys() == {groups[name][1]['uuid'] for name in source['bones']}
        for name, channels in source['bones'].items():
            frames = animation['animators'][groups[name][1]['uuid']]['keyframes']
            actual = {(frame['channel'],frame['time']): [float(frame['data_points'][0][c]) for c in 'xyz']
                      for frame in frames}
            expected = {(channel,float(time)): vector for channel, times in channels.items()
                        for time,vector in times.items()}
            assert actual == expected
            assert all(0 <= time <= animation['length'] for _,time in actual)


def check_table():
    name = 'niumo_table'
    project = read(HERE / f'{name}.bbmodel')
    model = read(ASSETS / f'models/block/{name}.json')
    assert model == PAYLOAD[name]['model']
    check_texture(name, ASSETS / f'textures/block/{name}.png', project)
    for cube, element in zip(model['elements'], project['elements'], strict=True):
        assert cube['from'] == element['from'] and cube['to'] == element['to']
        assert all(0 <= value <= 16 for value in [*cube['from'], *cube['to']])
        for face, info in cube['faces'].items():
            uv = info['uv']
            assert all(0 <= value <= 16 for value in uv)
            assert element['faces'][face]['uv'] == [value * 8 for value in uv]


if __name__ == '__main__':
    for name in ('bullman', 'lao_cai'):
        check_mob(name)
    check_table()
    print('PASS 牛人、又菜又爱玩、牛魔台的工程、发行资源与预览一致')

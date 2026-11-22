#!/usr/bin/env python3
"""Validate that the editable projects, shipped resources and review data agree."""
import base64
import json
import math
from pathlib import Path
import re

from PIL import Image

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parents[1] / 'versions/1.20.1/src/main/resources/assets/pinkkicore'
WIKI = HERE.parents[1] / 'wiki/public/images'
REVIEW = json.loads(re.search(r'id="galgame-payload" type="application/json">(.*?)</script>',
                             (HERE / 'review.html').read_text()).group(1))


def read(path):
    return json.loads(path.read_text())


def validate_texture(name, category, project):
    path = ASSETS / f'textures/{category}/{name}.png'
    with Image.open(path) as texture:
        assert texture.size == (128, 128) and texture.mode == 'RGBA'
        texture.verify()
    expected = path.read_bytes()
    for uri in (REVIEW[name]['texture'], project['textures'][0]['source']):
        assert base64.b64decode(uri.split(',', 1)[1], validate=True) == expected


def validate_uv(coords):
    assert len(coords) == 4
    assert all(math.isfinite(v) and 0 <= v <= 128 for v in coords)


def validate_zhang():
    project = read(HERE / 'zhang.bbmodel')
    geo = read(ASSETS / 'geo/zhang.geo.json')['minecraft:geometry'][0]
    assert geo == REVIEW['zhang']['geometry']
    animations = read(ASSETS / 'animations/zhang.animation.json')['animations']
    assert animations == REVIEW['zhang']['animations']
    elements = {part['name']: part for part in project['elements']}
    assert len(elements) == len(geo['bones'])
    groups = {group['name']: group for group in [*project['outliner'], *project['outliner'][0]['children'][1:]]}
    known = set()
    for bone in geo['bones']:
        name = bone['name']
        assert name not in known and ('parent' not in bone or bone['parent'] in known)
        known.add(name)
        actual = elements[name]
        cube, = bone['cubes']
        x, y, z = cube['origin']
        w, h, d = cube['size']
        assert actual['from'] == [-x - w, y, z] and actual['to'] == [-x, y + h, z + d]
        assert actual['uuid'] in groups[name]['children']
        for face, data in cube['uv'].items():
            u, v = data['uv']
            du, dv = data['uv_size']
            expected = [u + du, v + dv, u, v] if face in ('up', 'down') else [u, v, u + du, v + dv]
            validate_uv(expected)
            assert actual['faces'][face]['uv'] == expected
    for animation in project['animations']:
        source = animations[animation['name']]
        assert animation['length'] == source['animation_length']
        assert source['loop'] and animation['loop'] == 'loop'
        for part, channels in source['bones'].items():
            frames = animation['animators'][groups[part]['uuid']]['keyframes']
            expected = {(channel, float(time)): vector for channel, values in channels.items()
                        for time, vector in values.items()}
            actual = {(frame['channel'], frame['time']): [float(frame['data_points'][0][c]) for c in 'xyz']
                      for frame in frames}
            assert actual == expected
            assert all(0 <= t <= animation['length'] for _, t in actual)
    validate_texture('zhang', 'entity', project)
    with Image.open(ASSETS / 'textures/entity/zhang.png') as face:
        assert sum(r < 30 and g < 30 and b < 30 for r, g, b, _ in face.crop((0, 0, 32, 32)).getdata()) > 80


def validate_block(name):
    project = read(HERE / f'{name}.bbmodel')
    model = read(ASSETS / f'models/block/{name}.json')
    assert model == REVIEW[name]['model']
    assert len(project['elements']) == len(model['elements'])
    for editor, runtime in zip(project['elements'], model['elements']):
        assert editor['from'] == runtime['from'] and editor['to'] == runtime['to']
        assert all(0 <= a < b <= 16 for a, b in zip(editor['from'], editor['to']))
        for face, data in runtime['faces'].items():
            expected = [coord * 8 for coord in data['uv']]
            validate_uv(expected)
            assert editor['faces'][face]['uv'] == expected
    validate_texture(name, 'block', project)
    with Image.open(WIKI / f'{name}.png') as icon:
        assert icon.mode == 'RGBA' and icon.getbbox() is not None


def main():
    validate_zhang()
    validate_block('sword_in_stone')
    validate_block('galgame_arcade')
    for name in ('zhang_fav', 'galgame_soul', 'favorite_person', 'galgame_heart'):
        expected = ASSETS / f'textures/item/{name}.png'
        with Image.open(expected) as icon:
            assert icon.size == (16, 16) and icon.getbbox() is not None
        assert expected.read_bytes() == (WIKI / f'{name}.png').read_bytes()
    print('PASS Zhang and Galgame geometry, animations, texture atlas, review data and item icons')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Check the bed's Blockbench, preview and Minecraft asset round trip."""

import base64
import json
from io import BytesIO

from PIL import Image

from generate import ASSETS, FACES, HERE, WIKI, create_atlas, joined_elements


def read_json(path):
    return json.loads(path.read_text())


models = [read_json(ASSETS / f'models/block/guaba_bed_{part}.json') for part in ('head', 'foot')]
state = read_json(ASSETS / 'blockstates/guaba_bed.json')['variants']
for face, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
    for part in ('head', 'foot'):
        assert state[f'facing={face},part={part}'] == {'model': f'pinkkicore:block/guaba_bed_{part}', 'y': rotation}
assert len(state) == 8

bb = read_json(HERE / 'guaba_bed.bbmodel')
preview = (HERE / 'review.html').read_text().split('const data=', 1)[1]
payload, _ = json.JSONDecoder().raw_decode(preview)
atlas = Image.open(ASSETS / 'textures/block/guaba_bed.png').convert('RGBA')
assert atlas.size == (64, 64) and atlas.tobytes() == create_atlas().tobytes()
assert bb['textures'][0]['source'] == payload['texture']
embedded = Image.open(BytesIO(base64.b64decode(payload['texture'].partition(',')[2]))).convert('RGBA')
assert atlas.tobytes() == embedded.tobytes()
assert bb['meta']['model_format'] == 'java_block'
parts = list(joined_elements(models))
assert parts == payload['elements']
assert len(parts) == len(bb['outliner']) == len(bb['elements'])
assert len(set(bb['outliner'])) == len(parts)
for element, cube in zip(parts, bb['elements'], strict=True):
    assert cube['uuid'] in bb['outliner']
    assert (cube['from'], cube['to']) == (element['from'], element['to'])
    assert all(0 <= a < b <= limit for a, b, limit in zip(cube['from'], cube['to'], (16, 9, 32), strict=True))
    assert set(cube['faces']) == set(FACES)
    assert {face for face, details in cube['faces'].items() if details['texture'] is not None} == set(element['faces'])
    for face, details in element['faces'].items():
        bounds = [round(coordinate * 4) for coordinate in details['uv']]
        assert bounds == cube['faces'][face]['uv']
        assert cube['faces'][face]['texture'] == 0 and details['texture'] == '#bed'
        assert 0 <= bounds[0] < bounds[2] <= 64 and 0 <= bounds[1] < bounds[3] <= 64
        assert atlas.crop(bounds).getchannel('A').getextrema() == (255, 255)

for name in ('guaba_bed', 'cloud_dream', 'guaba_cloud_dream', 'nether_dream', 'guaba_nether_dream'):
    item_texture = ASSETS / f'textures/item/{name}.png'
    assert item_texture.read_bytes() == (WIKI / f'{name}.png').read_bytes()
    sprite = Image.open(item_texture)
    assert sprite.size == (16, 16) and sprite.getchannel('A').getextrema() == (0, 255)
    model = read_json(ASSETS / f'models/item/{name}.json')
    assert model == {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'pinkkicore:item/{name}'}}
print('Bed model, UVs, embedded texture, all orientations and five item icons match.')

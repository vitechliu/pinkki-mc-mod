#!/usr/bin/env python3
"""Generate the original striped bed and dream sprites. Requires Pillow."""

import base64
import io
import json
import random
import uuid
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
ASSETS = ROOT / 'versions/1.20.1/src/main/resources/assets/pinkkicore'
WIKI = ROOT / 'wiki/public/images'
TEXTURE = 'pinkkicore:block/guaba_bed'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')

CLOUD = (
    '................', '................', '................', '......231.......',
    '.....34431......', '....344442......', '..2334444321....', '.344444433321...',
    '.3444433334441..', '..2333323344431.', '...12223333331..', '..1...1222221...',
    '.........11...1.', '................', '................', '................',
)
MIST = (
    '................', '................', '.......12.......', '......232.1.....',
    '....123421......', '...234233211....', '...3431234321...', '..243211234321..',
    '..241112344321..', '..13223344321...', '...123334321....', '....1122321.1...',
    '.......11.......', '.....1..........', '................', '................',
)
BED = (
    '................', '................', '......22........', '.....24422......',
    '...224444422....', '..24444223322...', '..244223323332..', '..1233233232332.',
    '..1123232332332.', '..1.11233233321.', '.......11233211.', '.........1121.1.',
    '..........1.1...', '................', '................', '................',
)


def write_json(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, ensure_ascii=False, indent=2) + '\n')


def sprite(rows, palette):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows)
    result = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        for x, pixel in enumerate(row):
            if pixel != '.':
                result.putpixel((x, y), (*bytes.fromhex(palette[int(pixel) - 1]), 255))
    return result


def create_atlas():
    atlas = Image.new('RGBA', (64, 64))
    randomizer = random.Random(192)

    def fill(x, y, width, height, swatch):
        for row in range(height):
            for column in range(width):
                base = bytes.fromhex(swatch(column, row))
                variation = randomizer.choice((-5, -2, 0, 0, 2, 5))
                atlas.putpixel((x + column, y + row), tuple(channel + variation for channel in base) + (255,))

    def blanket(x, y):
        if x in (0, 15) or y == 31:
            return '7D542E'
        if x in (3, 4, 11, 12):
            return 'D9A963'
        if x in (2, 5, 10, 13):
            return 'A5723C'
        return 'BE8A47'

    def pillow(x, y):
        return 'B8A88B' if x in (0, 11) or y in (0, 5) else 'F0E2BE'

    fill(0, 0, 16, 32, blanket)
    fill(16, 0, 16, 16, lambda x, y: blanket(x, y + 16) if y <= 4 else 'AF793E')
    fill(16, 16, 12, 6, pillow)
    fill(32, 0, 16, 16, lambda x, y: '896448' if y in (1, 7, 14) else '64442F')
    fill(32, 16, 16, 16, lambda x, y: 'A4754B' if (x * 3 + y) % 7 == 0 else '795234')
    fill(48, 0, 16, 16, lambda x, y: 'CCB087' if y in (0, 6) else 'E4CF9F')
    return atlas


def cuboid(name, start, end, uv, faces=None):
    faces = faces or FACES
    return {
        'name': name,
        'from': start,
        'to': end,
        'faces': {
            face: {'uv': [value / 4 for value in uv[face]], 'texture': '#bed'}
            for face in faces
        },
    }


def half(head):
    seam = 'south' if head else 'north'
    outer_faces = [face for face in FACES if face != seam]
    fabric = (16, 0, 32, 4)
    wood = (32, 0, 48, 8)
    cover_top = (0, 0, 16, 16) if head else (0, 16, 16, 32)
    elements = [
        cuboid('深棕床架', [0, 3, 0], [16, 5, 16], dict.fromkeys(FACES, wood), outer_faces),
        cuboid('瓜巴条纹被面', [0, 5, 0], [16, 8, 16],
               dict.fromkeys(FACES, fabric) | {'up': cover_top}, outer_faces),
    ]
    leg_z = 0 if head else 13
    for label, leg_x in [('左', 0), ('右', 13)]:
        elements.append(cuboid(label + '床脚', [leg_x, 0, leg_z], [leg_x + 3, 3, leg_z + 3],
                               dict.fromkeys(FACES, (32, 16, 38, 22))))
    if head:
        elements.append(cuboid('米白枕头', [2, 8, 1], [14, 9, 7],
                                dict.fromkeys(FACES, (48, 0, 64, 3)) | {'up': (16, 16, 28, 22)}))
    return {'textures': {'bed': TEXTURE, 'particle': TEXTURE}, 'elements': elements}


def joined_elements(models):
    for offset, model in zip((0, 16), models):
        for element in model['elements']:
            yield {
                **element,
                'name': ('床头 · ' if not offset else '床尾 · ') + element['name'],
                'from': [element['from'][0], element['from'][1], element['from'][2] + offset],
                'to': [element['to'][0], element['to'][1], element['to'][2] + offset],
            }


def write_project(elements, texture_uri):
    def uid(name):
        return str(uuid.uuid5(uuid.NAMESPACE_URL, 'pinkkicore:guaba_bed/' + name))

    cubes = [
        {
            **part, 'type': 'cube', 'uuid': uid(part['name']), 'origin': [8, 0, 16],
            'autouv': 0, 'box_uv': False,
            'faces': {
                key: {'uv': [n * 4 for n in part['faces'][key]['uv']], 'texture': 0}
                if key in part['faces'] else {'uv': [0, 0, 0, 0], 'texture': None}
                for key in FACES
            },
        }
        for part in elements
    ]
    write_json(HERE / 'guaba_bed.bbmodel', {
        'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
        'name': '瓜巴床', 'model_identifier': 'guaba_bed', 'resolution': {'width': 64, 'height': 64},
        'elements': cubes, 'outliner': [cube['uuid'] for cube in cubes],
        'textures': [{'name': 'guaba_bed.png', 'id': '0', 'uuid': uid('texture'), 'source': texture_uri,
                      'mode': 'bitmap', 'saved': False, 'width': 64, 'height': 64, 'uv_width': 64, 'uv_height': 64}],
    })


def main():
    atlas = create_atlas()
    (ASSETS / 'textures/block').mkdir(parents=True, exist_ok=True)
    atlas.save(ASSETS / 'textures/block/guaba_bed.png')
    png = io.BytesIO()
    atlas.save(png, format='PNG')
    uri = 'data:image/png;base64,' + base64.b64encode(png.getvalue()).decode()

    models = [half(True), half(False)]
    for part, model in zip(('head', 'foot'), models):
        write_json(ASSETS / f'models/block/guaba_bed_{part}.json', model)
    write_json(ASSETS / 'blockstates/guaba_bed.json', {
        'variants': {
            f'facing={direction},part={part}': {'model': f'pinkkicore:block/guaba_bed_{part}', 'y': rotation}
            for direction, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]
            for part in ('head', 'foot')
        },
    })

    icons = {
        'cloud_dream': (CLOUD, ('6989A3', '9DB8CE', 'C5DFE9', 'F2F7EF')),
        'guaba_cloud_dream': (CLOUD, ('876039', 'B7854D', 'D9B677', 'F7E5B5')),
        'nether_dream': (MIST, ('372B42', '4E3C5D', '635075', '8A7599')),
        'guaba_nether_dream': (MIST, ('50352E', '76503C', 'A5794D', 'CEA56A')),
        'guaba_bed': (BED, ('603C2C', '946337', 'D8A759', 'F5E6C2')),
    }
    for name, (rows, palette) in icons.items():
        image = sprite(rows, palette)
        image.save(ASSETS / f'textures/item/{name}.png')
        image.save(WIKI / f'{name}.png')
        write_json(ASSETS / f'models/item/{name}.json',
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'pinkkicore:item/{name}'}})

    elements = list(joined_elements(models))
    write_project(elements, uri)
    payload = json.dumps({'elements': elements, 'texture': uri}, ensure_ascii=False)
    template = (HERE / 'review-template.html').read_text()
    (HERE / 'review.html').write_text(template.replace('/*ASSET*/null', payload))
    print('Generated five 16×16 sprites, a two-part bed model and its editable and review assets.')


if __name__ == '__main__':
    main()

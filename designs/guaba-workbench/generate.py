#!/usr/bin/env python3
"""Deterministic static Java Block/Item design; no external dependencies."""
import base64
import json
from pathlib import Path
import random
import struct
import uuid
import zlib

HERE = Path(__file__).resolve().parent


def generate():
    rng = random.Random(24)
    pixels = bytearray(64 * 64 * 4)
    # Ochre/brown swatches sampled from the existing guaba item rind.
    ochre, shadow, light = (178, 121, 76), (146, 94, 54), (217, 152, 102)

    def pixel(x, y, color):
        i = (y * 64 + x) * 4
        pixels[i:i + 4] = bytes((*color, 255))

    def rect(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                pixel(x, y, color)

    for y in range(32):
        for x in range(32):
            edge = min(x, y, 31-x, 31-y)
            c = ochre if edge < 2 else (214, 182, 155) if edge < 4 else (191, 49, 35)
            jitter = rng.choice([-5, -2, 0, 0, 3, 5])
            pixel(x, y, tuple(v+jitter for v in c))
            stripe = (x + (y // 6) % 2) % 10 < 3
            c = shadow if stripe else ochre
            j = rng.choice([-5, 0, 0, 5])
            pixel(x+32, y, tuple(v+j for v in c))
    # Uneven, broken crosshatching: no rectangular frame or crafting slots.
    def score(points, gaps=()):
        index = 0
        for (x0, y0), (x1, y1) in zip(points, points[1:]):
            steps = max(abs(x1-x0), abs(y1-y0))
            for step in range(steps):
                x = round(x0+(x1-x0)*step/steps)
                y = round(y0+(y1-y0)*step/steps)
                if index not in gaps:
                    pixel(x, y, (130, 29, 19))
                    if index % 3 != 0:
                        pixel(x, y+1, (209, 65, 45))
                index += 1

    score([(10, 9), (14, 12), (18, 13), (23, 17)], gaps=(5,))
    score([(10, 15), (13, 16), (17, 19), (22, 21)], gaps=(2, 9))
    score([(11, 21), (15, 23), (19, 24)])
    score([(10, 24), (13, 20), (15, 16), (19, 10)], gaps=(6, 7))
    score([(16, 25), (18, 21), (20, 16), (23, 12)], gaps=(3,))
    score([(10, 13), (12, 10), (14, 8)])
    # Flat pixel tools, matching the vanilla workbench's tool motif.
    # Hammer on the left: brown shaft, dark outline, steel head.
    rect(6, 11, 8, 21, (80, 50, 27))
    rect(6, 12, 7, 20, light)
    rect(4, 8, 9, 12, (58, 51, 42))
    rect(4, 8, 8, 10, (194, 196, 181))
    rect(4, 10, 8, 11, (137, 145, 131))
    pixel(8, 9, (194, 196, 181))
    # Knife on the right, blade above the wrapped handle.
    rect(25, 9, 28, 18, (80, 50, 27))
    rect(25, 9, 27, 17, (194, 196, 181))
    rect(25, 9, 26, 16, (229, 227, 205))
    rect(25, 18, 27, 23, (80, 50, 27))
    rect(25, 19, 26, 22, light)
    rect(24, 17, 28, 18, shadow)
    # Seeds and a few short score marks leave the melon recognisable.
    for x, y in [(7, 5), (19, 5), (6, 24), (25, 25)]:
        pixel(x, y, (80, 50, 27))
        pixel(x, y+1, (48, 31, 24))
    rect(12, 6, 16, 7, (214, 72, 52))
    rect(14, 7, 18, 8, (122, 24, 14))
    rect(12, 25, 19, 26, (122, 24, 14))
    for y in range(26):
        for x in range(32):
            stripe = (x + [0, 0, 1, 1, 0, -1][(y//2) % 6]) % 10 < 3
            c = shadow if stripe else ochre
            if y < 2:
                c = light if not stripe else ochre
            elif y >= 24:
                c = (80, 50, 27) if stripe else shadow
            j = rng.choice([-6, -3, 0, 0, 4, 6])
            pixel(x, y+32, tuple(v+j for v in c))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind+data) & 0xffffffff)

    raw = b''.join(b'\0'+pixels[y*256:(y+1)*256] for y in range(64))
    png = b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR', struct.pack('>IIBBBBB', 64, 64, 8, 6, 0, 0, 0))+chunk(b'IDAT', zlib.compress(raw))+chunk(b'IEND', b'')
    (HERE/'textures').mkdir(exist_ok=True)
    (HERE/'textures/guaba_workbench.png').write_bytes(png)
    source = 'data:image/png;base64,'+base64.b64encode(png).decode()
    uvs = {f: [0, 8, 8, 14.5] for f in ['north', 'south', 'east', 'west']}
    uvs.update(up=[0, 0, 8, 8], down=[8, 0, 16, 8])
    model = dict(credit='PinkkiCore — design draft', textures={'particle': 'pinkkicore:block/guaba_workbench', 'all': 'pinkkicore:block/guaba_workbench'},
                 elements=[{'from': [0, 0, 0], 'to': [16, 12.8, 16], 'faces': {f: dict(uv=uv, texture='#all') for f, uv in uvs.items()}}],
                 display={'gui': {'rotation': [30, 225, 0], 'translation': [0, 1, 0], 'scale': [.625, .625, .625]},
                          'ground': {'translation': [0, 1, 0], 'scale': [.25, .25, .25]},
                          'fixed': {'rotation': [0, 180, 0], 'translation': [0, 1, 0], 'scale': [.5, .5, .5]},
                          'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.375, .375, .375]},
                          'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 1, 0], 'scale': [.4, .4, .4]}})
    uid = str(uuid.uuid5(uuid.NAMESPACE_DNS, 'pinkkicore.guaba_workbench'))
    bb = dict(meta={'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False}, name='瓜巴工作台 · 第三稿',
              model_identifier='guaba_workbench', resolution={'width': 64, 'height': 64},
              elements=[dict(name='瓜巴工作台主体', uuid=uid, type='cube', **{'from': [0, 0, 0], 'to': [16, 12.8, 16]},
                             origin=[8, 0, 8], rotation=[0, 0, 0], autouv=0, box_uv=False,
                             faces={f: dict(uv=[v*4 for v in uv], texture=0) for f, uv in uvs.items()})],
              outliner=[uid], textures=[dict(name='guaba_workbench.png', id='0', uuid=str(uuid.uuid5(uuid.NAMESPACE_DNS, 'pinkkicore.guaba_texture')),
                                             source=source, mode='bitmap', saved=False, width=64, height=64, uv_width=64, uv_height=64)],
              display=model['display'])
    for name, data in [('guaba_workbench.json', model), ('guaba_workbench.bbmodel', bb)]:
        (HERE/name).write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    payload = json.dumps(dict(model=model, texture=source), ensure_ascii=False, separators=(',', ':'))
    fragment = (HERE/'review-template.html').read_text().replace('/*ASSET*/null', payload)
    (HERE/'review.html').write_text(fragment)
    (HERE/'review-standalone.html').write_text('<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>瓜巴工作台 · 模型审阅</title><style>body{font-family:system-ui;max-width:900px;margin:24px auto;padding:16px;color:#263425;background:#f5f5ee}button{padding:8px 16px;margin:4px}strong{margin-right:16px}</style>'+fragment+'</html>')
    print('Generated 1 cube, 6 textured faces; bounds 16 × 12.8 × 16; embedded 64 × 64 texture.')


if __name__ == '__main__':
    generate()

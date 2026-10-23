#!/usr/bin/env python3
"""Validate source, Java JSON, texture and preview against one another."""
import base64
import json
import math
from pathlib import Path
import re
import struct
import zlib

HERE = Path(__file__).resolve().parent
model = json.loads((HERE/'guaba_workbench.json').read_text())
bb = json.loads((HERE/'guaba_workbench.bbmodel').read_text())
assert bb['meta']['model_format'] == 'java_block'
assert len(bb['elements']) == len(model['elements']) == 1
cube, source = model['elements'][0], bb['elements'][0]
for key in ['from', 'to']:
    assert source[key] == cube[key]
    assert all(math.isfinite(n) and 0 <= n <= 16 for n in cube[key])
assert cube['to'] == [16, 12.8, 16]
assert set(cube['faces']) == {'north', 'south', 'east', 'west', 'up', 'down'}
for face, data in cube['faces'].items():
    assert source['faces'][face]['uv'] == [n*4 for n in data['uv']]
    assert all(math.isfinite(n) and 0 <= n <= 16 for n in data['uv'])
    assert data['uv'][0] < data['uv'][2] and data['uv'][1] < data['uv'][3]
    assert data['texture'] == '#all' and source['faces'][face]['texture'] == 0
png = (HERE/'textures/guaba_workbench.png').read_bytes()
assert base64.b64decode(bb['textures'][0]['source'].split(',')[1]) == png
assert png[:8] == b'\x89PNG\r\n\x1a\n'
assert struct.unpack('>II', png[16:24]) == (64, 64)
pos, compressed = 8, b''
while pos < len(png):
    size = struct.unpack('>I', png[pos:pos+4])[0]
    kind, payload = png[pos+4:pos+8], png[pos+8:pos+8+size]
    assert zlib.crc32(kind+payload) & 0xffffffff == struct.unpack('>I', png[pos+8+size:pos+12+size])[0]
    if kind == b'IDAT':
        compressed += payload
    pos += size+12
raw = zlib.decompress(compressed)
assert len(raw) == 64*(1+64*4)
for face in source['faces'].values():
    x0, y0, x1, y1 = face['uv']
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            assert raw[y*257+1+x*4+3] == 255
# All side/bottom pixels must use warm ochre/brown, without green rind.
for name in ['north', 'south', 'east', 'west', 'down']:
    x0, y0, x1, y1 = source['faces'][name]['uv']
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            i = y*257+1+x*4
            r, g, b = raw[i:i+3]
            assert r > g > b, (name, x, y)
fragment = (HERE/'review.html').read_text()
asset = json.loads(re.search(r'const data=(.*);\nconst canvas=', fragment).group(1))
assert asset == dict(model=model, texture=bb['textures'][0]['source'])
assert fragment == (HERE/'review-template.html').read_text().replace('/*ASSET*/null', json.dumps(asset, ensure_ascii=False, separators=(',', ':')))
assert fragment in (HERE/'review-standalone.html').read_text()
assert source['uuid'] in bb['outliner']
assert model['display'] == bb['display']
print('PASS: bounds, 6 faces, UV mapping, PNG CRC/decode/opacity, embedded texture, source/export/review equality, item display transforms.')

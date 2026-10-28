"""Verify the PNG, standard Alex body, UV bounds, and both embedded textures."""
import base64
import hashlib
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).parent
png = (ROOT / 'guaba_yingrong.png').read_bytes()
assert png[:8] == b'\x89PNG\r\n\x1a\n'
pos, compressed = 8, b''
while pos < len(png):
    size = struct.unpack('!I', png[pos:pos + 4])[0]
    kind = png[pos + 4:pos + 8]
    data = png[pos + 8:pos + 8 + size]
    crc = struct.unpack('!I', png[pos + 8 + size:pos + 12 + size])[0]
    assert zlib.crc32(kind + data) == crc
    if kind == b'IHDR':
        assert struct.unpack('!2I5B', data) == (64, 64, 8, 6, 0, 0, 0)
    if kind == b'IDAT':
        compressed += data
    pos += size + 12
assert pos == len(png)
raw = zlib.decompress(compressed)
assert len(raw) == 64 * 257
assert all(raw[y * 257] == 0 for y in range(64))

# All six inner-body cuboids must have opaque pixels on every used UV face.
def opaque(x, y, width, height):
    assert all(raw[dy * 257 + 1 + dx * 4 + 3] == 255
               for dy in range(y, y + height) for dx in range(x, x + width))

for u, v, w, h, d in [(0, 0, 8, 8, 8), (16, 16, 8, 12, 4),
                       (40, 16, 3, 12, 4), (32, 48, 3, 12, 4),
                       (0, 16, 4, 12, 4), (16, 48, 4, 12, 4)]:
    opaque(u + d, v, w * 2, d)
    opaque(u, v + d, (w + d) * 2, h)

for filename in ('guaba_yingrong.bbmodel', 'guaba_yingrong-review.bbmodel'):
    model = json.loads((ROOT / filename).read_text())
    assert model['name'] == 'guaba_yingrong'
    assert model['resolution'] == {'width': 64, 'height': 64}
    assert base64.b64decode(model['textures'][0]['source'].split(',')[1]) == png
    assert model['textures'][0]['name'] == 'guaba_yingrong.png'
    assert not model['textures'][0].get('path')
    if model['meta']['model_format'] == 'skin':
        assert model['skin_model'] == 'alex.java'
    else:
        assert len(model['elements']) == 12
        for element in model['elements']:
            assert element.get('visibility', True)
            assert all(math.isfinite(n) for n in element['from'] + element['to'])
            for face in element['faces'].values():
                assert all(math.isfinite(n) and 0 <= n <= 64 for n in face['uv'])
    print(filename + ': PASS')
print('PNG: PASS (64x64 RGBA, CRC, opaque inner faces)')
print('SHA256: ' + hashlib.sha256(png).hexdigest())

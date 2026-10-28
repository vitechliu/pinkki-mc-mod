"""guaba_yingrong: deterministic, hand-authored 64x64 Slim Minecraft UV art."""
import base64
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).parent
PALETTE = {
    '.': None,
    'k': '201f26', 'K': '302c32', 'd': '454039', 'D': '615849',
    'o': '77725b', 'O': '999178',
    'g': 'ae894c', 'G': 'd7b56b', 'y': 'f3da95',
    'r': '7b2935', 'R': 'b83b45', 'f': 'de605c', 'F': 'f9937b',
    'n': '23383f', 'N': '36525b', 'c': '52838b', 'C': '83b4b5',
    'w': 'f3eee3', 'W': 'cfceca', 'v': 'a1a8aa',
    's': 'f1d6c4', 'S': 'd4afa5', 't': 'ffe9d2', 'b': 'd3a3a0',
    'e': 'c59a48', 'E': 'f5ce67',
}
P = {k: (tuple(bytes.fromhex(v)) + (255,) if v else (0, 0, 0, 0))
     for k, v in PALETTE.items()}
px = [[P['.'] for _ in range(64)] for _ in range(64)]


def art(x, y, rows):
    assert rows and len({len(r) for r in rows}) == 1, (x, y, rows)
    assert 0 <= x and x + len(rows[0]) <= 64 and 0 <= y and y + len(rows) <= 64
    for dy, row in enumerate(rows):
        for dx, c in enumerate(row):
            px[y + dy][x + dx] = P[c]


def fill(x, y, width, height, color):
    art(x, y, [color * width] * height)


def box(u, v, width, height, depth, color):
    fill(u + depth, v, width * 2, depth, color)
    fill(u, v + depth, (width + depth) * 2, height, color)


# Head base: white forelock over dark teal hair. Two 2px eyes separated by 2px.
# Low eye line and no mouth follow the user's previous character-review feedback.
# Each 2x2 eye is rotated 180 degrees: black pupils are on the lower inner corners.
box(0, 0, 8, 8, 8, 'n')
art(8, 8, ['nnwwWnnn', 'nWwwwNnn', 'nWwwWNnn', 'cWwWssNn',
           'cwsstssn', 'sEwsswes', 'sGksskGs', 'SssssssS'])
art(0, 8, ['nNNcCNnn','nNcCCNnn','nNcCWNnn','nNccWNnn',
           'nNcCsSnn','nNcCSSnn','nNcCNnnn','nncCNnnn'])
art(16, 8, ['nnNCcNnn','nnNCccNn','nnNCccNn','nnNCccNn',
            'nnSsCcNn','nnSsCcNn','nnnNCcNn','nnnnNCnn'])
art(24, 8, ['nnNNNNnn','nNNcCNnn','nNcCCNnn','nNcCCNnn',
            'nNcCcNnn','nNccNNnn','nncNNNnn','nnNNnnnn'])
art(8, 0, ['nnNNNnnn','nNNcNNnn','nNwwwNnn','nWwwwNnn',
           'nWwwWNnn','nWwwNNnn','nnWWNnnn','nnNNnnnn'])

# Hood overlay. Olive edging is a raised rim; the face opening stays transparent.
art(40, 0, ['kkddddkk','kddDDddk','ddDOODdd','dDOooODd',
            'dDOooODd','ddDOODdd','kddDDddk','kkddddkk'])
art(40, 8, ['dDOOODdd','DgGyyGgD','o.nwW..d','o.wwW..d',
            'd......d','........','........','........'])
art(32, 8, ['kkdDOOod','kdDOOood','kdDOoodd','kdDooddk',
            'kdDood.c','kdDod.Cc','kdDd.Ccn','kdd.Ccnn'])
art(48, 8, ['doOODdkk','dooOODdk','ddooODdk','kddooDdk',
            'c.dooDdk','cC.doDdk','ncC.dDdk','nncC.ddk'])
art(56, 8, ['kddDOddk','ddDOODdd','dDOooODd','dDOooODd',
            'dDOooODd','ddDOODdd','kddDDddk','kkddddkk'])

# Jacket: ivory panels, yellow waistcoat, charcoal harness, ruby neck clasp.
box(16, 16, 8, 12, 4, 'd')
art(20, 20, ['GwSSsswG','wGyrryGw','wwRffRww','GwrRRrwG',
            'GywRRwyG','dyGrrGyd','kGyrryGk','kGyyGyGk',
            'kkGGGGkk','kGGggGGk','kkgKKgkk','kKdKKdKk'])
art(32, 20, ['ddDOODdd','dDOooODd','ddDOODdd','kddDDddk',
            'wGddddGw','wwGddGww','KwGGGGwK','kkGwwGkk',
            'kkGGGGkk','kGGggGGk','kkgKKgkk','kKdKKdKk'])
art(16, 20, ['GddG','wGdG','wwGG','GwGG','GyGd','dyGk',
            'kyGk','kGGk','kkGk','kGGk','kkgk','kkKk'])
art(28, 20, ['GddG','GdGw','GGww','GGwG','dGyG','kGyd',
            'kGyk','kGGk','kGkk','kGGk','kgkk','kKkk'])
art(20, 16, ['dGwwwwGd','GwwSSwwG','wGssssGw','wwGssGww'])

# Raised collar and red cravat; sparse overlay preserves the coat's readable shapes.
art(20, 36, ['G......G','wGyrryGw','.wRffRw.','..rRRr..',
            '.G.RR.G.','..GrrG..','...rr...','........',
            '.G....G.','..GGGG..','........','........'])
art(32, 36, ['ddDOODdd','dDOooODd','ddDOODdd','kddDDddk',
            '.G....G.','..G..G..','...GG...','........',
            '.G....G.','..GGGG..','........','........'])
art(16, 36, ['G..G','wG.G','.wGG','..GG','.yG.','..G.',
            '....','....','..G.','.GG.','....','....'])
art(28, 36, ['G..G','G.Gw','GGw.','GG..','.Gy.','.G..',
            '....','....','.G..','.GG.','....','....'])
fill(20, 32, 8, 4, 'G')


def arm(u, v, mirror=False, outer=False):
    if not outer:
        box(u, v, 3, 12, 4, 'w')
    front = ['Gww','wwW','wWW','Gww','dGw','KdG',
             'KdD','Kdd','GgG','kKK','sSs','stS']
    back = ['Gww','wwW','WWW','wWG','wGd','GdK',
            'DdK','ddK','GgG','KKk','sSs','stS']
    side = ['GwwG','wwWW','wWWW','GwWW','dGww','KdGG',
            'KddD','Kddd','GggG','kKKk','sSSs','stts']
    if outer:
        front = ['Gww','...','...','G..','.G.','..G',
                 '...','...','GgG','kKK','...','...']
        back = ['Gww','...','...','..G','.G.','G..',
                '...','...','GgG','KKk','...','...']
        side = ['GwwG','....','....','G...','.G..','..GG',
                '....','....','GggG','kKKk','....','....']
    for du, rows in [(0, side), (4, front), (7, side), (11, back)]:
        art(u + du, v + 4, [r[::-1] if mirror else r for r in rows])
    fill(u + 4, v, 3, 4, 'w' if not outer else 'G')


arm(40, 16)
arm(32, 48, True)
arm(40, 32, outer=True)
arm(48, 48, True, True)


def leg(u, v, mirror=False, outer=False):
    if not outer:
        box(u, v, 4, 12, 4, 'K')
    front = ['kdDK','kdDK','kdDK','kgGK','kKdK','kKdK',
             'dGGd','kDDk','kDDk','kDDk','dDDd','kkkk']
    side = ['KKdk','KKdk','KKdk','KGgk','KKdk','KKdk',
            'dGGd','kDDk','kDDk','kDDk','dDDd','kkkk']
    back = ['Kddk','Kddk','Kddk','Kggk','KKdk','KKdk',
            'dGGd','kDDk','kDDk','kDDk','dDDd','kkkk']
    if outer:
        front = ['Gwwg','Gwwg','GwWg','GWWg','GWvg','Gggg',
                 '....','....','....','....','....','....']
        side = ['dDdk','dDdk','dDdk','dDdk','dDdk','Gggk'] + ['....'] * 6
        back = ['krrG','krRG','krRG','krRG','krRG','kggG'] + ['....'] * 6
    for du, rows in [(0, side), (4, front), (8, side), (12, back)]:
        art(u + du, v + 4, [r[::-1] if mirror else r for r in rows])
    fill(u + 4, v, 4, 4, 'd')
    if not outer:
        fill(u + 8, v, 4, 4, 'k')


leg(0, 16)
leg(16, 48, True)
leg(0, 32, outer=True)
leg(0, 48, True, True)


def png_bytes():
    def chunk(kind, payload):
        return struct.pack('!I', len(payload)) + kind + payload + struct.pack('!I', zlib.crc32(kind + payload))
    raw = b''.join(b'\0' + bytes(c for p in row for c in p) for row in px)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 64, 64, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


data = png_bytes()
(ROOT / 'guaba_yingrong.png').write_bytes(data)
for name in ('guaba_yingrong.bbmodel', 'guaba_yingrong-review.bbmodel'):
    path = ROOT / name
    model = json.loads(path.read_text())
    model['name'] = 'guaba_yingrong'
    model['textures'][0]['source'] = 'data:image/png;base64,' + base64.b64encode(data).decode()
    model['textures'][0]['name'] = 'guaba_yingrong.png'
    for key in ('path', 'relative_path'):
        model['textures'][0].pop(key, None)
    path.write_text(json.dumps(model, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'asset': 'guaba_yingrong', 'size': [64, 64], 'model': 'slim', 'bytes': len(data)}))

"""Deterministic 64x64 Alex skin. All coordinates are Minecraft UV pixels."""
import base64
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).parent
P = {
    '.': (0, 0, 0, 0),
    'd': '#583b59', 'D': '#74506c',
    'h': '#cf789e', 'H': '#e59ab8', 'i': '#f4bfd0', 'j': '#ae608c',
    's': '#f8dccb', 'S': '#e8b7b1', 't': '#fff0db', 'b': '#d78b9e',
    'w': '#fff8f1', 'W': '#e8e3f1', 'v': '#c5b2dc', 'V': '#a38abd',
    'c': '#cff4eb', 'C': '#a8dcd9', 'y': '#e8efaa', 'Y': '#c3cc7e',
    'p': '#d87fa7', 'P': '#b86191', 'q': '#efacc8',
    'e': '#b05280', 'k': '#443646',
}
P = {k: ((tuple(bytes.fromhex(v[1:])) + (255,)) if isinstance(v, str) else v)
     for k, v in P.items()}
px = [[P['.'] for _ in range(64)] for _ in range(64)]


def art(x, y, rows):
    assert rows and len({len(row) for row in rows}) == 1, (x, y, rows)
    assert 0 <= x and x + len(rows[0]) <= 64 and 0 <= y and y + len(rows) <= 64
    for dy, row in enumerate(rows):
        for dx, color in enumerate(row):
            px[y + dy][x + dx] = P[color]


def fill(x, y, w, h, color):
    art(x, y, [color * w] * h)


def box(u, v, width, height, depth, color):
    # Vanilla unfolded cuboid: top/bottom above west/front/east/back.
    fill(u + depth, v, width * 2, depth, color)
    fill(u, v + depth, (width + depth) * 2, height, color)


# HEAD: widely spaced anime eyes; four skin pixels separate the 2px eye bands.
# Pupils at face columns 1 and 6 (was 2 and 4); no overlay obscures either eye.
box(0, 0, 8, 8, 8, 'h')
art(8, 8, [
    'hhHiiHhh', 'hHHiHHjh', 'HsstsssH', 'dDssssDd',
    'weSssSew', 'bssssssb', 'sssbbsss', 'SssssssS',
])
art(0, 8, ['jhhHHihh','jhHHHihh','jhHhHhhh','jhHssshh',
           'jhHssShh','jhHShhhh','jhhHhhhj','jjhhhjjj'])
art(16, 8, ['HiiHHhhj','HiiHHhhj','HHHhHhhj','ssHhHhhj',
            'sSHhHhhj','hhHhHhhj','hhHhhjjj','jhhjjjjj'])
art(24, 8, ['jhHiiHhj','jhHiiHhj','jhHiHHhj','jhHiHHhj',
            'jhHiHHhj','jhHHHhhj','jjhHHhjj','jjjhhjjj'])
art(8, 0, ['jjhhHhhj','jhHiiHhj','hHiiHHhh','hHiHHHhh',
           'hHHHhhHh','hhHhHHHh','jhHHHHhj','jjhhhhjj'])

# HEAD OVERLAY: clipped pastel horn accessories and swept fringe.
art(40, 8, ['Yy....w.','yyH..ww.','yHH..Hw.', '........',
            '........','........','h.......','j.......'])
art(32, 8, ['.y......','.yy.....','hhhH....','hHiH....',
            'hHiH....','hHiH....','hHiH....','jhHH....'])
art(48, 8, ['......w.','.....ww.','....HHHh','....HHih',
            '....hHHh','....hHHh','....hhHh','.....hhh'])
art(56, 8, ['..HiiH..','.hHiiHh.','.hHiiHh.','.hHiHHh.',
            '.hHiHHh.','.hHiHHh.','.hHHHHh.','..hhhh..'])
art(40, 0, ['........','.yy...w.','.yH..ww.','.hHHHHh.',
            '.hHiiHh.','.hHiHHh.','..hHHh..','........'])

# BODY base: cropped cream tee, pink graphic, bare midriff, waistband.
box(16, 16, 8, 12, 4, 'w')
art(20, 20, ['SSsttsSS','wSsttSww','wwSSSwww','wpdddppw',
            'wppppPpw','wwyYYyww','wwywwyww','WwwwwwwW',
            'SSsttsSS','SssttsSS','dppppppd','PpppqqpP'])
art(32, 20, ['wwSSSSww','wwwwwwww','wwwwwwww','WwwwwwwW',
            'WwwwwwwW','WwwwwwwW','WWwwwwWW','WWWWWWWW',
            'SSssssSS','SSssssSS','dppppppd','PppqqppP'])

# Jacket overlay: open front, purple shadows, mint/ivory shoulder panels.
art(20, 36, ['vW....Wv','cW....Wc','qW....Wq','vW....Wv',
            'qW....Wq','vW....Wv','vW....Wv','VW....WV',
            '........','........','........','........'])
art(16, 36, ['vWcW','vWqW','vWqW','vWvW','vWqW','vWqW',
            'VWvW','VVWW','....','....','....','....'])
art(28, 36, ['WcWv','WqWv','WqWv','WvWv','WqWv','WqWv',
            'WvWV','WWVV','....','....','....','....'])
art(32, 36, ['vWccccWv','WccccccW','WqqqqqqW','WqqvvqqW',
            'WqqvvqqW','WqqqqqqW','vWqqqqWv','VVWWWWVV',
            '........','........','........','........'])
fill(20, 32, 8, 4, 'W')


def arm(u, v, mirror=False, outer=False):
    if not outer:
        box(u, v, 3, 12, 4, 's')
        front = ['WcW','cWW','WWq','Wqq','qWv','vWW',
                 'WWq','Wqv','dWd','sSs','sts','SSs']
        back = ['WcW','cWW','WWq','Wqq','qWv','vWW',
                'WWq','Wqv','dWd','sSs','sts','SSs']
    else:
        front = ['WcW','cWW','WWq','Wqq','qWv','vWW',
                 'WWq','Wqv','dWd','...','...','...']
        back = front
    for du, rows in [(4, front), (11, back)]:
        art(u + du, v + 4, [r[::-1] if mirror else r for r in rows])
    side = ['WccW','cWWW','WWqq','WqqW','qqWv','vWWW',
            'WWqq','Wqqv','dWWd']
    if not outer:
        side += ['sSSs','stts','SSSS']
    else:
        side += ['....'] * 3
    art(u, v + 4, side)
    art(u + 7, v + 4, side)
    fill(u + 4, v, 3, 4, 'W')


arm(40, 16)
arm(32, 48, True)
arm(40, 32, outer=True)
arm(48, 48, True, True)


def leg(u, v, mirror=False, outer=False):
    if not outer:
        box(u, v, 4, 12, 4, 's')
    front = ['Pppq','pppq','dPPd','stts','SstS','dWWd',
             'pWvp','pWvp','pWvp','pccp','WccW','dPPd']
    side = ['ppVy','ppVy','dPPd','ssSS','ssSS','dWWd',
            'pVqp','pVqp','pVqp','yYqp','yWqp','dPPd']
    back = ['PppP','PppP','dPPd','SssS','SssS','dWWd',
            'PVqp','PVqp','PVqp','pYqp','pWqp','dPPd']
    if outer:
        front = ['Pppq','pppq','dPPd','....','....','dWWd',
                 'pW.p','pW.p','pW.p','pccp','WccW','dPPd']
        side = ['ppVy','ppVy','dPPd','....','....','dWWd',
                'pVqp','pVqp','pVqp','yYqp','yWqp','dPPd']
        back = ['PppP','PppP','dPPd','....','....','dWWd',
                'PVqp','PVqp','PVqp','pYqp','pWqp','dPPd']
    for du, rows in [(0, side), (4, front), (8, side), (12, back)]:
        art(u + du, v + 4, [r[::-1] if mirror else r for r in rows])
    fill(u + 4, v, 4, 4, 'p')
    fill(u + 8, v, 4, 4, 'D')


leg(0, 16)
leg(16, 48, True)
leg(0, 32, outer=True)
leg(0, 48, True, True)

# Right-side long ponytail cascades over jacket: continuous front/side/back UV.
art(16, 36, ['jhHi','jhHi','jhHi','jhHi','jhHi','jhHi',
            'jhHi','jhHh','.jhH','.jhH','..jh','...j'])
art(20, 36, ['hH....Wv','hH....Wc','hH....Wq','hH....Wv',
            'hH....Wq','hH....Wv','hH....Wv','jh....WV',
            'jh......','.h......','........','........'])
art(32, 36, ['vWccccHh','WcccccHh','WqqqqqHh','WqqvvqHh',
            'WqqvvqHh','WqqqqqHh','vWqqqqHh','VVWWWWjh',
            '......jh','......h.','........','........'])


def png_bytes():
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
    raw = b''.join(b'\0' + bytes(c for p in row for c in p) for row in px)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 64, 64, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


data = png_bytes()
(ROOT / 'pinkki-alex.png').write_bytes(data)
for filename in ('pinkki-alex.bbmodel', 'pinkki-review.bbmodel'):
    project = ROOT / filename
    if project.exists():
        model = json.loads(project.read_text())
        model['textures'][0]['source'] = 'data:image/png;base64,' + base64.b64encode(data).decode()
        model['textures'][0].pop('path', None)
        model['textures'][0].pop('relative_path', None)
        project.write_text(json.dumps(model, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'size': [64,64], 'model': 'slim', 'bytes': len(data)}))

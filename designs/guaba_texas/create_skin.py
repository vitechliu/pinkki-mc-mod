"""guaba_texas: hand-authored 64x64 Slim UV pixel art, not a projected image."""
import base64
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).parent
PALETTE = {
    '.': None,
    'k': '201b23', 'h': '2c252e', 'H': '403641', 'i': '594c59',
    'd': '312a35', 'D': '49404c',
    'B': '45649e', 'c': '6687cc', 'C': '9cb9ea', 'n': '354a76',
    'w': 'fff8ed', 'W': 'e5dbd4', 'v': 'beb5b9', 'V': '91868f',
    'g': '8f6946', 'G': 'bb9563', 'y': 'e2c493',
    's': 'f5d9c9', 'S': 'ddb5b0', 't': 'ffe9db', 'b': 'ce9ca6',
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


# Head: swept blue-streaked bangs, low eyes, no mouth. The black pupils occupy
# the lower INNER cells of two 2x2 eye patches (columns 1/2 and 5/6).
box(0, 0, 8, 8, 8, 'h')
art(8, 8, ['hHHHiHhh','hHiHHHhh','HHicHHhh','hHHiHhhh',
           'HsHisssH','sCwsswCs','sBksskBs','SssssssS'])
art(0, 8, ['hHHHihhh','hHHiHHhh','hHHicHhh','hHHiCHhh',
           'hHHcBHhs','hHHiBHSs','hHHiBHhs','hhHcBhhh'])
art(16, 8, ['hhhHHHhh','hhHHHiHh','hhHHiHHh','hhHiHHHh',
            'sHHiHHHh','SHiHHHhh','sHHHihhh','hhcBHHhh'])
art(24, 8, ['hhHHHhhh','hHHHiHhh','hHHiHHhh','hHHHiHhh',
            'hHHiHHhh','hhHHiHhh','hhHHHhhh','hhhHHhhh'])
art(8, 0, ['hhhHHhhh','hhHHHihh','hHHiHHhh','hHiHHHhh',
           'hHiHcHhh','hhHHBhhh','hhhHHhhh','hhhhhhhh'])

# White ruffled headband wraps over the crown; raised fringe does not hide eyes.
art(40, 0, ['..WwwW..','.WwwwwW.','WwwWWwwW','wwW..Www',
            'wW....Ww','W......W','........','........'])
art(40, 8, ['WwWwwWwW','w.hHiH.w','w.HicH.w','W.HicH.W',
            '.H.H....','........','........','g......g'])
art(32, 8, ['.WwWwW..','WwWwW...','wW..HH..','W..HHi..',
            '..cCHi..','.cCBHH..','..BHHH..','...hHH..'])
art(48, 8, ['..WwWwW.','...WwWwW','..HH..Ww','..iHH..W',
            '..iHH...','..HHi...','..HHH...','..HHh...'])
art(56, 8, ['..HHHH..','.HHHiHH.','.HHiHHH.','.HHHiHH.',
            '.HHcBHH.','.HcCBHH.','..cBHH..','..BHHH..'])

# Blouse, ruffled bib, black bow, gold buttons and cinched black waist.
box(16, 16, 8, 12, 4, 'h')
art(20, 20, ['wWSSssWw','wwWkkWww','WwkyykwW','wwkkkkww',
            'WwGkkGwW','WwwGywwW','hWwGywWh','hhWWWWWh',
            'hDhhhhDh','hDhhhhDh','gGyGGyGg','hGgGGgGh'])
art(16, 20, ['Wwhh','wWhh','Wwhh','wWhh','Whhh','hhhh',
            'hDhh','hDhh','hDhh','hDhh','gGyG','hGgh'])
art(28, 20, ['hhwW','hhWw','hhwW','hhWw','hhhW','hhhh',
            'hhDh','hhDh','hhDh','hhDh','GyGg','hgGh'])
art(32, 20, ['hhhHHhhh','hhHHiHhh','hhHHiHhh','hhHHiHhh',
            'hhHHiHhh','hhHHiHhh','hhHHicBh','hHHHicBh',
            'hHHHiCBh','hhHHicBh','hGHHHBGh','hWhHHhWh'])
art(20, 16, ['hWwsswWh','WwwsswwW','wWssssWw','hhWssWhh'])

# Front overlay has scalloped ruffles, a neck clasp and round belt medallions.
art(20, 36, ['w......w','Ww....wW','.wkyykw.','..kkkk..',
            '.wGkkGw.','W..Gy..W','.W....W.','..WWWW..',
            '........','........','gGyGGyGg','.GgGGgG.'])
art(16, 36, ['Ww..','wW..','Ww..','wW..','W...','....',
            '....','....','....','....','gGyG','....'])
art(28, 36, ['..wW','..Ww','..wW','..Ww','...W','....',
            '....','....','....','....','GyGg','....'])
art(32, 36, ['.HHHiHH.','.HHHiHH.','.HHHiHH.','.HHHiHH.',
            '.HHHiHH.','.HHHiHH.','.HHHicB.','.HHHicB.',
            '.HHHiCB.','..HHiCB.','.W.HHBW.','WwW..WwW'])
fill(20, 32, 8, 4, 'W')


def arm(u, v, mirror=False, outer=False):
    if not outer:
        box(u, v, 3, 12, 4, 'h')
    front = ['Www','wWh','WhH','hHD','hDh','DhH',
             'hHh','hDh','hHh','WwW','sSs','stS']
    side = ['WwwW','wWWh','WhHH','hHDD','hDhH','DhHH',
            'hHHh','hDDh','hHHh','WwwW','sSSs','stts']
    back = ['Www','wWh','WhH','hHD','hDh','DhH',
            'hHh','hDh','hHh','WwW','sSs','stS']
    if outer:
        front = ['Www','wW.','W..','...','...','...',
                 '...','...','...','WwW','...','...']
        side = ['WwwW','wWW.','W...','....','....','....',
                '....','....','....','WwwW','....','....']
        back = front
    for du, rows in [(0, side), (4, front), (7, side), (11, back)]:
        art(u + du, v + 4, [r[::-1] if mirror else r for r in rows])
    fill(u + 4, v, 3, 4, 'W')


arm(40, 16)
arm(32, 48, True)
arm(40, 32, outer=True)
arm(48, 48, True, True)


def leg(u, v, mirror=False, outer=False):
    if not outer:
        box(u, v, 4, 12, 4, 's')
    front = ['hWww','hWwW','hWww','hWWw','hWwW','gGhg',
             'Ssts','Ssts','Ssts','WwwW','kssk','kkkk']
    side = ['hhHH','hHiH','hhHH','gHGh','hGgh','gGhg',
            'SssS','SssS','SssS','WwwW','kssk','kkkk']
    back = ['hhHi','hhHi','hhHi','hhHi','hcBH','hcBh',
            'SssS','SssS','SssS','WwwW','kssk','kkkk']
    if outer:
        front = ['.Www','.WwW','.Www','.WWw','.WwW','wWwW'] + ['....'] * 6
        side = ['hhHH','hHiH','hhHH','gHGh','hGgh','gGhg'] + ['....'] * 6
        back = ['.HHi','.HHi','.HHi','.HHi','.cB.','.cB.'] + ['....'] * 6
    for du, rows in [(0, side), (4, front), (8, side), (12, back)]:
        art(u + du, v + 4, [r[::-1] if mirror else r for r in rows])
    fill(u + 4, v, 4, 4, 'h')
    if not outer:
        fill(u + 8, v, 4, 4, 'k')


leg(0, 16)
leg(16, 48, True)
leg(0, 32, outer=True)
leg(0, 48, True, True)

# The reference's order slip hangs at the right hip; simplified as a small
# tan-edged paper panel on the skirt's outer side, with the same standard UV.
art(0, 36, ['GWWg','gWvg','gWWg','gWvg','gggg'])


def png_bytes():
    def chunk(kind, payload):
        return struct.pack('!I', len(payload)) + kind + payload + struct.pack('!I', zlib.crc32(kind + payload))
    raw = b''.join(b'\0' + bytes(c for p in row for c in p) for row in px)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 64, 64, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


data = png_bytes()
(ROOT / 'guaba_texas.png').write_bytes(data)
for name in ('guaba_texas.bbmodel', 'guaba_texas-review.bbmodel'):
    path = ROOT / name
    model = json.loads(path.read_text())
    model['name'] = 'guaba_texas'
    model['textures'][0]['source'] = 'data:image/png;base64,' + base64.b64encode(data).decode()
    model['textures'][0]['name'] = 'guaba_texas.png'
    for key in ('path', 'relative_path'):
        model['textures'][0].pop(key, None)
    path.write_text(json.dumps(model, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'asset': 'guaba_texas', 'size': [64, 64], 'model': 'slim', 'bytes': len(data)}))

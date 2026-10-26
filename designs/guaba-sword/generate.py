#!/usr/bin/env python3
"""Render the editable 16px Guaba sword pixel pattern (Pillow required)."""
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).resolve().parents[2]
# Diagonal vanilla diamond-sword silhouette; original Guaba flesh/rind palette and seeds.
ROWS = [
'             111',
'            1AA.',
'           1A5A.',
'          1ASA. ',
'         1A56.  ',
'        1A56.   ',
'  11   16S6.    ',
'  121 1656.     ',
'   13.636.      ',
'   13326.       ',
'    121.        ',
'   78.11.       ',
'  794 ..1.      ',
'1184    ..      ',
'12.             ',
'...             ',
]
PALETTE = {' ': (0,0,0,0), '.':'50321b', '1':'925e36', '2':'d99866',
           '3':'b2794c', '4':'50321b', '5':'bf3123', '6':'df5542',
           '7':'925e36', '8':'b2794c', '9':'d6b69b', 'A':'f69078', 'S':'502d25'}
image = Image.new('RGBA', (16,16))
for y, row in enumerate(ROWS):
    assert len(row) == 16
    for x, symbol in enumerate(row):
        color = PALETTE[symbol]
        image.putpixel((x,y), color if isinstance(color,tuple) else (*bytes.fromhex(color),255))
for target in ('designs/guaba-sword/guaba_sword.png',
               'versions/1.20.1/src/main/resources/assets/pinkkicore/textures/item/guaba_sword.png',
               'wiki/public/images/guaba_sword.png'):
    image.save(ROOT / target)
image.resize((256,256), Image.Resampling.NEAREST).save(ROOT/'designs/guaba-sword/preview.png')

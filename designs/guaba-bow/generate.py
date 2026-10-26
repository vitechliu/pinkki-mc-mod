#!/usr/bin/env python3
"""Render the four editable bow pixel patterns using the Guaba palette. Requires Pillow."""
import json
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).resolve().parents[2]
PALETTE = {' ': None, '.':'50321b', 'o':'b2794c', 'r':'f69078', 'h':'bf3123',
           's':'6c5947', 't':'bca48a', 'u':'e7d4b6', 'w':'ffffff', 'v':'b1b1b1', 'a':'d8d8d8', 'S':'502d25'}
frames = json.loads((ROOT/'designs/guaba-bow/pixels.json').read_text())
preview = Image.new('RGBA', (16*4,16))
for index,(name,rows) in enumerate(frames.items()):
    image = Image.new('RGBA',(16,16))
    assert len(rows)==16
    for y,row in enumerate(rows):
        assert len(row)==16
        for x,symbol in enumerate(row):
            color = PALETTE[symbol]
            if color: image.putpixel((x,y),(*bytes.fromhex(color),255))
    for target in [ROOT/f'designs/guaba-bow/{name}.png',
                   ROOT/f'versions/1.20.1/src/main/resources/assets/pinkkicore/textures/item/{name}.png']:
        image.save(target)
    if index==0: image.save(ROOT/'wiki/public/images/guaba_bow.png')
    preview.paste(image,(16*index,0))
preview.resize((768,192),Image.Resampling.NEAREST).save(ROOT/'designs/guaba-bow/preview.png')

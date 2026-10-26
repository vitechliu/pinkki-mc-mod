#!/usr/bin/env python3
"""Build the lightning-acquisition diagram from the actual shipped item textures."""
import base64
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
icons = ROOT / 'versions/1.20.1/src/main/resources/assets/pinkkicore/textures/item'
def png(name):
    return 'data:image/png;base64,' + base64.b64encode((icons / f'{name}.png').read_bytes()).decode()

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="480" height="168" viewBox="0 0 480 168" role="img" aria-labelledby="title">
<title id="title">地上的 N 个瓜巴遭受雷击，得到 N 个雷霆瓜巴</title>
<rect x="1" y="1" width="478" height="166" rx="14" fill="#eaf4fa" stroke="#8caabc"/>
<image href="{png('guaba')}" x="56" y="24" width="80" height="80" style="image-rendering:pixelated"/>
<image href="{png('thunder_guaba')}" x="344" y="24" width="80" height="80" style="image-rendering:pixelated"/>
<path d="M245 20 218 64H239L231 92 262 49H241Z" fill="#f5c842" stroke="#78601f" stroke-width="2"/>
<path d="M176 78H213M267 78H307L296 68M307 78 296 88" fill="none" stroke="#3b5b70" stroke-width="3" stroke-linejoin="round"/>
<g font-family="sans-serif" font-size="17" text-anchor="middle" fill="#203b4c">
<text x="96" y="133">瓜巴 × N</text><text x="240" y="122">地上遭受雷击</text><text x="384" y="133">雷霆瓜巴 × N</text>
</g></svg>
'''
(ROOT / 'wiki/public/images/thunder_guaba_conversion.svg').write_text(svg)

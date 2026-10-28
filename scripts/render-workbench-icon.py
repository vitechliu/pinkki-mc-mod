#!/usr/bin/env python3
"""Render the actual block atlas into a small isometric Wiki icon (no game client)."""
import argparse
import base64
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft-assets', type=Path, help='Optional local 1.20.1 resource JAR, for vanilla dirt and crafting-table icons')
    args = parser.parse_args()
    texture = ROOT/'versions/1.20.1/src/main/resources/assets/pinkkicore/textures/block/guaba_workbench.png'
    source = base64.b64encode(texture.read_bytes()).decode()
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="64" height="64" viewBox="0 0 32 32">
  <title>瓜巴工作台</title>
  <defs>
    <image id="atlas" width="64" height="64" image-rendering="pixelated" xlink:href="data:image/png;base64,{source}"/>
    <svg id="top" width="16" height="16" viewBox="0 0 32 32" overflow="hidden"><use xlink:href="#atlas"/></svg>
    <svg id="side" width="16" height="16" viewBox="0 32 32 26" preserveAspectRatio="none" overflow="hidden"><use xlink:href="#atlas"/></svg>
  </defs>
  <use xlink:href="#top" transform="matrix(.875 .4375 -.875 .4375 16 1)"/>
  <use xlink:href="#side" transform="matrix(.875 .4375 0 .8 2 8)"/>
  <path d="M2 8 16 15 16 27.8 2 20.8Z" fill="#000" opacity=".12"/>
  <use xlink:href="#side" transform="matrix(.875 -.4375 0 .8 16 15)"/>
  <path d="M16 15 30 8 30 20.8 16 27.8Z" fill="#000" opacity=".25"/>
</svg>
'''
    (ROOT/'wiki/public/images/guaba_workbench.svg').write_text(svg)
    if args.minecraft_assets:
        with zipfile.ZipFile(args.minecraft_assets) as archive:
            dirt = base64.b64encode(archive.read('assets/minecraft/textures/block/dirt.png')).decode()
            faces = {face: base64.b64encode(archive.read(
                f'assets/minecraft/textures/block/crafting_table_{face}.png')).decode()
                for face in ('top', 'front', 'side')}
        # Same isometric projection as the machine icon, using the actual vanilla block faces.
        table = '<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 32 32"><title>工作台</title>'
        for face, matrix in [('top', '.875 .4375 -.875 .4375 16 1'),
                             ('front', '.875 .4375 0 .875 2 8'),
                             ('side', '.875 -.4375 0 .875 16 15')]:
            table += (f'<image width="16" height="16" image-rendering="pixelated" '
                      f'transform="matrix({matrix})" href="data:image/png;base64,{faces[face]}"/>')
        table += '<path d="M2 8 16 15 16 29 2 22Z" fill="#000" opacity=".12"/>'
        table += '<path d="M16 15 30 8 30 22 16 29Z" fill="#000" opacity=".25"/></svg>\n'
        (ROOT/'wiki/public/images/minecraft/crafting_table.svg').write_text(table)
        mud = (ROOT/'wiki/public/images/minecraft/mud.svg').read_text()
        start = mud.index('data:image/png;base64,')+len('data:image/png;base64,')
        end = mud.index('"', start)
        (ROOT/'wiki/public/images/minecraft/dirt.svg').write_text(mud[:start]+dirt+mud[end:])
    print('Rendered Wiki block icons from actual textures (isometric projection, not a game screenshot).')


if __name__ == '__main__':
    main()

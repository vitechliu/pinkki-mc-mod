#!/usr/bin/env python3
"""Generate the editable projects, game assets and offline reviewer from the same models."""
import base64
import io
import json
import uuid
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
ASSETS = ROOT / 'versions/1.20.1/src/main/resources/assets/pinkkicore'
WIKI = ROOT / 'wiki/public/images'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
ATLAS = 128


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')


def uid(key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f'https://pinkkicore.local/zhang-galgame/{key}'))


def uri(image):
    buffer = io.BytesIO()
    image.save(buffer, format='PNG')
    return 'data:image/png;base64,' + base64.b64encode(buffer.getvalue()).decode()


def texture_entry(name, image):
    return dict(name=f'{name}.png', id='0', uuid=uid(name + '/texture'), source=uri(image),
                mode='bitmap', saved=False, width=image.width, height=image.height,
                uv_width=image.width, uv_height=image.height)


def tile(index):
    x, y = (index % 4) * 32, (index // 4) * 32
    return [x, y, x + 32, y + 32]


def swatch(image, index, base, light, dark, mottled=False):
    x, y, _, _ = tile(index)
    draw = ImageDraw.Draw(image)
    draw.rectangle((x, y, x + 31, y + 31), fill=base)
    draw.line((x, y, x + 31, y), fill=light)
    draw.line((x, y + 1, x, y + 31), fill=light)
    draw.line((x, y + 31, x + 31, y + 31), fill=dark)
    draw.line((x + 31, y + 1, x + 31, y + 31), fill=dark)
    if mottled:
        for i in range(17):
            u, v = x + 2 + (i * 17 % 27), y + 2 + (i * 13 % 27)
            draw.line((u, v, u + i % 3, v), fill=light if i % 2 else dark)


def geo_uv(index):
    x, y, _, _ = tile(index)
    return dict(uv=[x, y], uv_size=[32, 32])


def zhang():
    name = 'zhang'
    atlas = Image.new('RGBA', (ATLAS, ATLAS))
    for index, shade in enumerate(('#fafafa', '#f3f3f3', '#eaeaea', '#dfdfdf')):
        swatch(atlas, index, shade, '#ffffff', '#dedede')
    draw = ImageDraw.Draw(atlas)
    ink = '#161616'
    # 弓 and 长, drawn as connected brush strokes on the front face only.
    for stroke in (
        [(4, 7), (12, 7), (11, 13), (5, 13), (4, 18), (12, 18), (10, 25), (7, 27)],
        [(23, 7), (21, 10), (16, 12)],
        [(14, 16), (28, 15)],
        [(18, 8), (17, 26), (22, 23)],
        [(20, 17), (23, 22), (28, 27)],
    ):
        draw.line(stroke, fill=ink, width=2)
    atlas.save(ASSETS / 'textures/entity/zhang.png')
    specs = [
        ('body', [0, 12, 0], [-4, 12, -2], [8, 10, 4], 1),
        ('head', [0, 22, 0], [-4, 22, -4], [8, 8, 8], 0),
        ('left_arm', [4, 21, 0], [4, 12, -1.5], [3, 10, 3], 1),
        ('right_arm', [-4, 21, 0], [-7, 12, -1.5], [3, 10, 3], 1),
        ('left_leg', [2, 12, 0], [0, 0, -1.75], [3.75, 12, 3.5], 1),
        ('right_leg', [-2, 12, 0], [-3.75, 0, -1.75], [3.75, 12, 3.5], 1),
    ]
    bones, elements, groups = [], [], []
    for part, pivot, origin, size, front in specs:
        colors = dict(north=front, south=1, east=2, west=2, up=1, down=3)
        cube = dict(origin=origin, size=size, uv={face: geo_uv(colors[face]) for face in FACES})
        bones.append(dict(name=part, pivot=pivot, cubes=[cube], **({'parent': 'body'} if part != 'body' else {})))
        faces = {face: dict(uv=tile(colors[face]), texture=0) for face in FACES}
        for face in ('up', 'down'):
            u, v, end_u, end_v = faces[face]['uv']
            faces[face]['uv'] = [end_u, end_v, u, v]
        start = [-origin[0] - size[0], *origin[1:]]
        elements.append(dict(name=part, type='cube', uuid=uid(part + '/cube'),
                             **{'from': start, 'to': [a + b for a, b in zip(start, size)]},
                             origin=[-pivot[0], *pivot[1:]], rotation=[0, 0, 0],
                             autouv=0, box_uv=False, faces=faces))
        groups.append(dict(name=part, uuid=uid(part), origin=[-pivot[0], *pivot[1:]],
                           rotation=[0, 0, 0], export=True, isOpen=True, children=[uid(part + '/cube')]))
    groups[0]['children'].extend(groups[1:])
    shape = dict(description=dict(identifier='geometry.zhang', texture_width=ATLAS, texture_height=ATLAS,
                                  visible_bounds_width=2, visible_bounds_height=2.5,
                                  visible_bounds_offset=[0, 1, 0]), bones=bones)
    write(ASSETS / 'geo/zhang.geo.json', {'format_version': '1.12.0', 'minecraft:geometry': [shape]})
    idle = {'body': {'rotation': {'0.0': [0, 0, -.75], '1.0': [0, 0, .75], '2.0': [0, 0, -.75]}}}
    walk = {}
    for part, amount in [('left_leg', 32), ('right_leg', -32), ('left_arm', -23), ('right_arm', 23)]:
        walk[part] = {'rotation': {'0.0': [amount, 0, 0], '.4': [-amount, 0, 0], '.8': [amount, 0, 0]}}
    animations = {
        'animation.zhang.idle': dict(loop=True, animation_length=2, bones=idle),
        'animation.zhang.walk': dict(loop=True, animation_length=.8, bones=walk),
    }
    write(ASSETS / 'animations/zhang.animation.json', dict(format_version='1.8.0', animations=animations))
    editor_animations = []
    for motion, animation in animations.items():
        animators = {}
        for part, channels in animation['bones'].items():
            keyframes = []
            for channel, frames in channels.items():
                for seconds, values in frames.items():
                    keyframes.append(dict(channel=channel, time=float(seconds), interpolation='linear',
                                          uuid=uid(f'{motion}/{part}/{seconds}'), color=-1,
                                          data_points=[dict(zip('xyz', map(str, values)))]))
            animators[uid(part)] = dict(name=part, type='bone', keyframes=keyframes)
        editor_animations.append(dict(name=motion, uuid=uid(motion), loop='loop', snapping=20,
                                      length=animation['animation_length'], animators=animators))
    write(HERE / 'zhang.bbmodel', dict(
        meta=dict(format_version='4.10', model_format='geckolib_model', box_uv=False),
        name=name, model_identifier=name, geckolib_model_type='Entity', geckolib_modid='pinkkicore',
        visible_box=[2, 2.5, 1], resolution=dict(width=ATLAS, height=ATLAS), elements=elements,
        outliner=groups[:1], animations=editor_animations, textures=[texture_entry(name, atlas)]))
    return dict(geometry=shape, animations=animations, texture=uri(atlas), height=30)


def tile_uv(index):
    return [pixel / 8 for pixel in tile(index)]


def block(name, pieces, atlas):
    elements = []
    for label, start, end, tiles in pieces:
        faces = {face: dict(uv=tile_uv(tiles.get(face, tiles['all'])), texture='#atlas') for face in FACES}
        elements.append(dict(name=label, **{'from': start, 'to': end}, faces=faces))
    display = dict(gui=dict(rotation=[27, 225, 0], translation=[0, 0, 0], scale=[.78]*3),
                   ground=dict(scale=[.4]*3, translation=[0, 2, 0]),
                   fixed=dict(scale=[.7]*3))
    path = f'pinkkicore:block/{name}'
    model = dict(credit='PinkkiCore', textures=dict(atlas=path, particle=path), elements=elements, display=display)
    write(ASSETS / f'models/block/{name}.json', model)
    atlas.save(ASSETS / f'textures/block/{name}.png')
    editor_elements = []
    for element in elements:
        editor_elements.append(dict(
            **{'name': element['name'], 'from': element['from'], 'to': element['to']},
            uuid=uid(name + '/' + element['name']), type='cube', origin=[8, 0, 8], rotation=[0, 0, 0],
            autouv=0, box_uv=False,
            faces={face: dict(uv=[value * 8 for value in data['uv']], texture=0)
                   for face, data in element['faces'].items()}))
    write(HERE / f'{name}.bbmodel', dict(
        meta=dict(format_version='4.10', model_format='java_block', box_uv=False),
        name=name, model_identifier=name, resolution=dict(width=ATLAS, height=ATLAS), display=display,
        elements=editor_elements, outliner=[e['uuid'] for e in editor_elements],
        textures=[texture_entry(name, atlas)]))
    draw_block_icon(name, pieces, atlas)
    return dict(model=model, texture=uri(atlas), height=max(piece[2][1] for piece in pieces))


def draw_block_icon(name, pieces, atlas):
    im = Image.new('RGBA', (160, 160))
    faces = []

    def project(x, y, z):
        return (80 + (x - z) * 4.6, 126 - y * 6 + (x + z - 16) * 1.85)

    for _, (x, y, z), (X, Y, Z), tiles in pieces:
        for face, points, light in (
            ('north', [(x, Y, z), (X, Y, z), (X, y, z), (x, y, z)], .91),
            ('west', [(x, Y, Z), (x, Y, z), (x, y, z), (x, y, Z)], .75),
            ('up', [(x, Y, Z), (X, Y, Z), (X, Y, z), (x, Y, z)], 1),
        ):
            depth = sum(u + w - .8 * v for u, v, w in points) / 4
            faces.append((depth, points, tiles.get(face, tiles['all']), light))
    pixels = ImageDraw.Draw(im)
    for _, (a, b, _, d), material, light in sorted(faces, key=lambda entry: -entry[0]):
        x, y, _, _ = tile(material)
        for v in range(32):
            for u in range(32):
                corners = []
                for i, j in [(u,v), (u+1,v), (u+1,v+1), (u,v+1)]:
                    corners.append(project(*(a[k]+(b[k]-a[k])*i/32+(d[k]-a[k])*j/32 for k in range(3))))
                rgb = atlas.getpixel((x + u, y + v))
                pixels.polygon(corners, fill=tuple(round(c * light) for c in rgb[:3]) + (rgb[3],))
    im.save(WIKI / f'{name}.png')


def sword():
    atlas = Image.new('RGBA', (ATLAS, ATLAS))
    for index, palette in enumerate([
        ('#828186', '#a4a1a6', '#605d63'), ('#b9bec0', '#d5dadd', '#757a80'),
        ('#626271', '#93939e', '#4b4957'), ('#573d37', '#845f4b', '#3f2d2a'),
    ]):
        swatch(atlas, index, *palette, mottled=index in (0, 1))
    return block('sword_in_stone', [
        ('圆石薄底座', [0,0,1], [16,3,15], {'all':0}),
        ('石剑刃', [6.75,2.7,7.48], [9.25,11.5,8.52], {'all':1}),
        ('剑脊', [7.45,3,7.24], [8.55,11.4,8.76], {'all':1}),
        ('护手', [4.1,10.8,7.05], [11.9,12,8.95], {'all':2}),
        ('剑柄', [7.15,11.99,7.22], [8.85,14.7,8.78], {'all':3}),
        ('柄尾', [6.8,14.7,7], [9.2,15.7,9], {'all':2}),
    ], atlas)


def arcade():
    atlas = Image.new('RGBA', (ATLAS, ATLAS))
    for index, palette in enumerate([
        ('#d95991', '#f99cc4', '#a13867'), ('#f395bf', '#ffcee6', '#c75c91'),
        ('#563650', '#87526f', '#352641'), ('#362648', '#5d436c', '#261a35'),
        ('#ffaccd', '#ffe4f0', '#d974a4')
    ]):
        swatch(atlas, index, *palette)
    draw = ImageDraw.Draw(atlas)
    x, y, _, _ = tile(5)
    screen = atlas.crop((x, y, x+32, y+32))
    pixel = ImageDraw.Draw(screen)
    pixel.rectangle((0,0,31,31), fill='#734978')
    pixel.rectangle((1,1,30,20), fill='#e799bb')
    pixel.rectangle((1,9,30,20), fill='#e7b7d4')
    pixel.polygon([(1,18),(7,12),(12,17),(18,12),(23,17),(30,13),(30,20),(1,20)], fill='#9a82b4')
    pixel.ellipse((21,3,26,8), fill='#ffe1bb')
    pixel.polygon([(11,9),(14,6),(18,6),(21,9),(21,18),(10,18),(10,12)], fill='#643f72')
    pixel.rectangle((13,10,18,15), fill='#ffddd8')
    pixel.point((14,12), fill='#543450')
    pixel.point((17,12), fill='#543450')
    pixel.polygon([(12,20),(13,16),(18,16),(20,20)], fill='#b94b85')
    pixel.rectangle((1,21,30,30), fill='#4a304f', outline='#ffd3e9')
    for line_y, width in [(24,16),(27,11)]:
        pixel.line((4,line_y,4+width,line_y), fill='#ffd9e8')
    pixel.point((27,27), fill='#ff92ba')
    atlas.paste(screen, (x, y))
    return block('galgame_arcade', [
        ('桌面底座', [1,0,1], [15,1.45,15], {'all':0,'up':1}),
        ('键盘', [2,1.45,1.75], [11,2.15,6.75], {'all':4}),
        ('屏幕底座', [4,1.45,8], [12,2.45,13], {'all':1}),
        ('支架', [7,2.45,10], [9,5,12], {'all':0}),
        ('粉色屏幕壳', [2,4,4], [14,15,13], {'all':1,'south':0}),
        ('屏幕边框', [2.75,4.75,3.95], [13.25,14.25,4], {'all':2}),
        ('恋爱游戏画面', [3.25,5.5,3.9], [12.75,13.75,3.95], {'all':3,'north':5}),
        ('电源指示灯', [11.8,4.9,3.89], [12.75,5.25,3.95], {'all':4}),
        *[(f'键帽 {column}-{row}', [x,2.15,z], [x+1.08,2.4,z+.78], {'all':2})
          for column, x in enumerate([2.5,3.83,5.16,6.49,7.82,9.15])
          for row, z in enumerate([2.15,3.25,4.35,5.45])],
        ('鼠标', [11.7,1.45,3.2], [13.8,2.5,5.6], {'all':1}),
        ('滚轮', [12.5,2.48,3.8], [13,2.65,4.42], {'all':2}),
    ], atlas)


HEART = [
    '................', '................', '..####....####..', '.######..######.',
    '.##############.', '.##############.', '.##############.', '..############..',
    '...##########...', '....########....', '.....######.....', '......####......',
    '.......##.......', '................', '................', '................',
]


def icons():
    def heart(rainbow):
        image = Image.new('RGBA', (16, 16))
        rows = ['#ff6d90','#ff9567','#f4cc73','#a9dc8b','#65d7bc','#73badc','#9197e7','#b789cf']
        for y, row in enumerate(HEART):
            for x, cell in enumerate(row):
                if cell != '#':
                    continue
                border = any(HEART[y+dy][x+dx] != '#' for dx, dy in [(-1,0),(1,0),(0,-1),(0,1)])
                color = ('#56334d' if rainbow else '#962c64') if border else (
                    rows[min(len(rows)-1,max(0,(x+y-5)//2))] if rainbow else ('#ff9ace' if y < 7 else '#e84b92'))
                if (x,y) in [(3,4),(4,4),(3,5)]:
                    color = '#fff2f4'
                image.putpixel((x,y), ImageColor.getrgb(color) + (255,))
        return image

    soul = Image.new('RGBA', (16, 16))
    draw = ImageDraw.Draw(soul)
    draw.polygon([(2,14),(3,10),(4,8),(4,5),(6,2),(10,1),(13,3),(14,6),(13,10),(11,12),(8,12),(7,14),(5,13)],
                 fill='#b44784')
    draw.polygon([(4,12),(6,8),(5,6),(6,4),(9,2),(12,3),(13,6),(12,9),(10,11),(8,11),(6,12)],
                 fill='#fd8fc4')
    draw.line([(7,5),(8,4),(10,4)], fill='#ffe2ef')
    draw.point([(8,7),(11,7)], fill='#762a59')
    draw.point([(2,8),(14,11),(5,1)], fill='#fca9cd')
    person = Image.new('RGBA', (16, 16))
    draw = ImageDraw.Draw(person)
    draw.polygon([(2,14),(2,12),(4,10),(6,9),(6,8),(5,7),(5,4),(6,2),(9,2),(11,4),(11,7),
                  (10,8),(10,9),(12,10),(14,12),(14,14)], fill='#171522')
    draw.line([(3,13),(4,11),(6,10),(6,9)], fill='#57455f')
    draw.line([(5,5),(6,3),(9,3)], fill='#57455f')
    draw.line([(5,13),(7,12),(9,12),(11,13)], fill='#30283b')
    for name, image in [('zhang_fav',heart(True)), ('galgame_heart',heart(False)),
                        ('favorite_person',person), ('galgame_soul',soul)]:
        image.save(ASSETS / f'textures/item/{name}.png')
        image.save(WIKI / f'{name}.png')
    heart(False).resize((18,18), Image.Resampling.NEAREST).save(ASSETS / 'textures/mob_effect/galgame.png')


def main():
    payload = {'zhang': zhang(), 'sword_in_stone': sword(), 'galgame_arcade': arcade()}
    icons()
    template = (HERE / 'review-template.html').read_text()
    (HERE / 'review.html').write_text(template.replace('__GALGAME_ASSETS__',
        json.dumps(payload, ensure_ascii=False, separators=(',', ':'))))


if __name__ == '__main__':
    main()

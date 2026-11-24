#!/usr/bin/env python3
"""Generate the Niumo source projects, shipped models and offline model reviewer."""
import json
from pathlib import Path
import re
import runpy
import sys
import uuid

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
support = runpy.run_path(str(HERE.parent / 'zhang-galgame/generate.py'))
ASSETS, WIKI, FACES = (support[key] for key in ('ASSETS', 'WIKI', 'FACES'))
write, uri, tile, swatch = (support[key] for key in ('write', 'uri', 'tile', 'swatch'))


def uid(key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://pinkkicore.local/niumo/' + key))


def texture(name, image):
    return dict(name=name + '.png', id='0', uuid=uid(name + '/texture'), source=uri(image),
                mode='bitmap', saved=False, width=128, height=128, uv_width=128, uv_height=128)


def atlas(palettes):
    image = Image.new('RGBA', (128, 128))
    for i, colors in enumerate(palettes):
        swatch(image, i, *colors, mottled=i in (0, 2, 4))
    return image


def front(image, index, paint):
    x, y, x2, y2 = tile(index)
    part = image.crop((x, y, x2, y2))
    paint(ImageDraw.Draw(part))
    image.paste(part, (x, y))


def bull_art():
    image = atlas([
        ('#402a26', '#785543', '#26191a'), ('#694b3d', '#8a6451', '#432a24'),
        ('#c09a76', '#deb896', '#8d6453'), ('#292021', '#524043', '#1c1418'),
        ('#50342c', '#795540', '#392420'), ('#ab8a66', '#e8d0a3', '#69533e'),
        ('#bd9f87', '#d7b8a1', '#775e54'), ('#72524a', '#a77a62', '#452c29'),
    ])
    def face(draw):
        draw.polygon([(12,1),(19,1),(21,8),(18,13),(20,22),(13,23),(10,11)], fill='#b89e82')
        for x in (3, 22):
            draw.rectangle((x, 12, x+6, 17), fill='#1c1618')
            draw.rectangle((x+1, 12, x+2, 13), fill='#fcd39b')
    front(image, 1, face)
    def snout(draw):
        for x in (6, 21):
            draw.rectangle((x, 12, x+5, 17), fill='#583a35')
        draw.line([(8,25),(24,25)], fill='#886252', width=2)
    front(image, 2, snout)
    def chest(draw):
        draw.polygon([(0,0),(13,0),(19,13),(15,24),(23,31),(6,31),(3,18)], fill='#ad9477')
        draw.line((0,27,15,30), fill='#846447', width=2)
    front(image, 4, chest)
    return image


def lao_art():
    image = atlas([
        ('#345985', '#557eaa', '#233b63'), ('#84a8c1', '#b0cbce', '#547a9e'),
        ('#3b677a', '#709898', '#264757'), ('#bc839d', '#dba6aa', '#734e76'),
        ('#507483', '#8ca7a4', '#335165'), ('#3c6438', '#7c9a56', '#27412e'),
        ('#ab3445', '#e55d60', '#6c263d'), ('#252b47', '#363f6d', '#181c32'),
        ('#dd5a56', '#f07b71', '#972b3d'),
    ])
    def face(draw):
        draw.polygon([(2,11),(8,4),(13,7),(14,16),(9,20),(3,18)], fill='#739cae')
        draw.polygon([(18,16),(20,7),(27,4),(30,11),(29,18),(23,20)], fill='#739cae')
        for x in (5, 21):
            draw.rectangle((x, 10, x+5, 15), fill='#1c2741')
            draw.rectangle((x+1, 10, x+2, 11), fill='#d5f7f3')
        draw.polygon([(11,22),(20,21),(25,24),(20,28),(12,27),(8,24)], fill='#283551')
        draw.rectangle((16,21,18,23), fill='#f0daba')
        draw.rectangle((20,21,22,23), fill='#f0daba')
    front(image, 0, face)
    return image


def rotations(frames):
    return {'rotation': {str(t): v for t, v in frames}}


def motions(bull):
    stride = .8 if bull else .55
    def swing(amount, rest=0):
        return rotations([(0,[rest+amount,0,0]), (stride/2,[rest-amount,0,0]), (stride,[rest+amount,0,0])])
    idle = {'body': rotations([(0,[0,0,-1]), (1,[0,0,1]), (2,[0,0,-1])])}
    walk = dict(left_leg=swing(28), right_leg=swing(-28), left_arm=swing(-22 if bull else -9),
                right_arm=swing(22 if bull else 28))
    strike = dict(right_arm=rotations([(0,[0,0,0]), (.15,[30,0,-7]), (.3,[-88,-6,9]), (.55,[0,0,0])]),
                  body=rotations([(0,[0,0,0]), (.15,[-6,-10,0]), (.3,[8,12,0]), (.55,[0,0,0])]))
    if not bull:
        idle['ear_left'] = rotations([(0,[0,0,0]), (1,[0,0,8]), (2,[0,0,0])])
        idle['ear_right'] = rotations([(0,[0,0,0]), (1,[0,0,-8]), (2,[0,0,0])])
        idle['left_arm'] = rotations([(0,[-12,0,0]), (1,[-9,0,0]), (2,[-12,0,0])])
    return {f'animation.niumo.{name}':dict(loop=loop, animation_length=length, bones=bones)
            for name, loop, length, bones in [('idle', True, 2, idle), ('walk', True, stride, walk),
                                             ('attack', False, .55, strike)]}


def cube(label, origin, size, index, **faces):
    return label, origin, size, {face: faces.get(face, index) for face in FACES}


def bull_parts():
    return [
        ('body',None,[0,13,0],[cube('hide',[-4.5,13,-2.8],[9,10,5.6],0,north=4)]),
        ('head','body',[0,23,0],[
            cube('skull',[-4.25,23,-3.5],[8.5,7,7],0,north=1),
            cube('muzzle',[-3.4,22.7,-5.4],[6.8,3.1,3.05],7,north=2),
            cube('left ear',[4,26,0],[2.8,1.35,2.15],0,north=7),
            cube('right ear',[-6.8,26,0],[2.8,1.35,2.15],0,north=7),
            cube('left horn',[2.8,29,-0.5],[2,2.1,2],5),
            cube('right horn',[-4.8,29,-0.5],[2,2.1,2],5),
            cube('left tip',[3.65,31.1,-0.25],[1.2,1.3,1.4],6),
            cube('right tip',[-4.85,31.1,-0.25],[1.2,1.3,1.4],6),
        ]),
        *[(f'{side}_arm','body',[x,22,0],[
            cube(side+' arm',[x-1.6,13,-1.6],[3.2,9.4,3.2],0),
            cube(side+' hand',[x-1.6,11.5,-1.6],[3.2,1.5,3.2],3)])
          for side, x in [('left',6.1), ('right',-6.1)]],
        *[(f'{side}_leg','body',[x,13,0],[
            cube(side+' leg',[x-1.88,2,-1.9],[3.76,11,3.8],0),
            cube(side+' hoof',[x-1.88,0,-2.35],[3.76,2,4.25],3)])
          for side, x in [('left',2.24), ('right',-2.24)]],
    ]


def lao_parts():
    return [
        ('body',None,[0,7,0],[cube('body',[-3.5,7,-2.35],[7,7.6,4.7],2),
                              cube('bib',[-2,7.9,-2.55],[4,5.5,.35],4,north=1)]),
        ('head','body',[0,14,0],[
            cube('face',[-4.45,14,-3.6],[8.9,7.9,7.2],2,north=0),
            cube('nose',[-1.5,17,-4.45],[3,1.65,1.65],7),
            cube('crest',[-.8,21,-1],[1.6,1.1,2.75],4)]),
        *[(f'ear_{side}','head',[sign*4.1,18.5,.3],[
            cube(side+' ear',[ear_x,18.35,.35],[4.95,2.95,1.25],2,north=3),
            cube(side+' ear tip',[tip_x,20.5,.45],[1.8,1.85,1.0],2,north=3)])
          for side, sign, ear_x, tip_x in [('left',1,4.1,7.85),('right',-1,-9.05,-9.65)]],
        ('left_arm','body',[3.5,13.9,0],[
            cube('rose hand',[3.4,8.15,-1.2],[2.8,6.1,2.8],2),
            cube('stem',[4.45,6.3,-1.65],[.55,8.55,.55],5),
            cube('leaf',[4.95,10.75,-1.8],[1.55,.65,.8],5),
            cube('rose calyx',[4.0,13.55,-2.1],[1.55,.9,1.55],5),
            cube('rose petals',[3.35,14.15,-2.75],[2.85,2.05,2.85],6,north=8),
            cube('rose centre',[3.95,16.2,-2.15],[1.65,.55,1.65],6,up=8)]),
        ('right_arm','body',[-3.5,13.9,0],[cube('right arm',[-6.2,8.15,-1.2],[2.8,6.1,2.8],2)]),
        *[(f'{side}_leg','body',[x,7,0],[cube(side+' foot',[x-1.45,0,-1.75],[2.9,7,3.45],2,north=4)])
          for side,x in [('left',1.65),('right',-1.65)]],
    ]


def mob(name, image, parts, height):
    elements, groups, bones, roots = [], {}, [], []
    for part, parent, pivot, pieces in parts:
        group = dict(name=part, uuid=uid(f'{name}/{part}'), origin=[-pivot[0], *pivot[1:]],
                     rotation=[0,0,0], export=True, isOpen=True, children=[])
        groups[part] = group
        (groups[parent]['children'] if parent else roots).append(group)
        bone = dict(name=part, pivot=pivot, cubes=[], **({'parent':parent} if parent else {}))
        bones.append(bone)
        for label, origin, size, colors in pieces:
            faces = {}
            uv = {}
            for face, color in colors.items():
                u,v,x,y = tile(color)
                uv[face] = dict(uv=[u,v], uv_size=[x-u,y-v])
                faces[face] = dict(uv=[x,y,u,v] if face in ('up','down') else [u,v,x,y], texture=0)
            bone['cubes'].append(dict(origin=origin, size=size, uv=uv))
            start = [-origin[0]-size[0], *origin[1:]]
            id = uid(f'{name}/{part}/{label}')
            elements.append(dict(name=label, type='cube', uuid=id, **{'from':start,'to':[x+y for x,y in zip(start,size)]},
                                 origin=group['origin'], rotation=[0,0,0], autouv=0, box_uv=False, faces=faces))
            group['children'].append(id)

    shape = dict(description=dict(identifier='geometry.'+name, texture_width=128, texture_height=128,
                                 visible_bounds_width=2, visible_bounds_height=2.6,
                                 visible_bounds_offset=[0,1,0]), bones=bones)
    animations = motions(name == 'bullman')
    editor_animations = []
    for full_name, motion in animations.items():
        animators = {}
        for part, channels in motion['bones'].items():
            frames = [dict(channel=channel, time=float(t), interpolation='linear',
                           uuid=uid(f'{name}/{full_name}/{part}/{channel}/{t}'), color=-1,
                           data_points=[dict(zip('xyz',map(str,vector)))])
                      for channel, times in channels.items() for t,vector in times.items()]
            animators[groups[part]['uuid']] = dict(name=part, type='bone', keyframes=frames)
        editor_animations.append(dict(name=full_name, uuid=uid(name+'/'+full_name), loop='loop' if motion['loop'] else 'once',
                                     snapping=20, length=motion['animation_length'], animators=animators))
    image.save(ASSETS/f'textures/entity/{name}.png')
    write(ASSETS/f'geo/{name}.geo.json', {'format_version':'1.12.0','minecraft:geometry':[shape]})
    write(ASSETS/f'animations/{name}.animation.json', dict(format_version='1.8.0', animations=animations))
    write(HERE/f'{name}.bbmodel', dict(meta=dict(format_version='4.10',model_format='geckolib_model',box_uv=False),
                                       name=name,model_identifier=name,geckolib_model_type='Entity',geckolib_modid='pinkkicore',
                                       visible_box=[2,2.6,1],resolution=dict(width=128,height=128),elements=elements,
                                       outliner=roots,animations=editor_animations,textures=[texture(name,image)]))
    return dict(geometry=shape, animations=animations, texture=uri(image), height=height)


def workstation():
    image = atlas([
        ('#48342e','#6c4a3a','#332825'), ('#855a3a','#b07c4e','#5f402f'),
        ('#5f4034','#855a49','#442b26'), ('#d1a265','#ebce90','#927047'),
        ('#443130','#694940','#2c2323'), ('#7b5f51','#b08060','#593b36'),
        ('#5b5150','#9d8b78','#30272b'),
    ])
    def table_top(draw):
        for xy in ((2,2,29,29),(6,6,25,25),(11,11,20,20)):
            draw.rectangle(xy, outline='#d6ac79', width=1)
        draw.line((1,15,30,15), fill='#674638')
        draw.line((15,1,15,30), fill='#674638')
    def nose(draw):
        draw.rectangle((5,11,10,15),fill='#3b2c29')
        draw.rectangle((22,11,27,15),fill='#3b2c29')
    front(image, 1, table_top)
    front(image, 5, nose)
    render_block = support['block']
    render_block.__globals__.update(HERE=HERE, uid=uid)
    return render_block('niumo_table', [
        ('base',[0,0,0],[16,3,16],{'all':0}),
        ('work frame',[1,3,1],[15,12,15],{'all':2}),
        ('wood top',[0,12,0],[16,16,16],{'all':0,'up':1}),
        ('inlay',[0,10.7,0],[16,11.2,16],{'all':3}),
        ('bull head',[4.5,5.0,.35],[11.5,10.25,1.4],{'all':4}),
        ('bull snout',[5.25,4.4,0],[10.75,6.6,.65],{'all':4,'north':5}),
        ('left horn',[3.2,8.2,0],[4.7,10.6,1.1],{'all':6}),
        ('right horn',[11.3,8.2,0],[12.8,10.6,1.1],{'all':6}),
    ], image, gui_rotation=(27, 214, 0))


def icons():
    def icon():
        result = Image.new('RGBA',(16,16))
        return result, ImageDraw.Draw(result)
    bull, painter = icon()
    painter.polygon([(0,3),(3,4),(4,5),(5,3),(10,3),(12,5),(15,3),(14,8),(12,9),(11,13),
                     (4,13),(3,9),(1,8)], fill='#211b22')
    painter.polygon([(2,5),(4,6),(5,4),(10,4),(11,6),(13,5),(12,8),(10,12),(5,12),(3,8)],fill='#51372f')
    painter.rectangle((4,7,5,7),fill='#eaa46f')
    painter.rectangle((10,7,11,7),fill='#eaa46f')
    painter.rectangle((5,9,10,11),fill='#90694f')
    painter.point([(6,10),(9,10)],fill='#32222a')
    helmet = bull.copy()
    painter = ImageDraw.Draw(helmet)
    painter.line([(3,6),(4,3),(6,2),(9,2),(12,4),(13,6)],fill='#a7a9b0',width=2)
    painter.line([(3,9),(4,14),(6,14)],fill='#6d6c77',width=2)
    painter.line([(12,9),(11,14),(9,14)],fill='#6d6c77',width=2)
    whiskers, painter = icon()
    painter.line([(2,5),(4,7),(7,6),(8,6),(11,7),(13,5)],fill='#704453',width=3)
    painter.line([(2,6),(2,10),(4,12),(6,11),(7,8)],fill='#b9a2a1',width=2)
    painter.line([(13,6),(13,10),(11,12),(9,11),(8,8)],fill='#b9a2a1',width=2)
    painter.point([(3,8),(12,8),(5,11),(10,11)],fill='#f1dbca')
    soul, painter = icon()
    painter.polygon([(2,13),(3,7),(5,5),(5,2),(8,4),(10,1),(12,4),(14,6),(13,10),(11,12),(9,14)],
                     fill='#682f35')
    painter.polygon([(4,12),(5,7),(8,5),(11,4),(13,7),(11,11),(8,13)],fill='#af563e')
    painter.line([(6,8),(8,6),(10,6)],fill='#fbbe7c')
    painter.point([(7,9),(10,9),(3,4),(14,3)],fill='#f2a164')
    for name,image in [('pinkki_nm',bull),('niumo_helmet',helmet),('niumo_whiskers',whiskers),('demon_soul',soul)]:
        image.save(ASSETS/f'textures/item/{name}.png')
        image.save(WIKI/f'{name}.png')
    bull.resize((18,18),Image.Resampling.NEAREST).save(ASSETS/'textures/mob_effect/niumo.png')


def reviewer(payload):
    template = (HERE.parent/'zhang-galgame/review-template.html').read_text()
    replacements = {
        '张家与旮旯':'牛魔', 'Math.max(29,data.height*modelScale*1.68)':'Math.max(38,data.height*1.8)', "modelScale = name === 'big_zhang' ? 2 : 1;":'modelScale = 1;',
        "assets[name === 'big_zhang' ? 'zhang' : name]":'assets[name]',
        "model('zhang')":"model('bullman')", 'animation.zhang.':'animation.niumo.',
        '<option value="walk">行走</option>':'<option value="walk">行走</option><option value="attack">近战</option>',
        '__GALGAME_ASSETS__':json.dumps(payload, ensure_ascii=False,separators=(',',':')),
    }
    for original, replacement in replacements.items():
        assert original in template, original
        template = template.replace(original,replacement)
    options = ''.join(f'<option value="{id}">{name}</option>'
                      for id,name in [('bullman','牛人'),('lao_cai','又菜又爱玩'),('niumo_table','牛魔台')])
    template = re.sub('(<select id="galgame-model">).*?(</select>)',r'\1'+options+r'\2',template)
    (HERE/'review.html').write_text(template)


def main():
    reviewer({'bullman':mob('bullman',bull_art(),bull_parts(),32.4),
              'lao_cai':mob('lao_cai',lao_art(),lao_parts(),23.1),
              'niumo_table':workstation()})
    icons()


if __name__ == '__main__':
    main()

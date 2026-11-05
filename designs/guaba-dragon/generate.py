#!/usr/bin/env python3
"""Author the Guaba dragon review model and matching GeckoLib 4 assets; stdlib only."""
import base64
import hashlib
import json
import math
from pathlib import Path
import random
import struct
import uuid
import zlib

HERE = Path(__file__).resolve().parent
PALETTE = {'red':(177,48,34),'gold':(247,200,59),'orange':(233,112,25),'blue':(30,104,154),'cyan':(55,156,189),'ivory':(255,233,171),'mouth':(69,24,39),'black':(19,25,37)}
BONES = []
ELEMENTS = []
SEED = uuid.UUID('cdf2e8c1-c352-4b36-9fb6-4b23d5ec8cca')


def uid(name):
    return str(uuid.uuid5(SEED, name))


def bone(name, pivot, parent=None, rotation=None):
    item = dict(name=name, pivot=pivot, cubes=[])
    if parent:
        item['parent'] = parent
    if rotation:
        item['rotation'] = rotation
    BONES.append(item)
    return name


def cube(group, name, origin, size, color='skin', rotation=None, pivot=None, eyes=False):
    assert all(x > 0 for x in size), name
    record = dict(name=name, origin=origin, size=size, color=color, eyes=eyes)
    if rotation:
        record.update(rotation=rotation, pivot=pivot or origin)
    next(b for b in BONES if b['name'] == group)['cubes'].append(record)


bone('root', [0,0,0])
bone('body', [0,17,3], 'root')
cube('body','torso',[-6,11,-3],[12,15,15],'red')
cube('body','shoulder',[-5,23,-5],[10,6,12],'red')
cube('body','haunch',[-7,10,3],[14,10,11],'red')
cube('body','belly',[-4.8,12,-3.5],[9.6,13,1.5],'gold')
bone('neck_base',[0,24,-2],'body')
cube('neck_base','rising_neck',[-4.5,25,-7],[9,12,9],'gold',rotation=[-20,0,0],pivot=[0,26,-2])
bone('neck_tip',[0,35,-6],'neck_base')
cube('neck_tip','arched_neck',[-4,34,-15],[8,9,12],'gold')
cube('neck_tip','neck_red_back',[-4.4,35,-5],[8.8,7,3],'red')
for i in range(4):
    cube('neck_base',f'neck_band_{i}',[-4.65,27+i*2,-7.3],[9.3,.5,1],'orange')
bone('head',[0,37,-14],'neck_tip')
cube('head','skull',[-5.5,33,-23],[11,9,11],'red')
cube('head','muzzle',[-5,32,-30],[10,5,10],'red')
cube('head','nose_ridge',[-1.5,36,-29],[3,1.5,8],'cyan')
cube('head','mouth_roof',[-4.3,31.5,-29],[8.6,.6,9],'mouth')
for side,name in [(1,'left'),(-1,'right')]:
    cube('head','eye_rim_'+name,[side*5.5-.45,35,-23],[.9,5,5],'orange')
    cube('head','eye_gold_'+name,[side*6-.18,35.8,-22.4],[.36,3.4,3.6],'gold')
    cube('head','eye_slit_'+name,[side*6.2-.12,36,-21.1],[.24,2.8,.7],'black')
    cube('head','eye_glint_'+name,[side*6.35-.1,38,-21.25],[.2,.65,.7],'ivory')
    cube('head','brow_'+name,[side*5.6-.6,40,-23.6],[1.2,1.1,6],'cyan',rotation=[-10,0,0],pivot=[side*5.6,40,-20])
    cube('head','nostril_'+name,[side*3-.65,36.3,-30.15],[1.3,.8,.35],'black')
    cube('head','horn_base_'+name,[side*4-.9,41,-18],[1.8,5,2],'gold',rotation=[25,0,side*-16],pivot=[side*4,41,-17])
    cube('head','horn_tip_'+name,[side*5-.5,45,-15],[1,3,1.3],'ivory')
    for i in range(4):
        cube('head',f'fang_{name}_{i}',[side*4-.45,29.8,-28+i*2],[.9,2,1],'ivory')
bone('jaw',[0,32,-20],'head')
cube('jaw','lower_jaw',[-4.5,28,-29],[9,2,10],'red')
cube('jaw','jaw_inner',[-3.8,30,-28.8],[7.6,.45,8],'mouth')
cube('jaw','chin',[-3.5,27.3,-27.5],[7,.8,8],'cyan')
for side,name in [(1,'left'),(-1,'right')]:
    for i in range(3): cube('jaw',f'lower_fang_{name}_{i}',[side*3.5-.4,30,-27+i*2.3],[.8,1.2,.8],'ivory')
bone('breath_socket',[0,31,-30],'head')
# Segmented tapering tail, entirely geometric rather than a flat illustration.
parent='body'
for i,(z,y,w,h,l) in enumerate([(12,14,9,8,12),(22,13,7,6,11),(31,13,5,4,10),(39,15,3,3,8)]):
    name='tail_'+str(i+1);bone(name,[0,y,z],parent)
    cube(name,name+'_core',[-w/2,y-h/2,z-1],[w,h,l],'red')
    cube(name,name+'_gold',[-w/2,y+h/2-.7,z],[w,.9,l-1],'gold')
    parent=name
bone('tail_flame',[0,16,46],parent)
for i in range(3): cube('tail_flame','tail_ember_'+str(i),[-2+i*.5,15+i*2,44+i],[4-i,3,6-i],'orange' if i==0 else 'gold')
# Flame-shaped crest is solid ornament, not particles or lighting.
bone('crest',[0,28,4],'body')
for i in range(6):
    z=-1+i*2.8;y=29-i*1.6
    cube('crest','back_flame_'+str(i),[-.8,y,z],[1.6,5+(i%2)*2,2.4],'orange')
    cube('crest','back_flame_tip_'+str(i),[-.45,y+5+(i%2)*2,z+.6],[.9,2,1.3],'gold')
bone('neck_crest',[0,35,-3],'neck_tip')
for i in range(4):
    cube('neck_crest','mane_'+str(i),[-.7,39-i*1.7,-12+i*3],[1.4,5,2.5],'orange')
    cube('neck_crest','mane_tip_'+str(i),[-.4,44-i*1.7,-11.4+i*3],[.8,2,1.3],'gold')
for side,name in [(1,'left'),(-1,'right')]:
    bone('arm_'+name,[side*5,25,-3],'body')
    cube('arm_'+name,'upper_arm_'+name,[side*7-1.5,19,-5],[3,7,4],'red',rotation=[-22,0,side*18],pivot=[side*5,25,-3])
    bone('hand_'+name,[side*8,20,-5],'arm_'+name)
    cube('hand_'+name,'hand_'+name,[side*8-1.5,18,-10],[3,3,6],'blue')
    for j in range(3): cube('hand_'+name,f'claw_{name}_{j}',[side*8-1.2+j*.9,17.5,-12],[.7,1.2,3],'gold')
    bone('leg_'+name,[side*5,15,7],'body')
    cube('leg_'+name,'thigh_'+name,[side*7-2.5,6,3],[5,11,9],'red')
    bone('shin_'+name,[side*7,7,7],'leg_'+name)
    cube('shin_'+name,'shin_'+name,[side*7-1.5,2,2],[3,6,4],'blue')
    bone('foot_'+name,[side*7,2,3],'shin_'+name)
    cube('foot_'+name,'foot_'+name,[side*7-2.5,0,-3],[5,2.5,8],'blue')
    for j in range(3): cube('foot_'+name,f'toe_{name}_{j}',[side*7-2+j*1.6,0,-6],[1.1,1.3,4],'ivory')
ANIMATIONS={}
def anim(name,length,loop=True):
    data=dict(loop=loop,animation_length=length,bones={})
    ANIMATIONS['animation.guaba_dragon.'+name]=data
    return data['bones']
def channel(data,bone_name,kind,frames):
    data.setdefault(bone_name,{})[kind]={str(t):v for t,v in frames}
d=anim('idle',3)
channel(d,'neck_base','rotation',[(0,[0,0,0]),(1.5,[-2,0,0]),(3,[0,0,0])])
channel(d,'body','scale',[(0,[1,1,1]),(1.5,[1.015,1.01,1.015]),(3,[1,1,1])])
for i in range(4): channel(d,'tail_'+str(i+1),'rotation',[(0,[0,-3,0]),(1.5,[0,3,0]),(3,[0,-3,0])])
d=anim('breath_pose',4.8,False)
channel(d,'neck_base','rotation',[(0,[0,0,0]),(.65,[-9,0,0]),(1,[5,0,0]),(4,[5,0,0]),(4.8,[0,0,0])])
channel(d,'neck_tip','rotation',[(0,[0,0,0]),(.65,[-5,0,0]),(1,[5,0,0]),(4,[5,0,0]),(4.8,[0,0,0])])
channel(d,'jaw','rotation',[(0,[0,0,0]),(.65,[-6,0,0]),(1,[-32,0,0]),(4,[-32,0,0]),(4.8,[0,0,0])])
for side,sign in [('left',1),('right',-1)]:
    channel(d,'arm_'+side,'rotation',[(0,[0,0,0]),(.65,[-12,0,sign*8]),(4,[-12,0,sign*8]),(4.8,[0,0,0])])

d=anim('walk',1)
for side,sign in [('left',1),('right',-1)]:
    channel(d,'leg_'+side,'rotation',[(0,[18*sign,0,0]),(.5,[-18*sign,0,0]),(1,[18*sign,0,0])])
    channel(d,'arm_'+side,'rotation',[(0,[-10*sign,0,0]),(.5,[10*sign,0,0]),(1,[-10*sign,0,0])])
channel(d,'tail_1','rotation',[(0,[0,-4,0]),(.5,[0,4,0]),(1,[0,-4,0])])
d=anim('melee',.45,False)
channel(d,'neck_tip','rotation',[(0,[0,0,0]),(.15,[8,0,0]),(.45,[0,0,0])])
channel(d,'jaw','rotation',[(0,[0,0,0]),(.1,[-18,0,0]),(.25,[0,0,0]),(.45,[0,0,0])])

def png_bytes(width, height, pixels):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    raw = b''.join(b'\0' + bytes(pixels[y*width*4:(y+1)*width*4]) for y in range(height))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')


def write_json(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')


def build():
    # Per-face UVs, with one texel per Minecraft model unit; eyes have extra detail.
    atlas = 512
    pixels = bytearray([0] * atlas * atlas * 4)
    x = y = row = 1
    for group in BONES:
        for item in group['cubes']:
            uv = {}
            faces = {}
            sx, sy, sz = item['size']
            for face, width, height in [('north', sx, sy), ('south', sx, sy), ('east', sz, sy), ('west', sz, sy), ('up', sx, sz), ('down', sx, sz)]:
                w, h = max(1, math.ceil(width)), max(1, math.ceil(height))
                if item['eyes'] and face == 'north':
                    w, h = max(10, w), max(10, h)
                if x+w+1 > atlas:
                    x, y, row = 1, y+row+1, 0
                assert y+h+1 < atlas, 'Atlas overflow'
                uv[face] = dict(uv=[x, y], uv_size=[w, h])
                faces[face] = dict(uv=[x, y, x+w, y+h], texture=0)
                if face in ['up', 'down']:
                    faces[face]['uv'] = [x+w, y+h, x, y]
                rng = random.Random(item['name'] + face)
                for py in range(h):
                    for px in range(w):
                        color = PALETTE[item['color']]
                        jitter = rng.choice([0, 0, 0, -3, 2, 1])
                        # Red flesh remains exposed between blue muscle bands. Paint
                        # each real UV face; no flat reference-image projection.
                        if item['name'] in ['skull', 'muzzle', 'lower_jaw']:
                            u, v = (px + .5) / w, (py + .5) / h
                            blue = False
                            if item['name'] == 'skull':
                                if face in ['east', 'west']:
                                    # Brow and sweeping cheek band, with a red temple.
                                    blue = v < .22 or abs(v - (.78 - .35*u)) < .14
                                elif face in ['north', 'south']:
                                    blue = u < .2 or u > .8 or (.34 < u < .66 and v < .55)
                                elif face == 'up':
                                    blue = abs(u-.5) < .16 or abs(v-(.2+.65*abs(2*u-1))) < .12
                            elif item['name'] == 'muzzle':
                                if face in ['north', 'south']:
                                    # Blue outline and central bridge, red nostril pads.
                                    blue = u < .12 or u > .88 or v > .78 or (abs(u-.5) < .12 and v < .5)
                                elif face in ['east', 'west']:
                                    blue = v > .72 or abs(v-(.2+.4*u)) < .13
                                elif face == 'up':
                                    blue = abs(u-.5) < .15 or v > .78
                            else:
                                blue = face == 'down' or (face in ['north','south'] and (u < .23 or u > .77)) or (face in ['east','west'] and .35 < u < .65)
                            color = PALETTE['blue' if blue else 'red']

                        if item['eyes'] and face == 'north':
                            u, v = (px+.5)/w, (py+.5)/h
                            dx, dy = (u-.5)/.42, (v-.5)/.46
                            r = dx*dx+dy*dy
                            if r < 1:
                                color = (128, 143, 69) if r > .73 else (218, 225, 130)
                                if abs(u-.52) < .105 and .23 < v < .76:
                                    color = (48, 58, 37)
                                if .24 < u < .43 and .22 < v < .40:
                                    color = (252, 251, 215)
                            jitter = 0
                        elif item['color'] in ['red', 'gold', 'blue'] and rng.random() < .035:
                            jitter = -9
                        elif item['color'] == 'gold' and py % 4 == 0:
                            jitter = -10
                        rgba = [max(0, min(255, c+jitter)) for c in color] + [255]
                        i = ((y+py)*atlas+x+px)*4
                        pixels[i:i+4] = bytes(rgba)
                x += w+1
                row = max(row, h)
            item['uv'] = uv
            # Match Blockbench's official Bedrock codec coordinate conversion.
            position = [-(item['origin'][0]+sx), item['origin'][1], item['origin'][2]]
            pivot = item.get('pivot', group['pivot'])
            rotation = item.get('rotation', [0, 0, 0])
            element = dict(name=item['name'], type='cube', uuid=uid(item['name']),
                           **{'from': position, 'to': [a+b for a, b in zip(position, item['size'])]},
                           origin=[-pivot[0], pivot[1], pivot[2]], rotation=[-rotation[0], -rotation[1], rotation[2]],
                           autouv=0, box_uv=False, color=0, faces=faces)
            ELEMENTS.append(element)
    texture = png_bytes(atlas, atlas, pixels)
    (HERE/'textures').mkdir(exist_ok=True)
    (HERE/'textures/guaba_dragon.png').write_bytes(texture)
    texture_source = 'data:image/png;base64,' + base64.b64encode(texture).decode()
    geo_bones = []
    for group in BONES:
        data = {k: v for k, v in group.items() if k != 'cubes'}
        if group['cubes']:
            data['cubes'] = [{k: v for k, v in c.items() if k not in ['name', 'color', 'eyes']} for c in group['cubes']]
        geo_bones.append(data)
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': dict(identifier='geometry.guaba_dragon', texture_width=atlas, texture_height=atlas,
                            visible_bounds_width=10, visible_bounds_height=7, visible_bounds_offset=[0, 2, 0]),
        'bones': geo_bones}]}
    animation = {'format_version': '1.8.0', 'animations': ANIMATIONS}
    write_json(HERE/'guaba_dragon.geo.json', geo)
    write_json(HERE/'guaba_dragon.animation.json', animation)

    def outline(group):
        pivot = group['pivot']
        rotation = group.get('rotation', [0, 0, 0])
        return dict(name=group['name'], uuid=uid(group['name']), origin=[-pivot[0], pivot[1], pivot[2]],
                    rotation=[-rotation[0], -rotation[1], rotation[2]], color=0, export=True, isOpen=False,
                    children=[uid(c['name']) for c in group['cubes']] + [outline(b) for b in BONES if b.get('parent') == group['name']])

    bb_animations = []
    for name, data in ANIMATIONS.items():
        animators = {}
        for group_name, channels in data['bones'].items():
            keyframes = []
            for kind, frames in channels.items():
                for t, vector in frames.items():
                    keyframes.append(dict(channel=kind, data_points=[dict(zip(['x', 'y', 'z'], map(str, vector)))],
                                          uuid=uid(name+group_name+kind+t), time=float(t), color=-1, interpolation='linear'))
            animators[uid(group_name)] = dict(name=group_name, type='bone', keyframes=keyframes)
        bb_animations.append(dict(uuid=uid(name), name=name, loop='loop' if data['loop'] else 'once',
                                  override=False, length=data['animation_length'], snapping=20,
                                  anim_time_update='', blend_weight='', start_delay='', loop_delay='', animators=animators))
    bb = dict(meta=dict(format_version='4.10', model_format='geckolib_model', box_uv=False),
              name='guaba_dragon', model_identifier='guaba_dragon', geckolib_model_type='Entity',
              geckolib_modid='pinkkicore', visible_box=[10, 7, 2],
              resolution=dict(width=atlas, height=atlas), elements=ELEMENTS,
              outliner=[outline(BONES[0])], animations=bb_animations,
              textures=[dict(name='guaba_dragon.png', id='0', uuid=uid('texture'), source=texture_source,
                             relative_path='textures/guaba_dragon.png', mode='bitmap', particle=False,
                             width=atlas, height=atlas, uv_width=atlas, uv_height=atlas)])
    write_json(HERE/'guaba_dragon.bbmodel', bb)
    # The reviewer reads these exact geometry/animation values, not a second model.
    payload = dict(geometry=geo['minecraft:geometry'][0], animations=ANIMATIONS,
                   texture=texture_source, names={uid(c['name']): c['name'] for b in BONES for c in b['cubes']})
    write_json(HERE/'review-data.json', payload)
    template = HERE/'review-template.html'
    if template.exists():
        (HERE/'review.html').write_text(template.read_text().replace('__BINGWA_DATA__', json.dumps(payload, ensure_ascii=False, separators=(',', ':'))))
    report = dict(bones=len(BONES), cubes=len(ELEMENTS), animations=list(ANIMATIONS),
                  atlas_size=[atlas, atlas], used_atlas_rows=y+row,
                  texture_sha256=hashlib.sha256(texture).hexdigest())
    write_json(HERE/'model-info.json', report)
    print(json.dumps(report, ensure_ascii=False))


if __name__ == '__main__':
    build()

#!/usr/bin/env python3
"""Author the Bingwa review model and matching GeckoLib 4 assets; stdlib only."""
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
PALETTE = {
    'skin': (231, 229, 191), 'light': (243, 240, 208),
    'belly': (235, 220, 179), 'limb': (222, 219, 181),
    'lip': (210, 196, 157), 'mouth': (91, 65, 51),
    'tongue': (219, 126, 106), 'orange': (235, 151, 38),
    'orange_top': (247, 178, 49), 'leaf': (103, 137, 65),
    'stem': (119, 98, 55), 'spot': (177, 184, 123),
}
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


bone('root', [0, 0, 0])
bone('body', [0, 6, 1], 'root')
cube('body', 'belly_lower', [-6.8, 2.8, -4.8], [13.6, 2.4, 11.6], 'belly')
cube('body', 'body_lower_bevel', [-8.0, 5, -6.2], [16, 1.6, 13.8])
cube('body', 'body_wide', [-9, 6.6, -6.4], [18, 3.2, 14.0])
cube('body', 'back_round', [-7.8, 9.8, -4.8], [15.6, 1.5, 11.2], 'light')
cube('body', 'back_top', [-5.8, 11.3, -2.6], [11.6, 0.8, 7.6], 'light')
bone('head', [0, 9, -3.5], 'body')
cube('head', 'face', [-8.3, 7.8, -8.2], [16.6, 3.6, 8.6], 'light')
cube('head', 'face_upper_bevel', [-6.8, 11.4, -6.8], [13.6, 1, 6.6], 'light')
cube('head', 'muzzle', [-7.2, 6.9, -8.85], [14.4, 2.1, 4.4], 'light')
cube('head', 'mouth_gap', [-6.5, 6.1, -8.96], [13, 0.9, 0.2], 'mouth')
cube('head', 'upper_lip', [-6.8, 6.95, -9.02], [13.6, 0.32, 0.4], 'lip')
for side in [-1, 1]:
    x = side * 5.8
    name = 'left' if side > 0 else 'right'
    bone('eye_' + name, [x, 11.2, -6.2], 'head')
    cube('eye_' + name, 'eye_socket_' + name, [x - 2.1, 10.25, -7.55], [4.2, 3.6, 3.5], 'light', eyes=True)
    cube('eye_' + name, 'eyelid_top_' + name, [x - 1.8, 13.85, -7.1], [3.6, 0.3, 2.7])
    cube('head', 'nostril_' + name, [side * 2.8 - 0.22, 8.55, -8.91], [0.44, 0.32, 0.13], 'spot')
    for j in range(3):
        cube('head', f'cheek_{name}_{j}', [side * (6.8 + j * 0.37) - 0.24, 8.65 + (j % 2) * 0.55, -8.26], [0.42, 0.35, 0.14], 'spot')
bone('jaw', [0, 6.8, -4.3], 'head')
cube('jaw', 'chin_lower', [-5.8, 3.8, -7.6], [11.6, 1.4, 5.6], 'belly')
cube('jaw', 'chin', [-7, 5.2, -8.5], [14, 1.0, 6.5], 'belly')
cube('jaw', 'lower_lip', [-6.7, 6.2, -8.98], [13.4, 0.3, 0.48], 'lip')
bone('throat', [0, 4.9, -6.1], 'body')
cube('throat', 'throat_sac', [-4.3, 3.1, -7.8], [8.6, 1.4, 3.5], 'belly')
bone('tongue', [0, 6.55, -8.4], 'head')
cube('tongue', 'tongue_strip', [-1.0, 6.43, -8.65], [2, 0.24, 0.25], 'tongue')

for side, name in [(1, 'left'), (-1, 'right')]:
    # Splayed front legs and separate wrists make the waddling gait readable.
    bone('arm_' + name, [side * 7.5, 6.7, -3.8], 'body')
    cube('arm_' + name, 'upper_arm_' + name,
         [side * 8.5 - 1.4, 3.5, -5.2], [2.8, 3.4, 3.2], 'limb',
         rotation=[0, 0, side * -18], pivot=[side * 7.8, 6.5, -4.0])
    cube('arm_' + name, 'forearm_' + name,
         [side * 7.8 - 1.05, 1.5, -6.7], [2.1, 3.1, 2.6], 'light',
         rotation=[-18, 0, side * 13], pivot=[side * 8.0, 4.2, -5.2])
    bone('hand_' + name, [side * 7.4, 1.5, -7.1], 'arm_' + name)
    cube('hand_' + name, 'palm_' + name, [side * 7.2 - 1.8, 0.55, -8.5], [3.6, 1.05, 3.3], 'light')
    for j in range(3):
        cube('hand_' + name, f'finger_{name}_{j}',
             [side * 7.2 + (j - 1) * 1.25 - 0.45, 0.35, -9.8 - (0.4 if j == 1 else 0)],
             [0.9, 0.7, 2.2], 'light')
    bone('leg_' + name, [side * 7, 5.8, 4.1], 'body')
    cube('leg_' + name, 'thigh_' + name, [side * 9.3 - 2.5, 2.4, 1.8], [5, 4.6, 6], 'limb')
    cube('leg_' + name, 'thigh_bevel_' + name, [side * 9.3 - 2.0, 6.9, 2.4], [4, 0.7, 4.8])
    bone('shin_' + name, [side * 10.4, 2.9, 5.5], 'leg_' + name)
    cube('shin_' + name, 'folded_shin_' + name, [side * 10.7 - 1.2, 1.2, 3.6], [2.4, 2.0, 4.6], 'light')
    bone('foot_' + name, [side * 11, 1, 5.4], 'shin_' + name)
    cube('foot_' + name, 'rear_palm_' + name, [side * 11.0 - 2.2, 0.4, 3.3], [4.4, 0.85, 3.6], 'light')
    for j in range(3):
        cube('foot_' + name, f'toe_{name}_{j}', [side * 11 + (j - 1) * 1.45 - 0.5, 0.2, 6.1], [1, 0.65, 2.0 + (0.7 if j == 1 else 0)], 'light')

# The reference's little frog and orange are attached ornaments, not separate mobs.
bone('baby', [0, 12.0, 0.1], 'head')
cube('baby', 'baby_belly', [-3.7, 12.0, -2.5], [7.4, 1.0, 6.1], 'belly')
cube('baby', 'baby_body', [-4.6, 13.0, -3.0], [9.2, 2.5, 6.8], 'light')
cube('baby', 'baby_round_top', [-3.7, 15.5, -2.4], [7.4, 0.8, 5.6], 'light')
cube('baby', 'baby_muzzle', [-3.9, 13.4, -3.8], [7.8, 1.6, 2.9], 'light')
cube('baby', 'baby_mouth', [-2.7, 13.32, -3.84], [5.4, 0.17, 0.12], 'lip')
for side, name in [(1, 'left'), (-1, 'right')]:
    cube('baby', 'baby_eye_' + name, [side * 2.65 - 1.18, 15.1, -3.0], [2.36, 2.25, 2.3], 'light', eyes=True)
    cube('baby', 'baby_haunch_' + name, [side * 4.2 - 1.25, 12.5, 1.4], [2.5, 1.65, 2.9])
    cube('baby', 'baby_arm_' + name, [side * 3.7 - 0.6, 11.8, -3.7], [1.2, 1.7, 2.0], 'light')
    for j in range(3):
        cube('baby', f'baby_finger_{name}_{j}', [side * 3.7 + (j - 1) * 0.5 - 0.2, 11.65, -4.3], [0.4, 0.45, 0.9], 'light')
bone('orange', [0, 16.4, 0.9], 'baby')
cube('orange', 'orange_bottom', [-1.65, 16.3, -0.7], [3.3, 0.6, 3.3], 'orange')
cube('orange', 'orange_middle', [-2.35, 16.9, -1.4], [4.7, 2.8, 4.7], 'orange')
cube('orange', 'orange_top', [-1.75, 19.7, -0.8], [3.5, 0.65, 3.5], 'orange_top')
cube('orange', 'stem', [-0.25, 20.35, 0.6], [0.5, 0.6, 0.5], 'stem')
cube('orange', 'leaf', [0.1, 20.8, 0.1], [2.8, 0.22, 1.4], 'leaf', rotation=[0, -25, -18], pivot=[0.1, 20.8, 0.8])

ANIMATIONS = {}


def anim(name, length, loop=True):
    data = dict(loop=loop, animation_length=length, bones={})
    ANIMATIONS['animation.bingwa.' + name] = data
    return data['bones']


def channel(data, bone_name, kind, frames):
    data.setdefault(bone_name, {})[kind] = {str(t): vec for t, vec in frames}


d = anim('idle', 2.4)
channel(d, 'body', 'position', [(0, [0, 0, 0]), (1.2, [0, 0.22, 0]), (2.4, [0, 0, 0])])
channel(d, 'throat', 'scale', [(0, [1, 1, 1]), (1.2, [1.025, 1.08, 1.035]), (2.4, [1, 1, 1])])
channel(d, 'baby', 'rotation', [(0, [0, 0, -1]), (1.2, [0, 0, 1]), (2.4, [0, 0, -1])])
d = anim('walk', 1.2)
channel(d, 'body', 'rotation', [(0, [0, 0, -2]), (.3, [0, -2, 0]), (.6, [0, 0, 2]), (.9, [0, 2, 0]), (1.2, [0, 0, -2])])
channel(d, 'body', 'position', [(0, [0, 0, 0]), (.3, [0, .35, 0]), (.6, [0, 0, 0]), (.9, [0, .35, 0]), (1.2, [0, 0, 0])])
for side, phase in [('left', 1), ('right', -1)]:
    for limb, amount in [('arm_', 13), ('leg_', -15)]:
        channel(d, limb + side, 'rotation', [(0, [amount*phase, 0, 0]), (.6, [-amount*phase, 0, 0]), (1.2, [amount*phase, 0, 0])])
    channel(d, 'hand_' + side, 'rotation', [(0, [-8*phase, 0, 0]), (.6, [8*phase, 0, 0]), (1.2, [-8*phase, 0, 0])])
channel(d, 'orange', 'rotation', [(0, [0, 0, -3]), (.6, [0, 0, 3]), (1.2, [0, 0, -3])])
d = anim('jump', 1.2, False)
channel(d, 'body', 'position', [(0, [0, 0, 0]), (.2, [0, -.3, 0]), (.34, [0, 0, 0]), (.8, [0, 0, 0]), (.96, [0, -.2, 0]), (1.2, [0, 0, 0])])
channel(d, 'body', 'rotation', [(0, [0, 0, 0]), (.2, [3, 0, 0]), (.4, [-12, 0, 0]), (.72, [6, 0, 0]), (1.0, [0, 0, 0])])
for side in ['left', 'right']:
    channel(d, 'leg_' + side, 'rotation', [(0, [0, 0, 0]), (.2, [-18, 0, 0]), (.4, [32, 0, 0]), (.75, [22, 0, 0]), (1, [0, 0, 0])])
    channel(d, 'shin_' + side, 'rotation', [(0, [0, 0, 0]), (.24, [0, 0, 0]), (.42, [30, 0, 0]), (.75, [20, 0, 0]), (1, [0, 0, 0])])
    channel(d, 'arm_' + side, 'rotation', [(0, [0, 0, 0]), (.25, [8, 0, 0]), (.45, [-28, 0, 0]), (.82, [10, 0, 0]), (1.1, [0, 0, 0])])
channel(d, 'orange', 'rotation', [(0, [0, 0, 0]), (.35, [-8, 0, 0]), (.65, [6, 0, 0]), (1.2, [0, 0, 0])])
d = anim('swim', 1.1)
channel(d, 'body', 'rotation', [(0, [-5, 0, -1]), (.55, [-5, 0, 1]), (1.1, [-5, 0, -1])])
for side, sign in [('left', 1), ('right', -1)]:
    channel(d, 'leg_' + side, 'rotation', [(0, [-10, -12*sign, 0]), (.55, [30, 16*sign, 0]), (1.1, [-10, -12*sign, 0])])
    channel(d, 'arm_' + side, 'rotation', [(0, [-15, 0, -10*sign]), (.55, [12, 0, 15*sign]), (1.1, [-15, 0, -10*sign])])
d = anim('float', 2.6)
channel(d, 'body', 'position', [(0, [0, 0, 0]), (1.3, [0, .3, 0]), (2.6, [0, 0, 0])])
for side, sign in [('left', 1), ('right', -1)]:
    channel(d, 'leg_' + side, 'rotation', [(0, [12, 7*sign, 0]), (1.3, [9, 10*sign, 0]), (2.6, [12, 7*sign, 0])])
    channel(d, 'arm_' + side, 'rotation', [(0, [-8, 0, -7*sign]), (1.3, [-5, 0, -9*sign]), (2.6, [-8, 0, -7*sign])])
d = anim('croak', 1.4, False)
channel(d, 'throat', 'scale', [(0, [1, 1, 1]), (.35, [1.2, 1.6, 1.7]), (.85, [1.2, 1.6, 1.7]), (1.4, [1, 1, 1])])
channel(d, 'head', 'rotation', [(0, [0, 0, 0]), (.35, [-4, 0, 0]), (1, [-4, 0, 0]), (1.4, [0, 0, 0])])
d = anim('tongue', .8, False)
channel(d, 'jaw', 'rotation', [(0, [0, 0, 0]), (.14, [-20, 0, 0]), (.52, [-20, 0, 0]), (.8, [0, 0, 0])])
channel(d, 'tongue', 'scale', [(0, [1, 1, 1]), (.14, [1, 1, 1]), (.28, [1, 1, 40]), (.4, [1, 1, 40]), (.58, [1, 1, 1]), (.8, [1, 1, 1])])


def png_bytes(width, height, pixels):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    raw = b''.join(b'\0' + bytes(pixels[y*width*4:(y+1)*width*4]) for y in range(height))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')


def write_json(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')


def build():
    # Per-face UVs, with one texel per Minecraft model unit; eyes have extra detail.
    atlas = 256
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
                        elif item['color'] in ['skin', 'light', 'limb'] and rng.random() < .035:
                            jitter = -9
                        elif item['color'].startswith('orange') and rng.random() < .13:
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
    (HERE/'textures/bingwa.png').write_bytes(texture)
    texture_source = 'data:image/png;base64,' + base64.b64encode(texture).decode()
    geo_bones = []
    for group in BONES:
        data = {k: v for k, v in group.items() if k != 'cubes'}
        if group['cubes']:
            data['cubes'] = [{k: v for k, v in c.items() if k not in ['name', 'color', 'eyes']} for c in group['cubes']]
        geo_bones.append(data)
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': dict(identifier='geometry.bingwa', texture_width=atlas, texture_height=atlas,
                            visible_bounds_width=4, visible_bounds_height=3.5, visible_bounds_offset=[0, .85, 0]),
        'bones': geo_bones}]}
    animation = {'format_version': '1.8.0', 'animations': ANIMATIONS}
    write_json(HERE/'bingwa.geo.json', geo)
    write_json(HERE/'bingwa.animation.json', animation)

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
              name='bingwa', model_identifier='bingwa', geckolib_model_type='Entity',
              geckolib_modid='pinkkicore', visible_box=[4, 3.5, .85],
              resolution=dict(width=atlas, height=atlas), elements=ELEMENTS,
              outliner=[outline(BONES[0])], animations=bb_animations,
              textures=[dict(name='bingwa.png', id='0', uuid=uid('texture'), source=texture_source,
                             relative_path='textures/bingwa.png', mode='bitmap', particle=False,
                             width=atlas, height=atlas, uv_width=atlas, uv_height=atlas)])
    write_json(HERE/'bingwa.bbmodel', bb)
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

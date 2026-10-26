#!/usr/bin/env python3
"""Deterministic Java block + editable Blockbench + software 3D review. Pillow required."""
import base64, io, json, math, random, uuid
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]; HERE=Path(__file__).resolve().parent
RES=ROOT/'versions/1.20.1/src/main/resources'; NAME='guaba_charging_station'
rng=random.Random(2610)
colors=['292237','49344f','b2794c','d99866','955538','d99164','42a4c9','b5f4ff','d6b69b','e4bb51']
atlas=Image.new('RGBA',(64,64))
for n,c in enumerate(colors):
 rgb=bytes.fromhex(c); x0=(n%4)*16; y0=(n//4)*16
 for y in range(16):
  for x in range(16):
   delta=rng.choice([-5,0,0,4]);atlas.putpixel((x0+x,y0+y),tuple(max(0,min(255,v+delta)) for v in rgb)+(255,))
 # Edge pattern makes the carved stone and conductor strips legible on all faces.
 draw=ImageDraw.Draw(atlas);draw.line((x0,y0,x0+15,y0),fill=tuple(min(255,v+15) for v in rgb)+(255,))
atlas.save(HERE/f'{NAME}.png');atlas.save(RES/f'assets/pinkkicore/textures/block/{NAME}.png')
buf=io.BytesIO();atlas.save(buf,format='PNG');uri='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()
elements=[]
def cube(name,a,b,material):
 n=material;u=(n%4)*4;v=(n//4)*4
 elements.append({'name':name,'from':a,'to':b,'faces':{f:{'uv':[u,v,u+4,v+4],'texture':'#all'} for f in ['north','south','east','west','up','down']}})
cube('黑曜石台足',[0,0,0],[16,2,16],0)
cube('瓜巴色下沿',[.5,2,.5],[15.5,2.8,15.5],2)
cube('附魔石台身',[2,2.8,2],[14,7.8,14],1)
cube('上层石盘',[0,7.8,0],[16,9.4,16],0)
cube('台面嵌金',[.5,9.4,.5],[15.5,9.8,15.5],2)
cube('充能槽',[3.5,9.8,3.5],[12.5,10.2,12.5],6)
cube('核心晶体',[5,10.2,5],[11,11.2,11],7)
for i,x in enumerate([2.5,6.5,10.5]):
 cube('雷霆瓜巴底座'+str(i),[x, .4,0],[x+3,1.8,.25],6)
 cube('瓜巴晶核'+str(i),[x+.5,.7,0],[x+2.5,1.5,.3],9)
for i,(x,z) in enumerate([(1,7),(13,7),(7,13)]):
 cube('避雷针座'+str(i),[x-.4,9.8,z-.4],[x+2.4,10.4,z+2.4],4)
 cube('铜导柱'+str(i),[x+.5,10.4,z+.5],[x+1.5,12.2,z+1.5],5)
 cube('导柱针头'+str(i),[x,12.2,z],[x+2,12.8,z+2],3)
# Raised zigzag lightning runes, not a billboard or full lightning entity.
for i,(x,y,w) in enumerate([(6.5,6.8,3),(6,5.9,2),(7,5.1,2),(6,4.3,2)]):
 cube('正面电纹'+str(i),[x,y,1.8],[x+w,y+.65,2.04],7 if i%2 else 6)
for i,(x,z,w,d) in enumerate([(2.2,5,2.2,.5),(2.2,6.5,1.2,.5),(11.6,5,2.2,.5),(12.6,6.5,1.2,.5)]):
 cube('台面导线'+str(i),[x,9.8,z],[x+w,10.05,z+d],7)
model={'credit':'PinkkiCore','textures':{'all':'pinkkicore:block/'+NAME,'particle':'pinkkicore:block/'+NAME},'elements':elements,
 'display':{'gui':{'rotation':[30,225,0],'translation':[0,1,0],'scale':[.625]*3},'ground':{'translation':[0,1,0],'scale':[.25]*3},'fixed':{'scale':[.5]*3}}}
(HERE/f'{NAME}.json').write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n')
(RES/f'assets/pinkkicore/models/block/{NAME}.json').write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n')
uid=lambda s:str(uuid.uuid5(uuid.NAMESPACE_DNS,NAME+s))
bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'瓜巴充电桩','model_identifier':NAME,'resolution':{'width':64,'height':64},'elements':[], 'outliner':[], 'textures':[{'name':NAME+'.png','id':'0','uuid':uid('texture'),'source':uri,'mode':'bitmap','saved':False,'width':64,'height':64,'uv_width':64,'uv_height':64}],'display':model['display']}
for e in elements:
 bb['elements'].append(dict(name=e['name'],uuid=uid(e['name']),type='cube',**{'from':e['from'],'to':e['to']},origin=[8,0,8],rotation=[0,0,0],autouv=0,box_uv=False,faces={f:{'uv':[v*4 for v in x['uv']],'texture':0} for f,x in e['faces'].items()}));bb['outliner'].append(uid(e['name']))
(HERE/f'{NAME}.bbmodel').write_text(json.dumps(bb,ensure_ascii=False,indent=2)+'\n')
payload=json.dumps({'model':model,'texture':uri},ensure_ascii=False,separators=(',',':'))
t=(HERE/'review-template.html').read_text().replace('/*ASSET*/null',payload)
(HERE/'review.html').write_text('<!doctype html><meta charset="utf-8"><title>瓜巴充电桩 · 3D 审阅</title><style>body{font-family:system-ui;background:#ecf0f5;color:#243041;max-width:960px;margin:30px auto}button{padding:10px;margin:4px}</style>'+t)
# Isometric Wiki icon derived from the same textured faces, with depth ordering.
def project(p):
 x,y,z=p;return (128+(x-z)*6,207-(y*8)+(x+z-16)*3)
polys=[]
for e in elements:
 x,y,z=e['from'];X,Y,Z=e['to']
 for f,vs in {'north':[(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)],'west':[(x,y,Z),(x,y,z),(x,Y,z),(x,Y,Z)],'up':[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)]}.items():
  polys.append((sum(p[0]+p[2] for p in vs)/4+sum(p[1] for p in vs)/100,vs,e['faces'][f]['uv'],f))
im=Image.new('RGBA',(256,256));d=ImageDraw.Draw(im)
for _,vs,uv,f in sorted(polys,key=lambda t:-t[0]):
 color=atlas.getpixel((int(uv[0]*4+8),int(uv[1]*4+8)));factor=1 if f=='up' else .78 if f=='north' else .9
 d.polygon([project(p) for p in vs],fill=tuple(int(v*factor) for v in color[:3])+(255,))
im.save(ROOT/'wiki/public/images/guaba_charging_station.png');im.save(HERE/'icon.png')
print('Generated',len(elements),'cubes; bounds 16 x 12.8 x 16; Java, Blockbench, embedded 3D review.')

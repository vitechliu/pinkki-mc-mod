#!/usr/bin/env python3
"""Generate spawn-egg colors and vanilla ingredient icons from local 1.20.1 assets (Pillow)."""
from pathlib import Path
from zipfile import ZipFile
import io,json,math
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
z=ZipFile(R/'.local/gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar')
def png(path):return Image.open(io.BytesIO(z.read('assets/minecraft/'+path+'.png'))).convert('RGBA')
def egg(base,overlay):
 im=Image.new('RGBA',(16,16))
 for name,color in [('spawn_egg',base),('spawn_egg_overlay',overlay)]:
  a=png('textures/item/'+name)
  for y in range(16):
   for x in range(16):
    r,g,b,t=a.getpixel((x,y));a.putpixel((x,y),(r*color[0]//255,g*color[1]//255,b*color[2]//255,t))
  im.alpha_composite(a)
 return im
charged=egg((80,180,221),(192,245,255));charged.save(HERE/'charged_creeper_spawn_egg.png');charged.save(R/'versions/1.20.1/src/main/resources/assets/pinkkicore/textures/item/charged_creeper_spawn_egg.png');charged.save(R/'wiki/public/images/charged_creeper_spawn_egg.png')
egg((13,160,0),(0,0,0)).save(R/'wiki/public/images/minecraft/creeper_spawn_egg.png')
# Actual vanilla block model faces, mapped into an isometric SVG with embedded texture crops.
import base64
for name in ['lightning_rod','enchanting_table']:
 m=json.loads(z.read(f'assets/minecraft/models/block/{name}.json'));faces=[]
 def project(p):
  x,y,z=p;return [128+(x-z)*6,200-y*8+(x+z-16)*3]
 for e in m['elements']:
  x,y,z0=e['from'];X,Y,Z=e['to']
  for f,vs in {'north':[(x,Y,z0),(X,Y,z0),(X,y,z0),(x,y,z0)],'west':[(x,Y,Z),(x,Y,z0),(x,y,z0),(x,y,Z)],'up':[(x,Y,Z),(X,Y,Z),(X,Y,z0),(x,Y,z0)]}.items():
   if f not in e['faces']:continue
   v=e['faces'][f];texture=m['textures'][v['texture'][1:]];uv=v['uv'];im=png('textures/'+texture)
   crop=im.crop((min(uv[0],uv[2]),min(uv[1],uv[3]),max(uv[0],uv[2]),max(uv[1],uv[3])))
   if uv[2]<uv[0]:crop=crop.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
   if uv[3]<uv[1]:crop=crop.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
   buf=io.BytesIO();crop.save(buf,format='PNG');uri='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode();a,b,c,d=map(project,vs);w,h=crop.size
   matrix=[(b[0]-a[0])/w,(b[1]-a[1])/w,(d[0]-a[0])/h,(d[1]-a[1])/h,a[0],a[1]]
   faces.append((sum(p[0]+p[2] for p in vs)/4, f'<image width="{w}" height="{h}" transform="matrix({" ".join(map(str,matrix))})" href="{uri}" image-rendering="pixelated"/>'))
 svg='<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">'+''.join(f for _,f in sorted(faces,key=lambda v:-v[0]))+'</svg>'
 (R/f'wiki/public/images/minecraft/{name}.svg').write_text(svg)
print('Rendered actual vanilla ingredient model/egg icons and custom charged egg')

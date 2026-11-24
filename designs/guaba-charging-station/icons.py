#!/usr/bin/env python3
"""Generate spawn-egg colors and vanilla ingredient icons from local 1.20.1 assets (Pillow)."""
from functools import lru_cache
from pathlib import Path
from zipfile import ZipFile
import io,json,sys
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
from render_block_icon import render_icon

z=ZipFile(R/'.local/gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar')
@lru_cache
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
for name in ['lightning_rod','enchanting_table']:
 model=json.loads(z.read(f'assets/minecraft/models/block/{name}.json'))
 render_icon(model, lambda name: png('textures/'+name)).save(R/f'wiki/public/images/minecraft/{name}.png')
print('Rendered actual vanilla ingredient model/egg icons and custom charged egg')

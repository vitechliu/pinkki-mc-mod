#!/usr/bin/env python3
import base64,json,math,struct
from pathlib import Path
H=Path(__file__).resolve().parent;R=H.parents[1];N='guaba_charging_station'
m=json.loads((H/f'{N}.json').read_text());b=json.loads((H/f'{N}.bbmodel').read_text());png=(H/f'{N}.png').read_bytes()
assert struct.unpack('>II',png[16:24])==(64,64)
assert base64.b64decode(b['textures'][0]['source'].split(',',1)[1])==png
assert len(m['elements'])==len(b['elements']) and len(set(b['outliner']))==len(b['elements'])
for a,e in zip(m['elements'],b['elements']):
 assert a['from']==e['from'] and a['to']==e['to']
 for i in range(3):assert math.isfinite(a['from'][i]) and 0<=a['from'][i]<a['to'][i]<=([16,12.8,16][i])
 for f,v in a['faces'].items():
  assert all(0<=x<=16 for x in v['uv']);assert e['faces'][f]['uv']==[x*4 for x in v['uv']]
s=(H/'review.html').read_text();asset=json.JSONDecoder().raw_decode(s.split('const data=',1)[1])[0];assert asset['model']==m
assert base64.b64decode(asset['texture'].split(',',1)[1])==png
res=R/'versions/1.20.1/src/main/resources/assets/pinkkicore'
assert (res/f'textures/block/{N}.png').read_bytes()==png
assert json.loads((res/f'models/block/{N}.json').read_text())==m
print('PASS 30 cubes, 16 x 12.8 x 16, all UVs, embedded texture, Blockbench/export/review/runtime equality')

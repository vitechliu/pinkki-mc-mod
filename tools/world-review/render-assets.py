#!/usr/bin/env python3
"""Resolve vanilla block models into compact WebGL face templates and a local texture atlas."""
import argparse,collections,hashlib,json,math,pathlib,zipfile,io
import numpy as np
from PIL import Image,ImageDraw

class Resources:
    def __init__(self,paths):self.zips=[zipfile.ZipFile(p) for p in paths];self.cache={};self.statecache={}
    def read(self,path):
        for z in self.zips:
            try:return z.read(path)
            except KeyError:pass
        raise KeyError(path)
    def model(self,name):
        name=name.removeprefix('minecraft:')
        if name in self.cache:return self.cache[name]
        try:d=json.loads(self.read('assets/minecraft/models/'+name+'.json'))
        except KeyError:return {'textures':{'all':'minecraft:block/stone'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':{f:{'texture':'#all','cullface':f} for f in ('up','down','east','west','north','south')}}]}
        parent=self.model(d['parent']) if d.get('parent') and not d['parent'].startswith('builtin/') else {}
        result={**parent,**d,'textures':{**parent.get('textures',{}),**d.get('textures',{})}};self.cache[name]=result;return result
    def texture(self,m,t):
        seen=set()
        while t.startswith('#') and t not in seen:seen.add(t);t=m.get('textures',{}).get(t[1:],'minecraft:block/stone')
        return t.removeprefix('minecraft:')

def statekey(s):return s['Name']+'['+','.join(k+'='+v for k,v in sorted(s.get('Properties',{}).items()))+']'
def matches(cond,props):
    if 'OR' in cond:return any(matches(x,props) for x in cond['OR'])
    if 'AND' in cond:return all(matches(x,props) for x in cond['AND'])
    return all(props.get(k) in str(v).split('|') for k,v in cond.items())
def models_for(r,s):
    key=statekey(s)
    if key in r.statecache:return r.statecache[key]
    name=s['Name'].split(':')[-1];p=s.get('Properties',{})
    try:d=json.loads(r.read('assets/minecraft/blockstates/'+name+'.json'))
    except KeyError:return [{'model':'block/'+name}]
    out=[]
    for key,value in d.get('variants',{}).items():
        cond=dict(x.split('=',1) for x in key.split(',') if '=' in x)
        if matches(cond,p):out.append(value[0] if isinstance(value,list) else value);break
    for part in d.get('multipart',[]):
        if matches(part.get('when',{}),p):v=part['apply'];out.append(v[0] if isinstance(v,list) else v)
    r.statecache[key]=out
    return out

def rotate(vertices,origin,axis,angle,rescale=False):
    a=math.radians(angle);c=math.cos(a);s=math.sin(a);m=np.eye(3);i={'x':0,'y':1,'z':2}[axis];j=(i+1)%3;k=(i+2)%3;m[j,j]=m[k,k]=c;m[j,k]=-s;m[k,j]=s
    v=(vertices-origin)@m.T
    if rescale and abs(c)>.01:v[:,[j,k]]/=c
    return v+origin

def geometry(r,s):
    faces=[]
    for spec in models_for(r,s):
        model=r.model(spec['model'])
        if not model.get('elements'):
            name=s['Name'].split(':')[-1]
            tex=model.get('textures',{}).get('particle','minecraft:block/oak_planks')
            if 'chest' in name or 'shulker_box' in name:bounds=([1,0,1],[15,14,15])
            elif 'sign' in name:bounds=([1,5,7],[15,14,9])
            elif 'bed' in name:bounds=([0,0,0],[16,9,16])
            elif 'banner' in name:bounds=([1,0,7],[15,16,9])
            else:bounds=([2,0,2],[14,14,14])
            model={**model,'textures':{**model.get('textures',{}),'review_fallback':tex},'elements':[{'from':bounds[0],'to':bounds[1],'faces':{f:{'texture':'#review_fallback'} for f in ('up','down','east','west','north','south')}}]}
        for el in model.get('elements',[]):
            x,y,z=np.array(el['from'])/16;X,Y,Z=np.array(el['to'])/16
            vertices={'up':[[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],'down':[[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]],'north':[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],'south':[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],'east':[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],'west':[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]]}
            defaultuv={'up':[x*16,z*16,X*16,Z*16],'down':[x*16,z*16,X*16,Z*16],'north':[(1-X)*16,(1-Y)*16,(1-x)*16,(1-y)*16],'south':[x*16,(1-Y)*16,X*16,(1-y)*16],'east':[(1-Z)*16,(1-Y)*16,(1-z)*16,(1-y)*16],'west':[z*16,(1-Y)*16,Z*16,(1-y)*16]}
            for side,f in el.get('faces',{}).items():
                v=np.array(vertices[side],dtype=float)
                if 'rotation' in el:
                    rr=el['rotation'];v=rotate(v,np.array(rr['origin'])/16,rr['axis'],rr['angle'],rr.get('rescale',False))
                if spec.get('x'):v=rotate(v,np.array([.5,.5,.5]),'x',-spec['x'])
                if spec.get('y'):v=rotate(v,np.array([.5,.5,.5]),'y',-spec['y'])
                u0,v0,u1,v1=f.get('uv',defaultuv[side]);uv=[[u0/16,v1/16],[u1/16,v1/16],[u1/16,v0/16],[u0/16,v0/16]]
                steps=f.get('rotation',0)//90
                if steps:uv=uv[-steps:]+uv[:-steps]
                # Face direction derived from transformed geometry for neighbor culling.
                normal=np.cross(v[1]-v[0],v[2]-v[0]);l=np.linalg.norm(normal)
                if l:normal/=l
                cull=normal.round().astype(int).tolist() if f.get('cullface') and 'rotation' not in el else None
                faces.append({'v':v.round(5).tolist(),'uv':uv,'texture':r.texture(model,f['texture']),'cull':cull,'normal':normal.round(4).tolist(),'tint':f.get('tintindex',-1),'shade':el.get('shade',True)})
    return faces

def main():
    ap=argparse.ArgumentParser();ap.add_argument('site',type=pathlib.Path);ap.add_argument('jars',nargs='+',type=pathlib.Path);a=ap.parse_args();r=Resources(a.jars)
    cat=json.loads((a.site/'catalog.json').read_text());states={}
    for c in cat['items']:
        for s in json.loads((a.site/c['data']).read_text())['palette']:states[statekey(s)]=s
    geoms={k:geometry(r,s) for k,s in states.items()};names=sorted({f['texture'] for g in geoms.values() for f in g});n=math.ceil(math.sqrt(len(names)));tile=20;atlas=Image.new('RGBA',(n*tile,n*tile));colors={};texture_index={}
    for i,name in enumerate(names):
        try:im=Image.open(io.BytesIO(r.read('assets/minecraft/textures/'+name+'.png'))).convert('RGBA');im=im.crop((0,0,im.width,im.width)).resize((16,16),Image.Resampling.NEAREST)
        except KeyError:im=Image.new('RGBA',(16,16),(180,90,150,255))
        x=(i%n)*tile+2;y=(i//n)*tile+2;atlas.paste(im,(x,y));texture_index[name]=[x,y,16,16];pix=np.asarray(im);valid=pix[:,:,3]>100;colors[name]=np.mean(pix[:,:,:3][valid],axis=0).tolist() if valid.any() else [140,140,140]
        # Extruded edge pixels avoid atlas bleeding when zooming.
        atlas.paste(im.resize((20,20),Image.Resampling.NEAREST),(x-2,y-2));atlas.paste(im,(x,y))
    atlas.save(a.site/'atlas.png')
    for k,fs in geoms.items():
        for f in fs:
            tx,ty,tw,th=texture_index[f.pop('texture')];f['uv']=[[(tx+u*tw)/(n*tile),1-(ty+v*th)/(n*tile)] for u,v in f['uv']]
    (a.site/'models.json').write_text(json.dumps({'models':geoms,'atlasSize':n*tile,'resourceVersion':'1.20.1 with 1.21.1 fallback'},separators=(',',':')))
    # Compact top-down previews use actual block texture average colors; 3D remains full geometry.
    thumbs=a.site/'thumbs';thumbs.mkdir(exist_ok=True)
    for c in cat['items']:
        d=json.loads((a.site/c['data']).read_text());blocks=np.array(d['blocks'],dtype=np.int32);palette=d['palette'];width,height=c['size'][0],c['size'][2];scale=min(1,320/max(width,height));im=Image.new('RGB',(max(1,int(width*scale)+1),max(1,int(height*scale)+1)),(23,31,39));px=im.load();order=np.argsort(blocks[:,1],kind='stable')
        palette_colors=[]
        for s in palette:
            ms=models_for(r,s);col=[140,140,140]
            if ms:
                m=r.model(ms[0]['model']);els=m.get('elements',[])
                if els:
                    f=els[0].get('faces',{});face=f.get('up',next(iter(f.values()),{}));t=r.texture(m,face.get('texture','minecraft:block/stone'));col=colors.get(t,col)
            palette_colors.append(col)
        for idx in order:
            x,y,z,p=blocks[idx];col=palette_colors[p]
            col=tuple(max(0,min(255,int(v*(.68+.32*(y/max(1,c['size'][1])))))) for v in col);px[min(im.width-1,int(x*scale)),min(im.height-1,int(z*scale))]=col
        im.thumbnail((320,220),Image.Resampling.NEAREST);im.save(thumbs/(c['id']+'.png'))
    print('States',len(states),'textures',len(names),'thumbnails',len(cat['items']))
if __name__=='__main__':main()

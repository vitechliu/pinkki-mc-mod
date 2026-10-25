#!/usr/bin/env python3
"""Read-only Anvil inventory and reproducible candidate extraction. Never opens source for writing."""
import argparse, collections, concurrent.futures, gzip, hashlib, io, json, math, os, pathlib, struct, time, zlib
import numpy as np

class Reader:
    def __init__(self,b): self.b=b; self.p=0
    def take(self,n): p=self.p; self.p+=n; return self.b[p:self.p]
    def number(self,f): return struct.unpack('>'+f,self.take(struct.calcsize('>'+f)))[0]
    def string(self): return self.take(self.number('H')).decode('utf-8',errors='replace')
    def tag(self,t):
        if t in (1,2,3,4,5,6): return self.number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t==8:return self.string()
        if t in (7,11,12):
            n=self.number('i'); return np.frombuffer(self.take(n*{7:1,11:4,12:8}[t]),dtype={7:'i1',11:'>i4',12:'>i8'}[t])
        if t==9:
            ty=self.number('B'); return [self.tag(ty) for _ in range(self.number('i'))]
        if t==10:
            d={}
            while True:
                ty=self.number('B')
                if ty==0:return d
                name=self.string(); d[name]=self.tag(ty)
        raise ValueError('Unknown NBT tag '+str(t))
    def root(self):
        t=self.number('B');self.string();return self.tag(t)

def region_chunks(path):
    with open(path,'rb') as f:
        header=f.read(4096)
        for i in range(1024):
            loc=int.from_bytes(header[i*4:i*4+4],'big'); off=loc>>8
            if not off:continue
            f.seek(off*4096); n=int.from_bytes(f.read(4),'big'); mode=f.read(1)[0]; raw=f.read(n-1)
            if mode&128:
                rx,rz=map(int,path.stem.split('.')[1:]); raw=(path.parent/f'c.{rx*32+i%32}.{rz*32+i//32}.mcc').read_bytes(); mode&=127
            if mode==2:raw=zlib.decompress(raw)
            elif mode==1:raw=gzip.decompress(raw)
            elif mode!=3:raise ValueError(f'Unsupported compression {mode} in {path.name}')
            yield i,raw

def section_values(s):
    bs=s['block_states']; n=len(bs['palette'])
    if n==1:return np.zeros(4096,dtype=np.uint16)
    bits=max(4,(n-1).bit_length()); per=64//bits; words=np.asarray(bs['data'],dtype=np.uint64)
    return ((words[:,None] >> (np.arange(per,dtype=np.uint64)*bits)) & ((1<<bits)-1)).reshape(-1)[:4096].astype(np.uint16)

SOIL={'dirt','grass_block','coarse_dirt','rooted_dirt','podzol','mycelium','mud','clay','sand','red_sand','gravel','farmland','dirt_path','snow','snow_block','powder_snow','bedrock'}
NATURAL={'stone','deepslate','andesite','diorite','granite','tuff','calcite','dripstone_block','pointed_dripstone','netherrack','basalt','smooth_basalt','blackstone','end_stone','soul_sand','soul_soil','water','lava','ice','packed_ice','blue_ice','magma_block','obsidian','crying_obsidian','amethyst_block','budding_amethyst','moss_block','moss_carpet','pumpkin','melon','cactus','sugar_cane','bamboo','vine','glow_lichen','kelp','kelp_plant','seagrass','tall_seagrass','short_grass','grass','tall_grass','fern','large_fern','dead_bush','lily_pad','cobweb','spawner','infested_stone','infested_deepslate','nether_portal','end_portal','end_portal_frame','chorus_plant','chorus_flower','dandelion','poppy','brown_mushroom','red_mushroom','brown_mushroom_block','red_mushroom_block','mushroom_stem','nether_sprouts','crimson_roots','warped_roots','warped_wart_block','nether_wart_block','weeping_vines','weeping_vines_plant','twisting_vines','twisting_vines_plant','crimson_nylium','warped_nylium','sandstone','red_sandstone','terracotta'}
AIR={'air','cave_air','void_air'}
def kind(name):
    n=name.split(':')[-1]
    if n in AIR or n in SOIL:return 0
    if n in NATURAL or any(x in n for x in ('_ore','_leaves','_log','_stem','_hyphae','coral','azalea','flower','tulip','sapling','_roots','_bud','amethyst_cluster')):return 0
    if n in ('cobblestone','mossy_cobblestone','torch','wall_torch','rail','iron_bars','chain','chest','barrel','ladder','bookshelf','stone_bricks','mossy_stone_bricks','cracked_stone_bricks') or n.endswith(('_planks','_fence','_fence_gate','_stairs','_slab','_wall','_door','_trapdoor','_bed')):return 1
    return 2

def scan_region(args):
    path,out,world=args;path=pathlib.Path(path);dest=pathlib.Path(out)/world/path.stem;dest.parent.mkdir(parents=True,exist_ok=True)
    if pathlib.Path(str(dest)+'.json').exists():return json.loads(pathlib.Path(str(dest)+'.json').read_text())['summary']
    states=[];state_ids={}; coords=[]; chunks=[]; starts=[];errors=[];versions=collections.Counter();total=0
    try:
      for index,raw in region_chunks(path):
        total+=1
        try:
            c=Reader(raw).root(); versions[c.get('DataVersion',0)]+=1; cx=c.get('xPos');cz=c.get('zPos'); count=strong=0; ys=[]
            for s in c.get('sections',[]):
                if 'block_states' not in s:continue
                pal=s['block_states']['palette'];ks=np.array([kind(p['Name']) for p in pal],dtype=np.uint8)
                if not ks.any():continue
                ids=section_values(s); mask=ks[ids]>0;ind=np.flatnonzero(mask)
                if not len(ind):continue
                mapping=[]
                for st in pal:
                    key=json.dumps(st,sort_keys=True,separators=(',',':'))
                    if key not in state_ids:state_ids[key]=len(states);states.append(st)
                    mapping.append(state_ids[key])
                ar=np.empty((len(ind),4),dtype=np.int32);ar[:,0]=cx*16+ind%16;ar[:,1]=s['Y']*16+ind//256;ar[:,2]=cz*16+(ind//16)%16;ar[:,3]=np.array(mapping)[ids[ind]];coords.append(ar)
                count+=len(ind);strong+=int((ks[ids[ind]]==2).sum());ys.extend([int(ar[:,1].min()),int(ar[:,1].max())])
            if count:chunks.append([cx,cz,count,strong,min(ys),max(ys),c.get('InhabitedTime',0)])
            for name,st in c.get('structures',{}).get('starts',{}).items():
                if st.get('id')!='INVALID':
                    for child in st.get('Children',[]):
                        if 'BB' in child:starts.append([name,[int(v) for v in child['BB']]])
        except Exception as e:errors.append([index,str(e)])
    except Exception as e:errors.append(['region',str(e)])
    summary={'file':f'{world}/{path.name}','chunks':total,'signal_chunks':len(chunks),'blocks':sum(x[2] for x in chunks),'versions':dict(versions),'errors':errors,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
    np.savez_compressed(str(dest)+'.npz',blocks=np.concatenate(coords) if coords else np.empty((0,4),dtype=np.int32))
    pathlib.Path(str(dest)+'.json').write_text(json.dumps({'summary':summary,'states':states,'chunks':chunks,'starts':starts},ensure_ascii=False))
    return summary

def main():
    ap=argparse.ArgumentParser();ap.add_argument('source',type=pathlib.Path);ap.add_argument('output',type=pathlib.Path);ap.add_argument('--workers',type=int,default=4);a=ap.parse_args();a.output.mkdir(parents=True,exist_ok=True)
    tasks=[]
    for w in sorted(a.source.glob('world*')):
        for f in sorted(w.rglob('region/*.mca')):tasks.append((str(f),str(a.output/'scan'),w.name))
    results=[];t=time.time()
    with concurrent.futures.ProcessPoolExecutor(max_workers=a.workers) as pool:
      for r in pool.map(scan_region,tasks,chunksize=1):
        results.append(r)
        if len(results)%20==0:print(json.dumps({'files':len(results),'total':len(tasks),'chunks':sum(x['chunks'] for x in results),'seconds':round(time.time()-t),'errors':sum(len(x['errors']) for x in results)}),flush=True)
    (a.output/'inventory.json').write_text(json.dumps({'regions':results,'elapsed':time.time()-t},ensure_ascii=False,indent=2));print('Complete',len(results),flush=True)
if __name__=='__main__':main()

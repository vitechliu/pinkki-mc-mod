#!/usr/bin/env python3
"""Recover nearby timber/details and typed block entities for named review candidates."""
import argparse,collections,functools,gzip,io,json,pathlib,struct,zlib,hashlib
import numpy as np,nbtlib as nbt
from scan import Reader,section_values,SOIL,AIR
from extract import structure,kind

@functools.lru_cache(maxsize=16)
def region_index(path):
    p=pathlib.Path(path)
    if not p.exists():return b''
    return p.read_bytes()

def chunk(source,world,x,z):
    folder=source/world/({'world':'','world_nether':'DIM-1','world_the_end':'DIM1'}[world])/'region';p=folder/f'r.{x//32}.{z//32}.mca';b=region_index(str(p))
    if not b:return None,None
    idx=(x%32+(z%32)*32)*4;loc=int.from_bytes(b[idx:idx+4],'big');off=(loc>>8)*4096
    if not off:return None,None
    size=int.from_bytes(b[off:off+4],'big');mode=b[off+4];raw=b[off+5:off+4+size]
    if mode&128:raw=(folder/f'c.{x}.{z}.mcc').read_bytes();mode&=127
    if mode==2:raw=zlib.decompress(raw)
    elif mode==1:raw=gzip.decompress(raw)
    elif mode!=3:raise ValueError(mode)
    return Reader(raw).root(),raw

def main():
    ap=argparse.ArgumentParser();ap.add_argument('source',type=pathlib.Path);ap.add_argument('site',type=pathlib.Path);a=ap.parse_args();cat=json.loads((a.site/'catalog.json').read_text());selected=[c for c in cat['items'] if c['evidence'] and not c.get('enriched') and max(c['size'])<=512 and c['blocks']<=300000];total=0
    for count,c in enumerate(selected,1):
        print('recovering',c['id'],c['name'],flush=True)
        f=a.site/c['data'];d=json.loads(f.read_text());blocks=np.array(d['blocks'],np.int32);pal=d['palette'];origin=np.array(c['origin']);worldpos=blocks[:,:3]+origin;size=np.array(c['size']);shift=np.array([4,4,4]);box=size+8
        def encode(arr):
            v=arr+shift;return v[:,0].astype(np.int64)+box[0]*(v[:,2].astype(np.int64)+box[2]*v[:,1].astype(np.int64))
        keys=np.unique(encode(blocks[:,:3]));keyset=set(map(int,keys));dense=np.zeros(int(np.prod(box)),dtype=bool);dense[keys]=True;mapstate={json.dumps(s,sort_keys=True):i for i,s in enumerate(pal)};extras=[];bes={};positions=[]
        chunkcoords=set((int(p[0]//16),int(p[2]//16)) for p in worldpos)
        offsets=np.array([[x,y,z] for x in (-1,0,1) for y in (-2,-1,0,1,2) for z in (-1,0,1)])
        offsets_keys=offsets[:,0]+box[0]*(offsets[:,2]+box[2]*offsets[:,1]);dirs=np.array([1,-1,box[0],-box[0],box[0]*box[2],-box[0]*box[2]])
        for cx,cz in sorted(chunkcoords):
            cc,raw=chunk(a.source,c['world'],cx,cz)
            if cc is None:continue
            for s in cc.get('sections',[]):
                if 'block_states' not in s or s['Y']*16>c['origin'][1]+c['size'][1] or s['Y']*16+15<c['origin'][1]:continue
                palette=s['block_states']['palette'];modes=[]
                for st in palette:
                    n=st['Name'].split(':')[-1];p=st.get('Properties',{})
                    modes.append(1 if n.endswith(('_log','_wood','_stem','_hyphae','_terracotta')) or (n.endswith('_leaves') and p.get('persistent')=='true') else 2 if n in ('stone','deepslate','andesite','granite','diorite','sandstone','end_stone','blackstone','netherrack','packed_ice','blue_ice','ice','obsidian') else 0)
                if not any(modes):continue
                ids=section_values(s);mode=np.array(modes)[ids];ind=np.flatnonzero(mode>0)
                if not len(ind):continue
                pos=np.column_stack((cx*16+ind%16,s['Y']*16+ind//256,cz*16+(ind//16)%16))-origin;within=np.all((pos>=0)&(pos<size),axis=1);pos=pos[within];inds=ind[within]
                if not len(pos):continue
                kk=encode(pos);iswood=mode[inds]==1;keep=np.zeros(len(pos),dtype=bool)
                for delta in offsets_keys:keep |= dense[kk+delta]&iswood
                # Natural stone only when it is enclosed by at least three crafted neighbors.
                adjacent=sum(dense[kk+delta] for delta in dirs);keep |= (adjacent>=3)&~iswood
                keep &= ~np.isin(kk,keys)
                for p,ind0,k in zip(pos[keep],inds[keep],kk[keep]):
                    st=palette[ids[ind0]];skey=json.dumps(st,sort_keys=True)
                    if skey not in mapstate:mapstate[skey]=len(pal);pal.append(st)
                    extras.append([int(p[0]),int(p[1]),int(p[2]),mapstate[skey]]);keyset.add(int(k))
            if cc.get('block_entities'):
                typed=nbt.File.parse(io.BytesIO(raw))
                for be in typed.get('block_entities',[]):
                    pos=np.array([int(be.get(k,0)) for k in ('x','y','z')])-origin
                    if np.all((pos>=0)&(pos<size)) and int(encode(pos[None,:])[0]) in keyset:
                        be=nbt.Compound(be);[be.pop(k,None) for k in ('x','y','z')];bes[tuple(map(int,pos))]=be
        if extras:blocks=np.concatenate((blocks,np.array(extras,np.int32)))
        d['palette']=pal;d['blocks']=blocks.tolist();f.write_text(json.dumps(d,separators=(',',':')));structure(a.site/c['nbt'],blocks,pal,size,bes)
        c['enriched']=True
        c['sha256']=hashlib.sha256((a.site/c['nbt']).read_bytes()).hexdigest();c['blocks']=len(blocks);c['states']=len(pal);c['recovered_blocks']=len(extras);c['block_entities']=len(bes)
        c['warnings']=['已去除泥土、沙砾等地形；只补回贴近建筑核心的原木/陶瓦与被建筑包围的岩石，边界仍需审阅。','结构保留对应方块实体 NBT（容器、告示牌等），不含展示框、盔甲架及生物实体。']+c['warnings'][2:]
        c['histogram']=dict(collections.Counter(pal[int(i)]['Name'] for i in blocks[:,3]).most_common());total+=len(extras)
        (a.site/'catalog.json').write_text(json.dumps(cat,ensure_ascii=False))
        if count%10==0:print('enrich',count,'/',len(selected),'recovered',total,flush=True)
    cat['report']['enrichedCandidates']=len(selected);cat['report']['recoveredBlocks']=total;cat['report']['extraction']='conservative-core + anchored-detail-recovery'
    cat['report']['limitations'][1]='泥土地形全部排除；有来源线索的候选已补回邻近原木等细节，纯天然材料部分仍可能遗漏。'
    (a.site/'catalog.json').write_text(json.dumps(cat,ensure_ascii=False));print('complete',len(selected),total,flush=True)
if __name__=='__main__':main()

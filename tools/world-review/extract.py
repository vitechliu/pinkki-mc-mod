#!/usr/bin/env python3
"""Build a review catalog and vanilla structure NBTs from an immutable scan."""
import argparse, collections, copy, gzip, hashlib, io, itertools, json, math, pathlib, re, sqlite3, struct, time, zipfile
import nbtlib as nbt, numpy as np, yaml
from PIL import Image, ImageDraw
from scan import Reader, region_chunks, section_values, SOIL, AIR, kind as old_kind

# These naturally occurring materials are not reliable construction signals.
EXTRA_NATURAL={'sculk','sculk_vein','sculk_sensor','sculk_shrieker','sculk_catalyst','bubble_column','cave_vines','cave_vines_plant','pink_petals','azure_bluet','big_dripleaf','big_dripleaf_stem','small_dripleaf','sea_pickle','raw_iron_block','raw_copper_block','bone_block','rose_bush','lilac','peony','cocoa','oxeye_daisy','allium','lily_of_the_valley','sunflower','blue_orchid','sweet_berry_bush','beetroots','wheat','potatoes','carrots','nether_wart','fire','soul_fire','frogspawn','turtle_egg','sniffer_egg','attached_melon_stem','attached_pumpkin_stem','melon_stem','pumpkin_stem','hanging_roots','spore_blossom','light','structure_void'}
def kind(n):
    s=n.split(':')[-1]
    if s.startswith('stripped_'):return 2
    if s in EXTRA_NATURAL or s.startswith('infested_'):return 0
    if s.endswith('_terracotta') and not s.endswith('_glazed_terracotta'):return 0
    if s in {'prismarine','prismarine_bricks','dark_prismarine','sea_lantern','purpur_block','purpur_pillar','end_stone_bricks','nether_bricks','red_nether_bricks','polished_deepslate','chiseled_deepslate','deepslate_bricks','deepslate_tiles','cracked_deepslate_tiles','cracked_deepslate_bricks','chiseled_stone_bricks','chiseled_sandstone','cut_sandstone','smooth_sandstone','cut_red_sandstone','smooth_red_sandstone','gilded_blackstone','polished_blackstone_bricks','cracked_polished_blackstone_bricks','chiseled_polished_blackstone','polished_blackstone'}:return 1
    return old_kind(n)

def anchors(source):
    out=[]
    for f in sorted((source/'plugins/Residence/Save/Worlds').glob('*.yml')):
        d=yaml.safe_load(f.read_text());world=f.stem.removeprefix('res_')
        def walk(items,prefix=''):
          for name,r in items.items():
            for area,s in r.get('Areas',{}).items():
                v=list(map(int,s.split(':')));lo=[min(v[i],v[i+3]) for i in range(3)];hi=[max(v[i],v[i+3]) for i in range(3)]
                out.append({'name':prefix+name+(' / '+area if area!='main' else ''),'world':world,'lo':lo,'hi':hi,'type':'residence','owner':r.get('Permissions',{}).get('OwnerLastKnownName','')})
            walk(r.get('Subzones',{}),prefix+name+'/')
        walk(d.get('Residences',{}))
    for f in sorted((source/'plugins/Essentials/userdata').glob('*.yml')):
        d=yaml.safe_load(f.read_text()) or {}
        for name,h in (d.get('homes') or {}).items():
            if not isinstance(h,dict) or 'world' not in h:continue
            if all(k in h for k in ('x','y','z')):
                pos=[int(h[k]) for k in ('x','y','z')];out.append({'name':'家 / '+name,'world':h['world'],'lo':pos,'hi':pos,'type':'home','owner':d.get('last-account-name','')})
    for f in sorted((source/'plugins/Essentials/warps').glob('*.yml')):
        d=yaml.safe_load(f.read_text()) or {}
        if all(k in d for k in ('x','y','z','world')):
            pos=[int(d[k]) for k in ('x','y','z')];out.append({'name':'传送 / '+f.stem,'world':d['world'],'lo':pos,'hi':pos,'type':'warp','owner':''})
    database=source/'plugins/PlayerWarp/PlayerWarp.db'
    if database.exists():
        connection=sqlite3.connect(database.as_uri()+'?mode=ro&immutable=1',uri=True)
        for name,owner,location in connection.execute('select name,player_name,warp_location from warp_player'):
            h=json.loads(location);pos=[int(h[k]) for k in ('x','y','z')]
            out.append({'name':name,'world':h['world'],'lo':pos,'hi':pos,'type':'playerwarp','owner':owner})
        connection.close()
    return out

class Union:
    def __init__(self):self.parent=[]
    def add(self):i=len(self.parent);self.parent.append(i);return i
    def root(self,i):
        while self.parent[i]!=i:self.parent[i]=self.parent[self.parent[i]];i=self.parent[i]
        return i
    def join(self,a,b):a=self.root(a);b=self.root(b);self.parent[a]=b

def classify(names,counts,world):
    text=' '.join(names).lower(); total=sum(counts.values())
    if any(w in text for w in ['塔','刷','农场','树场','养殖','slime','pig','交易','全树','挂机','蛙鸣','史莱姆']):return '生产设施'
    if any(w in text for w in ['城堡','出生','粉红派','安全','相亲','sky','教堂']):return '公共与地标'
    if any(w in text for w in ['码头','交通','铁路','公路','航线']):return '交通与通道'
    if any(w in text for w in ['钓鱼','小屋','别墅','之家','home','家 /','睡觉','bed','二楼','vv1']):return '住宅与休闲'
    technical=sum(v for k,v in counts.items() if any(s in k for s in ['hopper','piston','observer','repeater','comparator','redstone','dispenser','dropper']))
    if technical>=20:return '生产设施'
    rails=sum(v for k,v in counts.items() if 'rail' in k)
    if rails>total*.3:return '交通与通道'
    return '待确认'

def build_candidates(root,anc):
    byworld={};allstarts={};stats={}; summaries=[]
    for world in ('world','world_nether','world_the_end'):
        chunks=[];states=[];lookup={};starts=[];inh={}
        for f in sorted((root/'scan'/world).glob('*.json')):
            d=json.loads(f.read_text());summaries.append(d['summary']);starts+=d['starts']
            ar=np.load(f.with_suffix('.npz'))['blocks']
            if not len(ar):continue
            ks=np.array([kind(s['Name']) for s in d['states']]);ar=ar[ks[ar[:,3]]>0]
            if not len(ar):continue
            mapping=[]
            for s in d['states']:
                key=json.dumps(s,sort_keys=True)
                if key not in lookup:lookup[key]=len(states);states.append(s)
                mapping.append(lookup[key])
            ar[:,3]=np.asarray(mapping)[ar[:,3]];chunks.append(ar)
            inh.update({(c[0],c[1]):c[6] for c in d['chunks']})
        arr=np.concatenate(chunks) if chunks else np.empty((0,4),np.int32)
        byworld[world]=(arr,states);allstarts[world]=starts;stats[world]={'signal_blocks':len(arr),'states':len(states)}
    candidates=[];tiny=[]
    pathlib.Path(root/'extract.pid').write_text(str(__import__('os').getpid()))
    for world,(arr,states) in byworld.items():
        print('cluster',world,len(arr),flush=True)
        natural_index=collections.defaultdict(list)
        for st,box in allstarts[world]:
            for bx in range(box[0]//128,box[3]//128+1):
                for bz in range(box[2]//128,box[5]//128+1):natural_index[(bx,bz)].append((st,box))
        # 16x24x16 cells, with touching neighboring cells joined in 3D.
        cells=np.floor_divide(arr[:,:3],np.array([16,24,16]));packed=(cells[:,0].astype(np.int64)+2097152)*4194304*64+(cells[:,2].astype(np.int64)+2097152)*64+(cells[:,1]+8);_,first,inverse=np.unique(packed,return_index=True,return_inverse=True);unique=cells[first]
        keys={tuple(v):i for i,v in enumerate(unique)};u=Union();u.parent=list(range(len(unique)))
        prev=[v for v in itertools.product((-1,0,1),repeat=3) if v<(0,0,0)]
        for key,i in keys.items():
            for delta in prev:
                j=keys.get(tuple(key[k]+delta[k] for k in range(3)))
                if j is not None:u.join(i,j)
        labels=np.array([u.root(i) for i in range(len(unique))])[inverse];order=np.argsort(labels);splits=np.flatnonzero(np.diff(labels[order]))+1
        print('components',world,len(splits)+1,flush=True)
        for indices in np.split(order,splits):
            if len(indices)<32: tiny.append({'world':world,'lo':arr[indices,:3].min(0).tolist(),'hi':arr[indices,:3].max(0).tolist(),'blocks':len(indices)});continue
            a=arr[indices];lo=a[:,:3].min(0);hi=a[:,:3].max(0)
            names=[];evidence=[]
            for anchor in anc:
                if anchor['world']!=world:continue
                if all(anchor['hi'][k]>=lo[k]-24 and anchor['lo'][k]<=hi[k]+24 for k in range(3)):
                    names.append(anchor['name']);evidence.append(anchor)
            hist=collections.Counter()
            ids,ct=np.unique(a[:,3],return_counts=True)
            for i,n in zip(ids,ct):hist[states[i]['Name']]+=int(n)
            strong=sum(n for name,n in hist.items() if kind(name)==2)
            # Strongly natural pieces remain in a separate catalog, never silently called player builds.
            overlaps=[]
            for bx in range(int(lo[0])//128,int(hi[0])//128+1):
                for bz in range(int(lo[2])//128,int(hi[2])//128+1):
                    for st,box in natural_index.get((bx,bz),[]):
                        if all(box[k+3]>=lo[k] and box[k]<=hi[k] for k in range(3)):overlaps.append(st)
            natural=bool(overlaps) and not evidence and strong<max(32,len(a)*.015)
            if not evidence and strong<8 and len(a)<128:
                tiny.append({'world':world,'lo':lo.tolist(),'hi':hi.tolist(),'blocks':len(a),'reason':'weak_signal'});continue
            cat='疑似天然结构' if natural else classify(names,hist,world)
            candidates.append({'world':world,'lo':lo.tolist(),'hi':hi.tolist(),'indices':indices,'names':names,'evidence':evidence,'histogram':dict(hist.most_common()),'strong':strong,'natural':sorted(set(overlaps)),'category':cat,'confidence':'领地/传送点关联' if evidence else ('天然结构重叠' if natural else '方块特征推测'),'type':'cluster','signal_count':len(a)})
        # Large residence boxes are useful individually even when roads join several buildings.
        for anchor in anc:
            if anchor['world']!=world or anchor['type']!='residence':continue
            size=np.array(anchor['hi'])-anchor['lo']+1
            if size[0]<12 or size[2]<12 or np.prod(size)<1000:continue
            lo=np.array(anchor['lo'])-np.array([2,4,2]);hi=np.array(anchor['hi'])+np.array([2,6,2]);mask=np.all((arr[:,:3]>=lo)&(arr[:,:3]<=hi),axis=1);ind=np.flatnonzero(mask)
            if len(ind)<32:continue
            hist=collections.Counter(states[int(i)]['Name'] for i in arr[ind,3]); candidates.append({'world':world,'lo':lo.tolist(),'hi':hi.tolist(),'indices':ind,'names':[anchor['name']],'evidence':[anchor],'histogram':dict(hist.most_common()),'strong':sum(n for s,n in hist.items() if kind(s)==2),'natural':[],'category':classify([anchor['name']],hist,world),'confidence':'领地边界（可能仅覆盖局部）','type':'residence','signal_count':len(ind)})
    candidates.sort(key=lambda c:(not bool(c['evidence']), c['category']=='疑似天然结构',-c['signal_count']))
    return byworld,candidates,tiny,stats,summaries

# Independent NBT writer via nbtlib for actual Minecraft structure files.
def structure(path,blocks,states,size,block_entities):
    palette=nbt.List[nbt.Compound]([nbt.Compound({'Name':nbt.String(s['Name']),**({'Properties':nbt.Compound({k:nbt.String(v) for k,v in s['Properties'].items()})} if s.get('Properties') else {})}) for s in states])
    data=[]
    for x,y,z,i in blocks:
        entry=nbt.Compound({'pos':nbt.List[nbt.Int]([int(x),int(y),int(z)]),'state':nbt.Int(int(i))})
        be=block_entities.get((int(x),int(y),int(z)))
        if be:entry['nbt']=be
        data.append(entry)
    nbt.File({'DataVersion':nbt.Int(3839),'size':nbt.List[nbt.Int](list(map(int,size))),'palette':palette,'blocks':nbt.List[nbt.Compound](data),'entities':nbt.List[nbt.Compound]([])}).save(path,gzipped=True)

def texture_color(name):
    colors={'white':0xe8e5df,'orange':0xdb8033,'magenta':0xb64bb0,'light_blue':0x66a9d2,'yellow':0xe5c53a,'lime':0x87ae27,'pink':0xe59bb0,'gray':0x494950,'light_gray':0x97968e,'cyan':0x287c8b,'purple':0x8049aa,'blue':0x3e4a9d,'brown':0x72513a,'green':0x527e32,'red':0xaa3a30,'black':0x29272c}
    for k,v in colors.items():
        if name.startswith(k+'_'):return v
    if any(s in name for s in ['wood','plank','log','fence','oak','chest','crafting','barrel']):return 0xa78153
    if 'cherry' in name:return 0xdfadb2
    if 'glass' in name:return 0x9ccacc
    if 'nether' in name:return 0x683b43
    if 'prismarine' in name:return 0x5d9d91
    if 'copper' in name:return 0xc37f59
    if 'quartz' in name:return 0xe5e1d7
    return 0x8b8e94

def main():
    ap=argparse.ArgumentParser();ap.add_argument('source',type=pathlib.Path);ap.add_argument('root',type=pathlib.Path);a=ap.parse_args();out=a.root/'site';(out/'assets').mkdir(parents=True,exist_ok=True);(out/'structures').mkdir(exist_ok=True)
    anc=anchors(a.source);worlds,candidates,tiny,stats,summaries=build_candidates(a.root,anc)
    (a.root/'anchors.json').write_text(json.dumps(anc,ensure_ascii=False,indent=2));(a.root/'small-signals.json').write_text(json.dumps(tiny,ensure_ascii=False))
    np.savez_compressed(a.root/'world-signals.npz',**{w:d[0] for w,d in worlds.items()})
    catalog=[]
    for num,c in enumerate(candidates,1):
        ident=f'B{num:04d}';arr,states=worlds[c['world']];blocks=arr[c.pop('indices')].copy();lo=blocks[:,:3].min(0);hi=blocks[:,:3].max(0);blocks[:,:3]-=lo
        used=np.unique(blocks[:,3]);mapping={int(old):i for i,old in enumerate(used)};pal=[states[i] for i in used];blocks[:,3]=np.array([mapping[int(i)] for i in blocks[:,3]])
        # Candidate masks contain no soil, natural terrain, inventories or entities.
        size=(hi-lo+1).tolist();c['origin']=lo.tolist();c['size']=size;c['id']=ident;c['name']=' · '.join(c['names'][:3]) or f'{c["category"]} {ident}';c['blocks']=len(blocks);c['states']=len(pal)
        c['warnings']=['保守提取：排除泥土、天然岩石、原木与植被；可能缺少天然材料搭建的部分。','不含生物、展示框、盔甲架及方块实体 NBT；机械、招牌和容器需挑选后补提。']
        if c['type']=='residence':c['warnings'].append('按领地范围切片，建筑可能越界；可对照同位置的聚落总览。')
        if max(size)>256:c['warnings'].append('大型连通片，可能由道路、矿道连接多栋建筑，需二次分割。')
        c['nbt']=f'structures/{ident}.nbt';c['data']=f'assets/{ident}.json';c['sha256']=''
        data={'palette':pal,'blocks':blocks.tolist(),'size':size}
        (out/c['data']).write_text(json.dumps(data,separators=(',',':')))
        structure(out/c['nbt'],blocks,pal,size,{})
        c['sha256']=hashlib.sha256((out/c['nbt']).read_bytes()).hexdigest();catalog.append(c)
        if num%50==0:print('export',num,'/',len(candidates),flush=True)
    levels={}
    for w in worlds:
        d=nbt.load(a.source/w/'level.dat')['Data'];levels[w]={'version':str(d['Version']['Name']),'dataVersion':int(d['DataVersion']),'spawn':[int(d.get('Spawn'+k,0)) for k in 'XYZ']}
    report={'sourceLabel':a.source.name,'worlds':levels,'stats':stats,'regions':len(summaries),'chunks':sum(s['chunks'] for s in summaries),'errors':[{'file':s['file'],'errors':s['errors']} for s in summaries if s['errors']],'anchors':len(anc),'smallSignals':len(tiny),'candidates':len(catalog),'extraction':'conservative-core','limitations':['无完整方块操作历史，不能保证识别所有玩家建筑，纯天然材料建筑可能漏检。','泥土与地形全部排除；天然原木、岩石和植被也排除，需按审阅反馈补提。','1.20.6 原始方块状态，不是已经验证可用于 1.20.1 的 mod 资产。','候选保留重叠的领地切片和连通聚落总览；不要直接计数为独立建筑。']}
    (out/'catalog.json').write_text(json.dumps({'report':report,'items':catalog},ensure_ascii=False))
    (a.root/'extraction-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print(json.dumps(report,ensure_ascii=False),flush=True)
if __name__=='__main__':main()

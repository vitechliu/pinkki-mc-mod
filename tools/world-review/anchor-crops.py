#!/usr/bin/env python3
"""Add modest review windows around every named anchor, avoiding giant road-connected groups."""
import argparse,collections,json,pathlib,hashlib
import numpy as np
from extract import kind,structure,classify

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=pathlib.Path);a=ap.parse_args();site=a.root/'site';cat=json.loads((site/'catalog.json').read_text());anc=json.loads((a.root/'anchors.json').read_text());byworld={}
    for w in ('world','world_nether','world_the_end'):
        arrays=[];pal=[];states={}
        for f in sorted((a.root/'scan'/w).glob('*.json')):
            d=json.loads(f.read_text());b=np.load(f.with_suffix('.npz'))['blocks'];mapping=[]
            for s in d['states']:
                k=json.dumps(s,sort_keys=True)
                if k not in states:states[k]=len(pal);pal.append(s)
                mapping.append(states[k])
            if len(b):
                b=b[np.array([kind(s['Name'])>0 for s in d['states']])[b[:,3]]];b[:,3]=np.array(mapping)[b[:,3]];arrays.append(b)
        byworld[w]=(np.concatenate(arrays),pal)
    coverage=[];new=[];existing={c['id'] for c in cat['items']}
    for num,an in enumerate(anc,1):
        if f'A{num:04d}' in existing:continue
        if an['world'] not in byworld:continue
        arr,pal=byworld[an['world']];center=(np.array(an['lo'])+an['hi'])/2;size=np.array(an['hi'])-an['lo']+1
        if an['type']=='residence' and size[0]>=12 and size[2]>=12 and np.prod(size)>=1000:
            coverage.append({'anchor':an['name'],'world':an['world'],'status':'residence_crop'});continue
        radius=40 if an['world']=='world' else 56
        lo=center-np.array([radius,80,radius]);hi=center+np.array([radius,130,radius]);mask=np.all((arr[:,:3]>=lo)&(arr[:,:3]<=hi),axis=1);blocks=arr[mask].copy()
        if len(blocks)<8:coverage.append({'anchor':an['name'],'world':an['world'],'status':'insufficient_core','blocks':len(blocks)});continue
        origin=blocks[:,:3].min(0);high=blocks[:,:3].max(0);blocks[:,:3]-=origin;used=np.unique(blocks[:,3]);mp={int(v):i for i,v in enumerate(used)};palette=[pal[i] for i in used];blocks[:,3]=np.array([mp[int(i)] for i in blocks[:,3]]);ident=f'A{num:04d}';dims=(high-origin+1).tolist();hist=collections.Counter(palette[int(i)]['Name'] for i in blocks[:,3]);catname=classify([an['name']],hist,an['world'])
        c={'id':ident,'world':an['world'],'lo':origin.tolist(),'hi':high.tolist(),'origin':origin.tolist(),'size':dims,'names':[an['name']],'name':an['name'],'evidence':[an],'histogram':dict(hist.most_common()),'strong':sum(v for k,v in hist.items() if kind(k)==2),'natural':[],'category':catname,'confidence':'命名位置周围的自动审阅窗口','type':'anchor','signal_count':len(blocks),'blocks':len(blocks),'states':len(palette),'warnings':['按命名位置周围自动切片，可能包含邻居或截断大建筑；可对照同坐标连通总览后调整。','保守去除地形；尚需检查天然材料与实体细节。'],'nbt':f'structures/{ident}.nbt','data':f'assets/{ident}.json'}
        (site/c['data']).write_text(json.dumps({'palette':palette,'blocks':blocks.tolist(),'size':dims},separators=(',',':')));structure(site/c['nbt'],blocks,palette,dims,{});c['sha256']=hashlib.sha256((site/c['nbt']).read_bytes()).hexdigest();new.append(c);coverage.append({'anchor':an['name'],'world':an['world'],'status':'anchor_crop','id':ident})
    cat['items']=new+cat['items'];cat['report']['anchorWindows']=sum(c['type']=='anchor' for c in cat['items']);cat['report']['anchors']=len(anc);cat['report']['candidates']=len(cat['items']);(site/'catalog.json').write_text(json.dumps(cat,ensure_ascii=False));previous=json.loads((a.root/'anchor-coverage.json').read_text()) if (a.root/'anchor-coverage.json').exists() else [];(a.root/'anchor-coverage.json').write_text(json.dumps(previous+coverage,ensure_ascii=False,indent=2));print('new windows',len(new),'unresolved',sum(x['status']=='insufficient_core' for x in coverage))
if __name__=='__main__':main()

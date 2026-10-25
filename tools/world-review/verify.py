#!/usr/bin/env python3
"""Independent structure readback, coordinate/palette checks and input hash verification."""
import argparse,collections,concurrent.futures,gzip,hashlib,json,pathlib,time
from scan import Reader,SOIL,section_values
from enrich import chunk

def verify_source(args):
    source,s=args;world,name=s['file'].split('/');folder=source/world/({'world':'','world_nether':'DIM-1','world_the_end':'DIM1'}[world])/'region';p=folder/name
    return s['file'] if hashlib.sha256(p.read_bytes()).hexdigest()!=s['sha256'] else None

def main():
    ap=argparse.ArgumentParser();ap.add_argument('source',type=pathlib.Path);ap.add_argument('root',type=pathlib.Path);a=ap.parse_args();site=a.root/'site';d=json.loads((site/'catalog.json').read_text());errors=[];blocks=0;hist=collections.Counter();allids=set();source_samples=collections.defaultdict(list)
    for c in d['items']:
        try:
            assert c['id'] not in allids;allids.add(c['id']);raw=(site/c['nbt']).read_bytes();assert hashlib.sha256(raw).hexdigest()==c['sha256']
            n=Reader(gzip.decompress(raw)).root();assert n['DataVersion']==3839;assert list(n['size'])==c['size'];assert len(n['blocks'])==c['blocks'];states=n['palette'];assert all(s['Name'].split(':')[-1] not in SOIL for s in states)
            for b in n['blocks']:
                assert 0<=b['state']<len(states);assert all(0<=p<c['size'][i] for i,p in enumerate(b['pos']))
            j=json.loads((site/c['data']).read_text());assert len(j['blocks'])==len(n['blocks']);assert j['palette']==states;assert j['size']==list(n['size'])
            # Compare every block, including ordering, not merely aggregate counts.
            assert all(x[:3]==y['pos'] and x[3]==y['state'] for x,y in zip(j['blocks'],n['blocks']))
            for i in sorted({0,len(j['blocks'])//2,len(j['blocks'])-1}):
                b=j['blocks'][i];pos=[b[k]+c['origin'][k] for k in range(3)];source_samples[(c['world'],pos[0]//16,pos[2]//16)].append((c['id'],pos,states[b[3]]))
            blocks+=len(n['blocks']);hist[c['category']]+=1
        except Exception as e:errors.append({'id':c['id'],'error':str(e) or type(e).__name__})
    checked=0
    for (world,cx,cz),samples in sorted(source_samples.items()):
        cc,_=chunk(a.source,world,cx,cz);sections={s['Y']:s for s in cc.get('sections',[]) if 'block_states' in s};decoded={}
        for ident,pos,state in samples:
            sy=pos[1]//16;s=sections.get(sy)
            if sy not in decoded and s:decoded[sy]=section_values(s)
            idx=(pos[0]%16)+(pos[2]%16)*16+(pos[1]%16)*256
            if s is None or s['block_states']['palette'][int(decoded[sy][idx])]!=state:errors.append({'id':ident,'error':'source block state mismatch','position':pos})
            checked+=1
    inv=json.loads((a.root/'inventory.json').read_text())['regions']
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:changed=[s for s in pool.map(verify_source,[(a.source,s) for s in inv]) if s]
    report={'structures':len(allids),'blocksIncludingOverlappingCrops':blocks,'errors':errors,'inputRegionsVerified':len(inv),'changedInputRegions':changed,'categories':dict(hist),'checks':['gzip and binary NBT readback','DataVersion and dimensions','palette/state and every coordinate range','no soil palette entries','JSON vs NBT every block','structure SHA256','source region SHA256 unchanged'],'sourceBlockSamples':checked,'minecraftRuntimeVerified':False}
    (a.root/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print(json.dumps(report,ensure_ascii=False,indent=2));raise SystemExit(bool(errors or changed))
if __name__=='__main__':main()

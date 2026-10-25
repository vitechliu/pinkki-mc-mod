#!/usr/bin/env python3
"""Rank weak/natural candidates without deleting any extracted evidence."""
import argparse,collections,json,pathlib

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=pathlib.Path);a=ap.parse_args();p=a.root/'site/catalog.json';d=json.loads(p.read_text());counts=collections.Counter()
    for c in d['items']:
        h=c['histogram'];n=sum(h.values());proof=sum(v for k,v in h.items() if any(t in k for t in ('concrete','beacon','diamond_block','netherite_block','hopper','observer','piston','redstone_lamp','enchanting_table','anvil','scaffolding','repeater','comparator','smooth_quartz','quartz_bricks','tinted_glass')))
        if not c['evidence']:
            if c['natural'] and proof<16:c['category']='疑似天然结构';c['confidence']='与存档天然结构范围重叠，未发现明确玩家线索'
            elif len(h)<=3 and proof==0:c['category']='地形与零散痕迹';c['confidence']='单一材料弱信号，可能是天然生成或玩家探索痕迹'
            if not c['names']:c['name']=c['category']+' '+c['id']
        c['playerSignal']=proof;counts[c['category']]+=1
    # Put standalone named windows/slices first; keep wide connected overviews for context.
    d['items'].sort(key=lambda c:(not bool(c['evidence']),{'residence':0,'anchor':1,'cluster':2}[c['type']],not bool(c.get('playerSignal')), -c['blocks']))
    r=d['report'];r['enrichedCandidates']=sum(bool(c.get('enriched')) for c in d['items']);r['recoveredBlocks']=sum(c.get('recovered_blocks',0) for c in d['items']);r['categoryCounts']=dict(counts);r['namedCandidates']=sum(bool(c['evidence']) for c in d['items']);r['reviewCandidates']=sum(c['category'] not in ('疑似天然结构','地形与零散痕迹') for c in d['items']);r['blockEntityCandidates']=sum(bool(c.get('block_entities')) for c in d['items']);r['candidates']=len(d['items']);r['coreProtectBlockHistoryRows']=0;r['sourceProtection']='read-only source; hashes recorded for every region file'
    p.write_text(json.dumps(d,ensure_ascii=False));(a.root/'extraction-report.json').write_text(json.dumps(r,ensure_ascii=False,indent=2));print(json.dumps(r,ensure_ascii=False,indent=2))
if __name__=='__main__':main()

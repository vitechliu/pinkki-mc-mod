#!/usr/bin/env python3
"""Parse with the locally cached 1.20.1 baseline; no game is launched."""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
cache=ROOT/'.local/gradle/caches'
out=ROOT/'.local/guaba-dragon-review'
out.mkdir(exist_ok=True)
def first(pattern):
    return next(cache.glob(pattern))
geo=first('forge_gradle/deobf_dependencies/software/bernie/geckolib/geckolib-forge-1.20.1/4.2.3*/*.jar')
mc=first('forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-*_mapped_official_1.20.1/*.jar')
libs=list((cache/'modules-2/files-2.1').glob('*/*/*/*/*.jar'))
cp=':'.join(map(str,[geo,mc]+[p for p in libs if 'geckolib' not in str(p) and 'neoforge' not in str(p)]))
jdk=ROOT/'.local/jdk-17/Contents/Home/bin'
if not jdk.exists(): jdk=ROOT/'.local/jdk-17/bin'
commands=[[str(jdk/'javac'),'-proc:none','-sourcepath',str(out/'empty-source'),'-cp',cp,'-d',str(out),str(HERE/'GeckoAssetCheck.java')],
[str(jdk/'java'),'-cp',str(out)+':'+cp,'GeckoAssetCheck',str(HERE/'guaba_dragon.geo.json'),str(HERE/'guaba_dragon.animation.json')]]
log='GeckoLib 4.2.3 / Forge 1.20.1: headless asset parsing\n'
for command in commands:
    result=subprocess.run(command,capture_output=True,text=True)
    log+=result.stdout+result.stderr
    (out/'geckolib-parser.log').write_text(log)
    if result.returncode:
        print(log[-5000:]);raise SystemExit(result.returncode)
print(log)

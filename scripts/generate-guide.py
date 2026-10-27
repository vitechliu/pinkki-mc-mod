#!/usr/bin/env python3
"""Compile the supported player-Wiki Markdown subset into bundled offline guide resources."""
import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
PAGES = ('handbook', 'items', 'guaba-workbench', 'guaba-potions', 'recipes', 'compatibility', 'bosses')
ITEM_IMAGES = {'/images/guaba_charging_station.png': 'pinkkicore:guaba_charging_station', '/images/charged_creeper_spawn_egg.png': 'pinkkicore:charged_creeper_spawn_egg', '/images/thunder_guaba.png': 'pinkkicore:thunder_guaba', '/images/guaba_bow.png': 'pinkkicore:guaba_bow', '/images/guaba_sword.png': 'pinkkicore:guaba_sword', '/images/guaba.png': 'pinkkicore:guaba',
               '/images/guaba_workbench.svg': 'pinkkicore:guaba_workbench'}
ITEM_IMAGES.update({f'/images/{name}.png': f'pinkkicore:{name}' for name in
                    ('pink_ore', 'deepslate_pink_ore', 'raw_pink', 'pink_ingot', 'pink_nugget', 'pink_block', 'raw_pink_block')})


def compile_book(root=ROOT):
    root = root.resolve()
    entries = []
    anchors = {}
    for page in PAGES:
        source = (root / 'wiki' / f'{page}.md').read_text()
        imports = dict(re.findall(r"import (\w+) from '([^']+\.json)'", source))
        source = re.sub(r'<script setup>.*?</script>', '', source, flags=re.S)
        title = re.search(r'^# (.+)$', source, re.M).group(1)
        blocks = []
        for line in source.splitlines():
            if line.startswith('# '):
                continue
            heading = re.match(r'^(#{2,3}) (.+?)(?: \{#([^}]+)\})?$', line)
            if heading:
                label = heading[2]
                anchor = heading[3] or re.sub(r'[^\w\u4e00-\u9fff -]', '', label).strip().replace(' ', '-').lower()
                anchors[f'{page}#{anchor}'] = len(blocks)
                blocks.append(dict(type='heading', text=label, anchor=anchor))
            elif line.startswith('<table'):
                effect = 'thunder_guaba' if 'data-effect="thunder_guaba"' in line else 'guaba'
                blocks.append(dict(type='potion_stats', id=effect))
            elif line.startswith(('<thead', '<tbody', '</table')):
                continue
            elif any(f'<{component} ' in line for component in ('CraftingRecipe', 'GuabaConversion', 'GuabaCharging', 'SmeltingRecipe')):
                variable = re.search(r':recipe="(\w+)"', line).group(1)
                path = (root / 'wiki' / imports[variable]).resolve()
                relative = path.relative_to(root / 'versions/1.20.1/src/main/resources/data')
                namespace, _, *parts = relative.parts
                blocks.append(dict(type='recipe', id=namespace + ':' + '/'.join(parts).removesuffix('.json')))
            elif line.strip() in ('<GuabaBrewing />', '<GuabaBrewing thunder />'):
                effect = 'thunder_guaba' if ' thunder ' in line else 'guaba'
                blocks.append(dict(type='brewing', id=effect))
            elif re.match(r'!\[.*\]\((.*)\)', line):
                path = re.match(r'!\[.*\]\((.*)\)', line)[1]
                if path == '/images/thunder_guaba_conversion.svg':
                    blocks.extend([dict(type='item', id='pinkkicore:guaba'),
                                   dict(type='text', text='掉在地上的瓜巴遭受雷击 → 等量雷霆瓜巴'),
                                   dict(type='item', id='pinkkicore:thunder_guaba')])
                    continue
                if path not in ITEM_IMAGES:
                    raise ValueError(f'{page}: add a native item/image mapping for {path}')
                blocks.append(dict(type='item', id=ITEM_IMAGES[path]))
            elif line.startswith('```') or re.match(r'^\|[\s:|-]+\|$', line):
                continue
            elif line.startswith('|'):
                blocks.append(dict(type='text', text=' · '.join(c.strip() for c in line.strip('|').split('|'))))
            elif line.strip():
                if '<' in line:
                    raise ValueError(f'{page}: unsupported guide markup: {line}')
                blocks.append(dict(type='text', text=line.lstrip('> ')))
        entries.append(dict(id=page, title=title, blocks=blocks))
    ids = {e['id'] for e in entries}
    for entry in entries:
        for block in entry['blocks']:
            for target in re.findall(r'\[[^]]+\]\(([^)]+)\)', block.get('text', '')):
                page, _, anchor = target.partition('#')
                page = page.removeprefix('./').removesuffix('.md') or entry['id']
                if page not in ids or anchor and f'{page}#{anchor}' not in anchors:
                    raise ValueError(f'Unresolved guide link: {entry["id"]} -> {target}')
    return dict(version=1, entries=entries)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    book = compile_book()
    destination = args.output / 'assets/pinkkicore/guide/zh_cn.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(book, ensure_ascii=False, indent=2) + '\n')
    print(f'Guide compiled: {len(book["entries"])} chapters -> {destination}')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Compile the supported player-Wiki Markdown subset into bundled offline guide resources."""
import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
LEGACY_PAGES = ('handbook', 'items', 'guaba-workbench', 'guaba-potions', 'recipes', 'compatibility', 'bosses')
ITEM_IMAGES = {'/images/guaba_charging_station.png': 'pinkkicore:guaba_charging_station', '/images/charged_creeper_spawn_egg.png': 'pinkkicore:charged_creeper_spawn_egg', '/images/thunder_guaba.png': 'pinkkicore:thunder_guaba', '/images/guaba_bow.png': 'pinkkicore:guaba_bow', '/images/guaba_sword.png': 'pinkkicore:guaba_sword', '/images/guaba.png': 'pinkkicore:guaba',
               '/images/guaba_workbench.svg': 'pinkkicore:guaba_workbench'}
ITEM_IMAGES.update({f'/images/{name}.png': f'pinkkicore:{name}' for name in
                    ('guaba_hood', 'pink_blessing', 'pink_ore', 'deepslate_pink_ore', 'raw_pink', 'pink_ingot', 'pink_nugget', 'pink_block', 'raw_pink_block')})


ITEM_IMAGES.update({
    f'/images/{name}.png': f'pinkkicore:{name}'
    for name in ('guaba_bed', 'cloud_dream', 'guaba_cloud_dream', 'nether_dream', 'guaba_nether_dream', 'zhang_fav', 'galgame_arcade')
})
ITEM_IMAGES.update({
    f'/images/{prefix}guaba_dragon_egg.png': f'pinkkicore:{prefix}guaba_dragon_egg'
    for prefix in ('', 'pink_', 'thunder_', 'cloud_', 'nether_')
})


def read_pages(root, minecraft_version, primary):
    root = root.resolve()
    pages = {}
    for page in (*LEGACY_PAGES, 'guide', 'galgame') if primary else LEGACY_PAGES:
        source = (root / 'wiki' / f'{page}.md').read_text()
        source = re.sub(r'<!-- guide-version: ([\d.]+) -->\n(.*?)<!-- /guide-version -->\n?',
                        lambda match: match[2] if minecraft_version == match[1] else '', source, flags=re.S)
        source = re.sub(r'<!-- wiki-only -->\n.*?<!-- /wiki-only -->\n?', '', source, flags=re.S)
        imports = dict(re.findall(r"import (\w+) from '([^']+\.json)'", source))
        source = re.sub(r'<script setup>.*?</script>', '', source, flags=re.S)
        title = re.search(r'^# (.+)$', source, re.M).group(1)
        blocks = []
        required_mods = []
        for line in source.splitlines():
            if line.startswith('<!-- guide-mod: ') and line.endswith(' -->'):
                if required_mods:
                    raise ValueError(f'{page}: nested guide-mod')
                required_mods = line.removeprefix('<!-- guide-mod: ').removesuffix(' -->').split(',')
                continue
            if line == '<!-- /guide-mod -->':
                if not required_mods:
                    raise ValueError(f'{page}: unexpected end of guide-mod')
                required_mods = []
                continue
            if line.startswith('# '):
                continue
            start = len(blocks)
            heading = re.match(r'^(#{2,3}) (.+?)(?: \{#([^}]+)\})?$', line)
            if heading:
                label = heading[2]
                anchor = heading[3] or re.sub(r'[^\w\u4e00-\u9fff -]', '', label).strip().replace(' ', '-').lower()
                blocks.append(dict(type='heading', text=label, anchor=anchor, level=len(heading[1])))
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
            elif line.startswith('<!-- guide-summary: ') and line.endswith(' -->'):
                if primary:
                    blocks.append(dict(type='text', text=line.removeprefix('<!-- guide-summary: ').removesuffix(' -->')))
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
            if primary and required_mods:
                for index in range(start, len(blocks)):
                    blocks[index]['requiredMods'] = required_mods
        if required_mods:
            raise ValueError(f'{page}: unclosed guide-mod')
        pages[page] = dict(id=page, title=title, blocks=blocks)
    return pages


def player_chapters(pages):
    entries = []
    routes = {'guide': 'handbook', 'handbook': 'handbook#reading', 'recipes#guide-book': 'handbook#guide-book'}

    def part(page, anchor='', *, children=True, heading=False, types=None):
        blocks = pages[page]['blocks']
        if anchor:
            start = next((i for i, block in enumerate(blocks) if block.get('anchor') == anchor), None)
            if start is None:
                raise ValueError(f'Missing guide source section: {page}#{anchor}')
            level = blocks[start]['level'] if children else 3
            end = next((i for i in range(start + 1, len(blocks)) if blocks[i].get('level', 4) <= level), len(blocks))
            selected = blocks[start if heading else start + 1:end]
        else:
            end = len(blocks) if children else next((i for i, block in enumerate(blocks) if 'anchor' in block), len(blocks))
            selected = blocks[:end]
        return page, anchor, [block.copy() for block in selected if types is None or block['type'] in types]

    def recipe(page_anchor):
        return part('recipes', page_anchor, types={'recipe'})

    def chapter(page_id, title):
        entries.append(dict(id=page_id, title=title, blocks=[]))

    def add(anchor, title, *parts, toc=None, required_mods=()):
        entry = entries[-1]
        destination = entry['id'] + ('#' + anchor if anchor else '')
        blocks = entry['blocks']
        if anchor:
            header = dict(type='heading', anchor=anchor, text=title)
            if toc is not False:
                header['toc'] = toc or title
            if required_mods:
                header['requiredMods'] = list(required_mods)
            blocks.append(header)
        for page, source, selected in parts:
            if page and (source or page not in routes):
                routes[page + ('#' + source if source else '')] = destination
            for block in selected:
                block['sourcePage'] = page
                block['section'] = destination
                if child_anchor := block.get('anchor'):
                    routes[f'{page}#{child_anchor}'] = f'{entry["id"]}#{child_anchor}'
                blocks.append(block)

    equipment_mods = ('silentgear', 'tconstruct', 'tetra')
    chapter('handbook', '序章')
    add('', '', part('guide', children=False))
    add('guide-book', '瓜巴手册', part('guide', 'guide-book'))
    add('reading', '阅读', part('handbook', 'reading'), part('handbook', '内容'))

    chapter('materials', '材料')
    add('guaba', '瓜巴', part('items', 'guaba'), recipe('guaba'), part('compatibility', 'guaba'))
    add('thunder-guaba', '雷霆瓜巴', part('items', 'thunder-guaba'), part('compatibility', 'thunder-guaba'))
    add('pink-ore', '粉红矿', part('items', 'pink-ore', children=False))
    add('cloud-dream', '云梦与瓜巴云梦', part('items', 'cloud-dream'), recipe('guaba-cloud-dream'))
    add('nether-dream', '冥梦与瓜巴冥梦', part('items', 'nether-dream'), recipe('guaba-nether-dream'))
    add('dream-materials', '梦系材料联动', part('compatibility', 'dream-materials'), required_mods=equipment_mods)
    add('guaba-bed', '瓜巴床', part('items', 'guaba-bed'), recipe('guaba-bed'))
    add('zhang-fav', '张哥之爱', part('galgame', 'zhang-fav'))
    add('galgame-soul', '旮旯之魂', part('galgame', 'galgame-soul'))

    chapter('work-blocks', '工作方块')
    add('guaba-workbench', '瓜巴工作台', part('guaba-workbench', children=False),
        recipe('guaba-workbench'), recipe('guaba-conversion'), part('guaba-workbench', '游戏内配方预览', heading=True),
        part('guaba-workbench', '获取与放置', heading=True), part('guaba-workbench', '红石控制', heading=True),
        part('guaba-workbench', '六面输入输出', heading=True))
    add('guaba-charging-station', '瓜巴充电桩', part('items', 'guaba-charging-station'),
        recipe('guaba-charging-station'), recipe('guaba-charging'))
    add('sword-in-stone', '石中剑', part('galgame', 'sword-in-stone'))
    add('galgame-arcade', '旮旯游戏机', part('galgame', 'galgame-arcade'))
    add('irons-charging', '铁魔法充能', part('recipes', 'irons-charging'), toc=False, required_mods=('irons_spellbooks',))

    chapter('weapons', '基础武器')
    add('guaba-sword', '瓜巴剑', part('items', 'guaba-sword'), recipe('guaba-sword'))
    add('guaba-bow', '瓜巴弓', part('items', 'guaba-bow'), recipe('guaba-bow'))
    add('accessories', '饰品')
    add('guaba-hood', '瓜巴头套', part('items', 'guaba-hood'), recipe('guaba-hood'), toc=False)
    add('pink-blessing', '粉红之祝', part('items', 'pink-blessing'), recipe('pink-blessing'), toc=False)

    add('favorite-person', '最爱之人', part('galgame', 'favorite-person'))
    add('galgame-heart', '旮旯之心', part('galgame', 'galgame-heart'))

    chapter('guaba-potions', '药水效果')
    add('guaba', '瓜巴药水', part('guaba-potions', children=False), part('guaba-potions', '酿造', heading=True),
        part('guaba-potions', '喷溅与滞留', heading=True))
    add('thunder-guaba', '雷霆瓜巴药水', part('guaba-potions', 'thunder-guaba'))

    add('galgame', '旮旯', part('galgame', 'galgame'))

    chapter('creatures', '生物')
    add('zhang', '张小弟与张哥', part('galgame', 'zhang'))
    add('guaba-dragon', '朱罗纪瓜巴龙', part('bosses', 'guaba-dragon'),
        part('items', 'guaba-dragon-spawn-egg'), part('bosses', 'pet-guaba-dragons', heading=True),
        part('items', 'guaba-dragon-eggs', heading=True), part('recipes', 'guaba-dragon-eggs'), toc='瓜巴龙与变种')
    for anchor, title in (('pinkki', '粉红妹'), ('guaba-yingrong', '瓜巴影融'), ('bingwa', '饼蛙')):
        add(anchor, title, part('bosses', anchor), part('items', anchor + '-spawn-egg'))
    add('charged-creeper-spawn-egg', '闪电苦力怕', part('items', 'charged-creeper-spawn-egg'))
    add('factions', '阵营与特殊敌对', part('bosses', 'factions'), part('bosses', '头顶名称', heading=True))
    add('wildlife', '野外遇见与掉落', part('bosses', 'wildlife'))

    for entry in entries:
        for block in entry['blocks']:
            source = block.pop('sourcePage', '')
            section = block.pop('section', '')
            if 'text' not in block:
                continue

            def move_link(match):
                label, target = match.groups()
                page, sep, anchor = target.partition('#')
                key = (page.removeprefix('./').removesuffix('.md') or source) + sep + anchor
                if key not in routes:
                    raise ValueError(f'Unresolved guide link: {source} -> {target}')
                if routes[key] == section:
                    return label
                target_page, sep, target_anchor = routes[key].partition('#')
                return f'[{label}](./{target_page}.md{sep}{target_anchor})'

            block['text'] = re.sub(r'\[([^]]+)\]\(([^)]+)\)', move_link, block['text'])
    return entries


def compile_book(root=ROOT, minecraft_version='1.20.1'):
    primary = minecraft_version == '1.20.1'
    pages = read_pages(root, minecraft_version, primary)
    entries = player_chapters(pages) if primary else list(pages.values())
    ids = {e['id'] for e in entries}
    anchors = set()
    for entry in entries:
        for block in entry['blocks']:
            block.pop('level', None)
            if anchor := block.get('anchor'):
                key = (entry['id'], anchor)
                if key in anchors:
                    raise ValueError(f'Duplicate guide anchor: {entry["id"]}#{anchor}')
                anchors.add(key)
    for entry in entries:
        for block in entry['blocks']:
            for target in re.findall(r'\[[^]]+\]\(([^)]+)\)', block.get('text', '')):
                page, _, anchor = target.partition('#')
                page = page.removeprefix('./').removesuffix('.md') or entry['id']
                if page not in ids or anchor and (page, anchor) not in anchors:
                    raise ValueError(f'Unresolved guide link: {entry["id"]} -> {target}')
    return dict(version=1, entries=entries)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--minecraft-version', default='1.20.1')
    args = parser.parse_args()
    book = compile_book(minecraft_version=args.minecraft_version)
    destination = args.output / 'assets/pinkkicore/guide/zh_cn.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(book, ensure_ascii=False, indent=2) + '\n')
    print(f'Guide compiled: {len(book["entries"])} chapters -> {destination}')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Check the shipped Pink Ore resources; optionally compare generation against a local vanilla JAR."""
import argparse
import json
from pathlib import Path
import struct
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def verify(archive, vanilla=None):
    def read(path):
        return json.loads(archive.read(path))

    blocks = ('pink_ore', 'deepslate_pink_ore', 'pink_block', 'raw_pink_block')
    items = ('raw_pink', 'pink_ingot', 'pink_nugget')
    assert 'io/github/vitech/pinkkicore/forge/ore/PinkOreContent.class' in archive.namelist()
    for name in blocks + items:
        kind = 'block' if name in blocks else 'item'
        path = f'assets/pinkkicore/textures/{kind}/{name}.png'
        texture = archive.read(path)
        assert struct.unpack('>II', texture[16:24]) == (16, 16)
        assert texture == (ROOT / f'wiki/public/images/{name}.png').read_bytes()
        assert texture == (ROOT / 'versions/1.20.1/src/main/resources' / path).read_bytes()
        model = read(f'assets/pinkkicore/models/item/{name}.json')
        if kind == 'item':
            assert model['textures']['layer0'] == f'pinkkicore:item/{name}'
        else:
            assert model['parent'] == f'pinkkicore:block/{name}'
            assert read(f'assets/pinkkicore/models/block/{name}.json')['textures']['all'] == f'pinkkicore:block/{name}'
            assert read(f'assets/pinkkicore/blockstates/{name}.json')['variants']['']['model'] == f'pinkkicore:block/{name}'
            read(f'data/pinkkicore/loot_tables/blocks/{name}.json')
        for language in ('zh_cn', 'en_us'):
            assert read(f'assets/pinkkicore/lang/{language}.json')[f'{kind}.pinkkicore.{name}']
    for suffix, size, air, frequency in (('', 4, 0.5, 9), ('_large', 12, 0.7, 7), ('_buried', 8, 1.0, 5)):
        config = read(f'data/pinkkicore/worldgen/configured_feature/ore_pink{suffix}.json')
        placed = read(f'data/pinkkicore/worldgen/placed_feature/ore_pink{suffix}.json')
        assert config['type'] == 'minecraft:ore'
        assert config['config']['size'] == size and config['config']['discard_chance_on_air_exposure'] == air
        assert [t['state']['Name'] for t in config['config']['targets']] == [f'pinkkicore:{b}' for b in blocks[:2]]
        assert placed['feature'] == f'pinkkicore:ore_pink{suffix}'
        assert placed['placement'][0] == ({'type': 'minecraft:rarity_filter', 'chance': frequency} if suffix == '_large'
                                          else {'type': 'minecraft:count', 'count': frequency})
        if vanilla:
            vanilla_name = f'ore_diamond{suffix}'
            vanilla_config = json.loads(vanilla.read(f'data/minecraft/worldgen/configured_feature/{vanilla_name if suffix else "ore_diamond_small"}.json'))
            normalized = json.loads(json.dumps(config).replace('pinkkicore:pink_ore', 'minecraft:diamond_ore')
                                    .replace('pinkkicore:deepslate_pink_ore', 'minecraft:deepslate_diamond_ore'))
            assert normalized == vanilla_config, 'Ore shape/replacement/exposure drifted from vanilla'
            vanilla_placed = json.loads(vanilla.read(f'data/minecraft/worldgen/placed_feature/{vanilla_name}.json'))
            assert placed['placement'][1:] == vanilla_placed['placement'][1:], 'Height/biome/square placement drifted'
    modifier = read('data/pinkkicore/forge/biome_modifier/add_pink_ore.json')
    assert modifier == {'type': 'forge:add_features', 'biomes': '#minecraft:is_overworld',
                        'features': ['pinkkicore:ore_pink', 'pinkkicore:ore_pink_large', 'pinkkicore:ore_pink_buried'], 'step': 'underground_ores'}
    for name in ('mineable/pickaxe', 'needs_iron_tool'):
        assert read(f'data/minecraft/tags/blocks/{name}.json') == {'replace': False, 'values': [f'pinkkicore:{b}' for b in blocks]}
    assert read('data/pinkkicore/tags/items/materials/tier_1.json')['values'] == ['pinkkicore:guaba', 'pinkkicore:pink_ingot']
    recipe_names = []
    for source in ('raw_pink', 'pink_ore', 'deepslate_pink_ore'):
        for method, time in (('smelting', 200), ('blasting', 100)):
            name = f'pink_ingot_from_{method}_{source}'
            recipe = read(f'data/pinkkicore/recipes/{name}.json')
            assert recipe['type'] == f'minecraft:{method}' and recipe['ingredient'] == {'item': f'pinkkicore:{source}'}
            assert recipe['result'] == 'pinkkicore:pink_ingot' and recipe['experience'] == 1 and recipe['cookingtime'] == time
            recipe_names.append(name)
    for large, small in (('pink_block', 'pink_ingot'), ('raw_pink_block', 'raw_pink'), ('pink_ingot', 'pink_nugget')):
        name = f'{large}_from_{small}'
        recipe = read(f'data/pinkkicore/recipes/{name}.json')
        assert recipe['pattern'] == ['###'] * 3 and recipe['key'] == {'#': {'item': f'pinkkicore:{small}'}}
        assert recipe['result'] == {'item': f'pinkkicore:{large}'}
        recipe_names.append(name)
        name = f'{small}_from_{large}'
        recipe = read(f'data/pinkkicore/recipes/{name}.json')
        assert recipe['ingredients'] == [{'item': f'pinkkicore:{large}'}] and recipe['result'] == {'item': f'pinkkicore:{small}', 'count': 9}
        recipe_names.append(name)
    guide = read('assets/pinkkicore/guide/zh_cn.json')
    guide_ids = {block.get('id') for entry in guide['entries'] for block in entry['blocks'] if block['type'] == 'recipe'}
    for name in recipe_names:
        assert f'pinkkicore:{name}' in guide_ids, f'Missing offline recipe diagram {name}'
        assert read(f'data/pinkkicore/advancements/recipes/misc/{name}.json')['rewards']['recipes'] == [f'pinkkicore:{name}']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--vanilla', type=Path)
    args = parser.parse_args()
    props = dict(line.split('=', 1) for line in (ROOT / 'mod.properties').read_text().splitlines() if '=' in line)
    jar = ROOT / f"versions/1.20.1/build/libs/pinkkicore-forge-1.20.1-{props['mod_version']}.jar"
    with zipfile.ZipFile(jar) as archive:
        if args.vanilla:
            with zipfile.ZipFile(args.vanilla) as vanilla:
                verify(archive, vanilla)
        else:
            verify(archive)
    print('Pink Ore resources PASS: generation, textures, tags, 12 recipes, unlocks and offline diagrams')


if __name__ == '__main__':
    main()

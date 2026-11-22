import importlib.util
from pathlib import Path
import re
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('guide_generator', ROOT / 'scripts/generate-guide.py')
guide = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guide)


class GuideContentTest(unittest.TestCase):
    def test_current_player_pages_compile_with_native_recipe_blocks(self):
        book = guide.compile_book()
        entries = {entry['id']: entry for entry in book['entries']}
        self.assertEqual([entry['title'] for entry in entries.values()], ['序章', '材料', '工作方块', '基础武器', '药水效果', '生物'])
        self.assertIn('pinkkicore:guide_book', recipe_ids(entries['handbook']))
        self.assertIn('pinkkicore:guaba_conversion/dirt_to_mud', recipe_ids(entries['work-blocks']))
        self.assertEqual([b['toc'] for b in entries['work-blocks']['blocks'] if 'toc' in b], ['瓜巴工作台', '瓜巴充能台'])
        self.assertEqual([b['toc'] for b in entries['creatures']['blocks'] if 'toc' in b][:2], ['瓜巴龙与变种', '粉红妹'])
        self.assertEqual([b['toc'] for b in entries['weapons']['blocks'] if 'toc' in b], ['瓜巴剑', '瓜巴弓', '饰品'])
        self.assertTrue(any(b['type'] == 'brewing' for b in entries['guaba-potions']['blocks']))
        self.assertTrue(any(b['type'] == 'potion_stats' for b in entries['guaba-potions']['blocks']))
        for block_type in ('brewing', 'potion_stats'):
            self.assertEqual({b['id'] for b in entries['guaba-potions']['blocks'] if b['type'] == block_type}, {'guaba', 'thunder_guaba'})
        self.assertIn('雷击强度和间隔不随等级变化', str(entries['guaba-potions']))

    def test_versions_and_pink_derivative_recipes_stay_on_wiki(self):
        book = guide.compile_book()
        content = str(book)
        self.assertNotIn('GeckoLib', content)
        self.assertNotIn('15.2.0.27', content)
        self.assertIn('粉红粒和储存块可与锭或粗锭互相拆合', content)
        for item in ('pink_ore', 'pink_nugget', 'pink_block', 'raw_pink', 'deepslate_pink_ore'):
            self.assertNotIn(f"'id': 'pinkkicore:{item}'", content)
        materials = next(entry for entry in book['entries'] if entry['id'] == 'materials')
        self.assertFalse(any('pink' in recipe.partition(':')[2] for recipe in recipe_ids(materials)))
        self.assertIn('粉红材料压缩与拆分', (ROOT / 'wiki/recipes.md').read_text())

    def test_installed_mods_keep_their_own_material_text_and_visible_links(self):
        cases = [(), ('silentgear',), ('tconstruct',), ('tetra',), ('irons_spellbooks',),
                 ('silentgear', 'tconstruct', 'tetra', 'irons_spellbooks')]
        hosts = {'silentgear': '寂静装备', 'tconstruct': '匠魂', 'tetra': 'Tetra'}
        for installed in cases:
            with self.subTest(installed=installed):
                entries = {entry['id']: [b for b in entry['blocks'] if not b.get('requiredMods')
                           or set(b['requiredMods']).intersection(installed)] for entry in guide.compile_book()['entries']}
                for mod, text in hosts.items():
                    self.assertEqual(text in str(entries['materials']), mod in installed)
                self.assertEqual(any(b.get('anchor') == 'dream-materials' for b in entries['materials']),
                                 bool(set(hosts).intersection(installed)))
                self.assertEqual('lightning_bottle' in str(entries['work-blocks']), 'irons_spellbooks' in installed)
                for blocks in entries.values():
                    for block in blocks:
                        for page, anchor in re.findall(r'\(\./([\w-]+)\.md(?:#([^)]*))?\)', block.get('text', '')):
                            self.assertIn(page, entries)
                            if anchor:
                                self.assertTrue(any(b.get('anchor') == anchor for b in entries[page]), f'{page}#{anchor}')

    def test_dragon_recipes_and_variant_icons_only_ship_in_primary_guide(self):
        for minecraft in ('1.20.1', '1.19.1', '1.21.1'):
            with self.subTest(minecraft=minecraft):
                book = guide.compile_book(minecraft_version=minecraft)
                blocks = [block for entry in book['entries'] for block in entry['blocks']]
                species = [block for block in blocks if block.get('anchor') == 'pet-guaba-dragons']
                icons = [block for block in blocks if block['type'] == 'item' and block['id'].endswith('guaba_dragon_egg')]
                recipes = [block for block in blocks if block['type'] == 'recipe' and 'guaba_dragon_egg' in block['id']]
                self.assertEqual((len(species), len(icons), len(recipes)), (1, 5, 8) if minecraft == '1.20.1' else (0, 0, 0))

    def temporary_wiki(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        shutil.copytree(ROOT / 'wiki', root / 'wiki', ignore=shutil.ignore_patterns('.vitepress', 'public'))
        # Recipe imports are references only; no Minecraft runtime is needed by this compiler.
        return root

    def test_wiki_edit_updates_bundled_prose(self):
        root = self.temporary_wiki()
        page = root / 'wiki/bosses.md'
        page.write_text(page.read_text() + '\n中文同步检查：新操作说明。\n')
        book = guide.compile_book(root)
        self.assertIn('新操作说明', str(book))

    def test_broken_links_fail_the_build(self):
        root = self.temporary_wiki()
        page = root / 'wiki/handbook.md'
        page.write_text(page.read_text() + '\n[坏链接](./recipes.md#missing-anchor)\n')
        with self.assertRaisesRegex(ValueError, 'Unresolved guide link'):
            guide.compile_book(root)

    def test_unsupported_widgets_fail_instead_of_silently_losing_content(self):
        root = self.temporary_wiki()
        page = root / 'wiki/items.md'
        page.write_text(page.read_text() + '\n<UnknownWidget />\n')
        with self.assertRaisesRegex(ValueError, 'unsupported guide markup'):
            guide.compile_book(root)


def recipe_ids(entry):
    return [block['id'] for block in entry['blocks'] if block['type'] == 'recipe']

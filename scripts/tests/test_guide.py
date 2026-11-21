import importlib.util
from pathlib import Path
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
        self.assertEqual(set(entries), set(guide.PAGES))
        ids = [b['id'] for b in entries['recipes']['blocks'] if b['type'] == 'recipe']
        self.assertIn('pinkkicore:guide_book', ids)
        self.assertIn('pinkkicore:guaba_conversion/dirt_to_mud', ids)
        self.assertTrue(any(b['type'] == 'brewing' for b in entries['guaba-potions']['blocks']))
        self.assertTrue(any(b['type'] == 'potion_stats' for b in entries['guaba-potions']['blocks']))
        for block_type in ('brewing', 'potion_stats'):
            self.assertEqual({b['id'] for b in entries['guaba-potions']['blocks'] if b['type'] == block_type}, {'guaba', 'thunder_guaba'})
        self.assertIn('雷击强度和间隔不随等级变化', str(entries['guaba-potions']))


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
        page = root / 'wiki/items.md'
        page.write_text(page.read_text() + '\n[坏链接](./recipes.md#missing-anchor)\n')
        with self.assertRaisesRegex(ValueError, 'Unresolved guide link'):
            guide.compile_book(root)

    def test_unsupported_widgets_fail_instead_of_silently_losing_content(self):
        root = self.temporary_wiki()
        page = root / 'wiki/items.md'
        page.write_text(page.read_text() + '\n<UnknownWidget />\n')
        with self.assertRaisesRegex(ValueError, 'unsupported guide markup'):
            guide.compile_book(root)

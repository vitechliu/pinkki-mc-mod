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

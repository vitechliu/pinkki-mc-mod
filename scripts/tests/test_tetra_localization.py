"""Validate the naming keys Tetra derives from our actual material identifiers."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / 'versions/1.20.1/src/main/resources'


class TetraMaterialLocalizationTest(unittest.TestCase):
    def test_material_names_prefixes_and_categories_exist_in_both_languages(self):
        materials = [json.loads(path.read_text()) for path in
                     (RESOURCES / 'data/tetra/materials').rglob('pinkkicore_*.json')]
        self.assertTrue(materials)
        for language in ('zh_cn', 'en_us'):
            strings = json.loads((RESOURCES / f'assets/pinkkicore/lang/{language}.json').read_text())
            for material in materials:
                # ItemModule.getItemPrefix derives this separate key from the variant material.
                for key in (f"tetra.material.{material['key']}",
                            f"tetra.material.{material['key']}.prefix",
                            f"tetra.variant_category.{material['category']}.label"):
                    with self.subTest(language=language, key=key):
                        self.assertTrue(key in strings, f'Missing Tetra naming translation: {key}')
                        self.assertTrue(strings.get(key, '').strip())
                        self.assertNotEqual(strings.get(key), key)


if __name__ == '__main__':
    unittest.main()

import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('dependency_baselines', Path(__file__).resolve().parents[1] / 'check-dependency-baselines.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

class DependencyBaselineTests(unittest.TestCase):
    def test_numeric_maven_bounds_and_build_metadata(self):
        self.assertTrue(audit.contains('[9.2.2,10)', '9.2.2+1.21.1'))
        self.assertFalse(audit.contains('[9.2.2,10)', '9.2.1+1.21.1'))
        self.assertFalse(audit.contains('[9.2.2,10)', '10.0.0'))
        self.assertTrue(audit.contains('[21.1.72,21.2)', '21.1.249'))
        with self.assertRaises(ValueError):
            audit.contains('[9,10)', '9.2.2-beta.1')

    def test_1192_is_never_used_as_1191(self):
        result = audit.inspect({'id': 'wrong-game', 'minecraft': '1.19.2', 'loader': 'forge', 'mods': {}}, {})
        self.assertEqual('excluded', result['status'])

    def test_fork_loader_and_known_bad_manifest_are_excluded(self):
        for sample in [dict(loader='neoforge'), dict(loader='forge', excluded_reason='duplicate mod ID')]:
            result = audit.inspect({'id': 'bad', 'minecraft': '1.20.1', 'mods': {}, **sample}, {})
            self.assertEqual('excluded', result['status'])

    def test_absent_optional_mod_does_not_become_a_dependency_conflict(self):
        sample = {'id': 'minimal', 'minecraft': '1.20.1', 'loader': 'forge', 'loader_version': '47.2.0', 'mods': {}}
        config = {'forge_min_version': '47.1.0', 'curios_version_range': '[5.3.5,6)'}
        result = audit.inspect(sample, config)
        self.assertEqual('accepted', result['status'])
        self.assertTrue(result['needs_geckolib'])
        sample['mods']['curios'] = '5.3.4'
        self.assertEqual('conflict', audit.inspect(sample, config)['status'])

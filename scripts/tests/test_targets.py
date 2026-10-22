"""纯文件/状态验证：不启动 Gradle、Minecraft 或远端工作流。"""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("targets", ROOT / "scripts/targets.py")
targets = importlib.util.module_from_spec(spec)
spec.loader.exec_module(targets)


class TargetWorkflowTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.patches = [patch.object(targets, "ROOT", self.root),
                        patch.object(targets, "REGISTRY", self.root / "config/targets.json"),
                        patch.object(targets, "STATE", self.root / "docs/porting/status.json")]
        for item in self.patches:
            item.start()
        targets.REGISTRY.parent.mkdir(parents=True)
        targets.REGISTRY.write_text((ROOT / "config/minecraft-targets.json").read_text())
        self.write("common/src/main/java/Rule.java", "class Rule {}\n")
        self.write("versions/1.20.1/src/main/java/Adapter.java", "class Adapter {}\n")
        self.write("mod.properties", "mod_version=0.1.0\n")
        self.write("evidence.log", "Fixture proof; no actual game execution.\n")
        self.ref = patch.object(targets.subprocess, "check_output", return_value="baseline-ref\n")
        self.ref.start()
        for version in ["1.21.1", "1.19.1"]:
            with contextlib.redirect_stdout(io.StringIO()):
                targets.checkpoint(version, "evidence.log")

    def tearDown(self):
        self.ref.stop()
        for item in reversed(self.patches):
            item.stop()
        self.temporary.cleanup()

    def write(self, path, content):
        destination = self.root / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(content)

    def cli(self, *args):
        with patch("sys.argv", ["targets.py", *args]), contextlib.redirect_stdout(io.StringIO()) as output:
            targets.main()
        return output.getvalue()

    def test_default_build_selects_only_primary(self):
        rows = json.loads(self.cli("matrix"))["include"]
        self.assertEqual([row["minecraft"] for row in rows], ["1.20.1"])
        self.assertEqual(len(json.loads(self.cli("matrix", "--targets", "all"))["include"]), 3)

    def test_unknown_empty_and_duplicate_selection_fails(self):
        for value in ["", "1.19.2", "1.20.1,1.20.1", "1.20.1,"]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                targets.select(value)

    def test_unlogged_new_source_invalidates_secondary_release(self):
        self.write("common/src/main/java/NewFeature.java", "class NewFeature {}\n")
        self.cli("status", "--targets", "1.20.1", "--check")
        with self.assertRaises(SystemExit):
            self.cli("status", "--targets", "1.21.1", "--check")
        with contextlib.redirect_stdout(io.StringIO()):
            targets.checkpoint("1.21.1", "evidence.log")
        self.cli("status", "--targets", "1.21.1", "--check")
        with self.assertRaises(SystemExit):
            self.cli("status", "--targets", "1.19.1", "--check")

    def test_target_change_invalidates_only_that_target(self):
        self.write("versions/1.19.1/src/main/java/Port.java", "class Port {}\n")
        self.cli("status", "--targets", "1.21.1", "--check")
        with self.assertRaises(SystemExit):
            self.cli("status", "--targets", "1.19.1", "--check")

    def test_document_changes_do_not_require_game_revalidation(self):
        self.write("docs/new-guide.md", "说明\n")
        self.cli("status", "--targets", "all", "--check")

    def test_crlf_does_not_make_cross_platform_checkpoint_stale(self):
        before = targets.source_fingerprint()
        (self.root / "common/src/main/java/Rule.java").write_bytes(b"class Rule {}\r\n")
        self.assertEqual(targets.source_fingerprint(), before)

    def test_missing_evidence_cannot_advance_checkpoint(self):
        before = targets.STATE.read_bytes()
        with self.assertRaises(ValueError):
            targets.checkpoint("1.21.1", "missing.log")
        self.assertEqual(targets.STATE.read_bytes(), before)


if __name__ == "__main__":
    unittest.main()

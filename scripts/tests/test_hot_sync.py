"""受管资源包文件测试，不启动游戏、不读写真实实例和存档。"""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("hot_sync", Path(__file__).parents[1] / "hot-sync.py")
hot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hot)


class HotSyncTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.local = self.root / ".local/hmcl"
        self.game = self.local / ".minecraft"
        for name, value in [("ROOT", self.root), ("LOCAL", self.local), ("GAME_ROOT", self.game)]:
            p = patch.object(hot.hmcl, name, value)
            p.start()
            self.addCleanup(p.stop)
        self.version = self.root / "versions/1.20.1"
        self.version.mkdir(parents=True)
        (self.version / "gradle.properties").write_text("pack_format=15\n")
        self.shared = self.root / "common/src/main/resources"
        self.source = self.version / "src/main/resources"
        self.write(self.shared / "assets/pinkkicore/lang/zh_cn.json", b'{"test":"shared"}')
        self.write(self.source / "assets/pinkkicore/lang/zh_cn.json", b'{"test":"version"}')
        self.write(self.source / "assets/pinkkicore/models/item/test.json", b'{}')
        self.write(self.source / "data/pinkkicore/recipes/test.json", b'{}')
        self.write(self.source / "data/minecraft/tags/items/test.json", b'{"values":[]}')
        self.targets = {}
        for profile in ["clean", "compat"]:
            target = self.game / "versions" / f"PinkkiCore-1.20.1-{profile}"
            self.write(target / "mods/pinkki.jar", b"do not replace")
            self.write(target / "saves/测试世界/level.dat", b"world untouched")
            self.targets[profile] = target
        p = patch.object(hot.deploy, "check_instance", side_effect=lambda profile: (self.targets[profile], {}))
        p.start()
        self.addCleanup(p.stop)
        stream = contextlib.redirect_stdout(io.StringIO())
        stream.__enter__()
        self.addCleanup(stream.__exit__, None, None, None)

    def write(self, path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def pack(self, profile="clean"):
        return self.targets[profile] / "resourcepacks" / hot.PACK_NAME

    def test_default_all_worlds_both_profiles_and_version_precedence(self):
        self.write(self.targets["clean"] / "saves/第二世界/level.dat", b"second world")
        hot.run("sync", ["clean", "compat"])
        for profile in self.targets:
            pack = self.pack(profile)
            self.assertEqual((pack / "assets/pinkkicore/lang/zh_cn.json").read_bytes(), b'{"test":"version"}')
            self.assertEqual(json.loads((pack / "pack.mcmeta").read_text())["pack"]["pack_format"], 15)
            self.assertFalse((pack / "data").exists())
            self.assertTrue((self.targets[profile] / "saves/测试世界/datapacks" / hot.PACK_NAME / "data/pinkkicore/recipes/test.json").is_file())
            self.assertEqual((self.targets[profile] / "mods/pinkki.jar").read_bytes(), b"do not replace")
            self.assertEqual((self.targets[profile] / "saves/测试世界/level.dat").read_bytes(), b"world untouched")
        self.assertTrue((self.targets["clean"] / "saves/第二世界/datapacks" / hot.PACK_NAME / "pack.mcmeta").is_file())

    def test_explicit_world_data_only_and_other_namespaces(self):
        hot.run("sync", ["clean"], "测试世界", True)
        world = self.targets["clean"] / "saves/测试世界"
        pack = world / "datapacks" / hot.PACK_NAME
        self.assertTrue((pack / "data/minecraft/tags/items/test.json").is_file())
        self.assertFalse(self.pack().exists())
        self.assertEqual((world / "level.dat").read_bytes(), b"world untouched")

    def test_explicit_world_must_be_single_instance_existing_child(self):
        for profiles, world, data_only in [(["clean", "compat"], "测试世界", False),
                (["clean"], "../escape", True), (["clean"], "", True),
                (["clean"], "missing", True)]:
            with self.assertRaises(ValueError):
                hot.run("sync", profiles, world, data_only)
        self.assertFalse(self.pack().exists())

    def test_resources_only_does_not_touch_worlds(self):
        hot.run("sync", ["clean", "compat"], resources_only=True)
        for target in self.targets.values():
            self.assertFalse((target / "saves/测试世界/datapacks").exists())
        with self.assertRaises(ValueError):
            hot.run("sync", ["clean"], "测试世界", resources_only=True)

    def test_data_only_all_worlds_and_new_world_discovery(self):
        hot.run("sync", ["clean", "compat"], data_only=True)
        self.assertFalse(self.pack().exists())
        new = self.targets["compat"] / "saves/新建世界"
        self.write(new / "level.dat", b"new world")
        receipt = hot.run("sync", ["clean", "compat"], data_only=True)
        self.assertEqual(receipt["world_scope"], "all")
        self.assertEqual(len(receipt["packs"]), 3)
        self.assertTrue((new / "datapacks" / hot.PACK_NAME / "pack.mcmeta").is_file())

    def test_no_world_or_linked_world_is_not_created_or_followed(self):
        import shutil
        for target in self.targets.values():
            shutil.rmtree(target / "saves")
        elsewhere = self.root / "other-install/world"
        self.write(elsewhere / "level.dat", b"outside target")
        saves = self.targets["clean"] / "saves"
        saves.mkdir()
        (saves / "linked").symlink_to(elsewhere, target_is_directory=True)
        (saves / "not-a-world").mkdir()
        receipt = hot.run("sync", ["clean", "compat"])
        self.assertEqual(len(receipt["packs"]), 2)
        self.assertFalse((elsewhere / "datapacks").exists())
        self.assertFalse((self.targets["compat"] / "saves").exists())

    def test_default_remove_all_world_packs_keeps_other_datapacks(self):
        hot.run("sync", ["clean", "compat"])
        other = self.targets["compat"] / "saves/测试世界/datapacks/other/pack.mcmeta"
        self.write(other, b"keep other pack")
        hot.run("remove", ["clean", "compat"])
        for target in self.targets.values():
            self.assertFalse((target / "resourcepacks" / hot.PACK_NAME).exists())
            self.assertFalse((target / "saves/测试世界/datapacks" / hot.PACK_NAME).exists())
            self.assertEqual((target / "saves/测试世界/level.dat").read_bytes(), b"world untouched")
        self.assertEqual(other.read_bytes(), b"keep other pack")

    def test_rename_deletes_stale_and_retains_backup(self):
        hot.run("sync", ["clean"])
        old = self.source / "assets/pinkkicore/models/item/test.json"
        old.rename(old.with_name("renamed.json"))
        receipt = hot.run("sync", ["clean"])
        self.assertFalse((self.pack() / "assets/pinkkicore/models/item/test.json").exists())
        self.assertTrue((self.pack() / "assets/pinkkicore/models/item/renamed.json").exists())
        self.assertIn("assets/pinkkicore/models/item/test.json", receipt["packs"][0]["deleted"])
        self.assertTrue(list((self.local / "hot-sync").glob("*/backups/0/assets/pinkkicore/models/item/test.json")))

    def test_invalid_json_aborts_before_any_pack_written(self):
        self.write(self.source / "data/pinkkicore/recipes/test.json", b'{invalid')
        with self.assertRaises(json.JSONDecodeError):
            hot.run("sync", ["clean"], "测试世界")
        self.assertFalse(self.pack().exists())

    def test_unmanaged_conflict_aborts_all_profiles(self):
        self.pack("compat").mkdir(parents=True)
        (self.pack("compat") / "user.txt").write_text("user")
        with self.assertRaisesRegex(ValueError, "拒绝接管"):
            hot.run("sync", ["clean", "compat"])
        self.assertFalse(self.pack().exists())
        self.assertEqual((self.pack("compat") / "user.txt").read_text(), "user")

    def test_manual_edits_not_overwritten_or_removed(self):
        hot.run("sync", ["clean"])
        path = self.pack() / "assets/pinkkicore/lang/zh_cn.json"
        path.write_text("user changed")
        for action in ["sync", "remove"]:
            with self.assertRaisesRegex(ValueError, "手动修改"):
                hot.run(action, ["clean"])
        self.assertEqual(path.read_text(), "user changed")

    def test_symlink_never_followed(self):
        resourcepacks = self.targets["clean"] / "resourcepacks"
        elsewhere = self.root / "elsewhere"
        elsewhere.mkdir()
        resourcepacks.symlink_to(elsewhere, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "符号链接"):
            hot.run("sync", ["clean"])
        self.assertEqual(list(elsewhere.iterdir()), [])

    def test_remove_only_owned_pack_and_preserve_world(self):
        hot.run("sync", ["clean"], "测试世界")
        other = self.targets["clean"] / "resourcepacks/other/pack.mcmeta"
        self.write(other, b"keep")
        hot.run("remove", ["clean"], "测试世界")
        self.assertFalse(self.pack().exists())
        self.assertEqual(other.read_bytes(), b"keep")
        self.assertEqual((self.targets["clean"] / "saves/测试世界/level.dat").read_bytes(), b"world untouched")

    def test_manifest_traversal_and_extra_file_rejected(self):
        hot.run("sync", ["clean"])
        manifest = self.pack() / hot.MANIFEST
        data = json.loads(manifest.read_text())
        data["files"]["../outside"] = "malicious"
        manifest.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, "非法"):
            hot.run("remove", ["clean"])
        del data["files"]["../outside"]
        manifest.write_text(json.dumps(data))
        (self.pack() / "user.txt").write_text("keep")
        with self.assertRaisesRegex(ValueError, "额外文件"):
            hot.run("remove", ["clean"])

    def test_status_does_not_write_packs(self):
        receipt = hot.run("status", ["clean", "compat"])
        self.assertFalse(self.pack().exists())
        self.assertFalse(receipt["packs"][0]["present"])

    def test_report_snapshots_overlay_manifest_without_world_copy(self):
        hot.run("sync", ["clean"], "测试世界")
        target = self.targets["clean"]
        (target / f"{target.name}.json").write_text("{}")
        hot.hmcl.report("1.20.1", "clean", "overlay test")
        report = next((self.local / "reports").glob("*/report.json"))
        data = json.loads(report.read_text())
        self.assertEqual(len(data["hot_sync_overlays"]), 2)
        self.assertFalse(list(report.parent.rglob("level.dat")))


if __name__ == "__main__":
    unittest.main()

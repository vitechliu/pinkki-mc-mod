"""文件级部署回归，不启动 Minecraft，不使用真实实例。"""
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

spec = importlib.util.spec_from_file_location("deploy_local", Path(__file__).parents[1] / "deploy-local.py")
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)


class DeployTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.local = self.root / ".local/hmcl"
        self.games = self.local / ".minecraft"
        for name, value in [("ROOT", self.root), ("LOCAL", self.local), ("GAME_ROOT", self.games)]:
            patcher = patch.object(deploy.hmcl, name, value)
            patcher.start()
            self.addCleanup(patcher.stop)
        (self.root / "mod.properties").write_text("mod_id=pinkkicore\nmod_version=0.1.0\n")
        config = self.root / "config"
        config.mkdir()
        config.joinpath("hmcl-runtime.json").write_text(json.dumps({"1.20.1": {"geckolib": {
            "version": "4.4.9", "filename": "geckolib.jar"}}}))
        version = self.root / "versions/1.20.1"
        version.mkdir(parents=True)
        version.joinpath("gradle.properties").write_text("geckolib_version_range=[4.4.9,5)\n")
        self.source = deploy.hmcl.artifact("1.20.1")
        self.jar(self.source, "pinkkicore", "0.1.0")
        self.targets = []
        for profile in ["clean", "compat"]:
            target = self.games / "versions" / f"PinkkiCore-1.20.1-{profile}"
            target.mkdir(parents=True)
            (target / f"{target.name}.json").write_text(json.dumps({"patches": [
                {"id": "game", "version": "1.20.1"}, {"id": "forge", "version": "47.4.28"}],
                "javaVersion": {"majorVersion": 17}}))
            settings = target / ".hmcl/config/instance-game-settings.json"
            settings.parent.mkdir(parents=True)
            settings.write_text(json.dumps({"overrideProperties": ["runningDirectory", "javaType", "customJavaPath"],
                "javaType": "CUSTOM", "customJavaPath": "/fixture/java"}))
            self.targets.append(target)

    def jar(self, path, mod_id, version):
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("META-INF/mods.toml", f'[[mods]]\nmodId="{mod_id}"\nversion="{version}"\n'
                f'[[dependencies.{mod_id}]]\nmodId="forge"\nversionRange="[47.1,)"\n'
                f'[[dependencies.{mod_id}]]\nmodId="minecraft"\nversionRange="[1.20.1,)"\n')

    def curios_jar(self, path, version, minecraft="[1.20,1.21)", loader="forge", manifest=False):
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("META-INF/mods.toml", '\n'.join([
                '[[mods]]', 'modId="curios"',
                'version="' + ("${file.jarVersion}" if manifest else version) + '"',
                '[[dependencies.curios]]', 'modId="' + loader + '"', 'versionRange="[46,)"',
                '[[dependencies.curios]]', 'modId="minecraft"', 'versionRange="' + minecraft + '"']))
            if manifest:
                archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\nImplementation-Version: " + version + "\r\n")

    def test_curios_manifest_preserved_and_duplicates_rejected(self):
        jar = self.targets[0] / "mods/curios.jar"
        self.curios_jar(jar, "5.14.1+1.20.1", manifest=True)
        self.assertEqual(deploy.curios_plan(self.targets[0]), jar)
        self.curios_jar(jar.with_name("second.jar"), "5.14.1+1.20.1")
        with self.assertRaisesRegex(ValueError, "多份"):
            deploy.curios_plan(self.targets[0])

    def test_curios_wrong_version_and_loader_rejected(self):
        jar = self.targets[0] / "mods/curios.jar"
        for version, mc, loader in [("5.14.0", "[1.20,1.21)", "forge"),
                                     ("6.0.0", "[1.20,1.21)", "forge"),
                                     ("5.14.1", "[1.19.2]", "forge"),
                                     ("5.14.1", "[1.20,1.21)", "neoforge")]:
            self.curios_jar(jar, version, mc, loader)
            with self.assertRaises(ValueError):
                deploy.curios_plan(self.targets[0])

    def java_result(self):
        return subprocess.CompletedProcess([], 0, "", "java.version = 17.0.20.1\nos.arch = aarch64\n")

    def test_java_and_isolation(self):
        with patch.object(deploy.subprocess, "run", return_value=self.java_result()):
            self.assertEqual(deploy.check_instance("clean")[1]["java_arch"], "aarch64")
            p = self.targets[0] / ".hmcl/config/instance-game-settings.json"
            data = json.loads(p.read_text())
            data["runningDirectory"] = str(self.root)
            p.write_text(json.dumps(data))
            with self.assertRaisesRegex(ValueError, "各实例独立"):
                deploy.check_instance("clean")

    def test_wrong_minecraft_rejected(self):
        p = self.targets[0] / f"{self.targets[0].name}.json"
        data = json.loads(p.read_text())
        data["patches"][0]["version"] = "1.20.2"
        p.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            deploy.check_instance("clean")

    def test_compatible_dependency_preserved_and_duplicate_rejected(self):
        jar = self.targets[0] / "mods/existing-gecko.jar"
        self.jar(jar, "geckolib", "4.8.4")
        self.assertEqual(deploy.dependency_plan(self.targets[0], (4, 4, 9)), jar)
        self.jar(jar.with_name("duplicate.jar"), "geckolib", "4.4.9")
        with self.assertRaisesRegex(ValueError, "多份"):
            deploy.dependency_plan(self.targets[0], (4, 4, 9))

    def test_incompatible_dependency_rejected(self):
        self.jar(self.targets[0] / "mods/old.jar", "geckolib", "4.4.8")
        with self.assertRaisesRegex(ValueError, "不满足"):
            deploy.dependency_plan(self.targets[0], (4, 4, 9))

    def test_running_instance_rejected(self):
        result = subprocess.CompletedProcess([], 0, f"java --gameDir {self.targets[0]} --accessToken SECRET", "")
        with patch.object(deploy.subprocess, "run", return_value=result):
            with self.assertRaisesRegex(ValueError, "仍在运行") as error:
                deploy.ensure_stopped(self.targets)
        self.assertNotIn("SECRET", str(error.exception))

    def test_both_install_backup_and_preserve_other_files(self):
        gecko = self.root / "gecko.jar"
        self.jar(gecko, "geckolib", "4.4.9")
        old = self.targets[0] / "mods/old-pinkki.jar"
        self.jar(old, "pinkkicore", "0.0.1")
        other = self.targets[0] / "mods/third-party.jar"
        self.jar(other, "other", "1.0")
        before = other.read_bytes()
        save = self.targets[0] / "saves/world/level.dat"
        save.parent.mkdir(parents=True)
        save.write_bytes(b"save")
        overlay = self.targets[0] / "resourcepacks/pinkkicore-dev-overlay/.pinkkicore-hot-sync.json"
        overlay.parent.mkdir(parents=True)
        overlay.write_text("{}")
        with patch.object(deploy, "check_instance", side_effect=[(t, {"instance": t.name}) for t in self.targets]), \
                patch.object(deploy, "ensure_stopped"), patch.object(deploy, "runtime_jar", return_value=gecko), \
                patch.object(deploy.subprocess, "run"):
            deploy.deploy(["clean", "compat"])
        for target in self.targets:
            self.assertEqual((target / "mods" / self.source.name).read_bytes(), self.source.read_bytes())
            self.assertEqual((target / "mods/gecko.jar").read_bytes(), gecko.read_bytes())
        self.assertEqual(other.read_bytes(), before)
        self.assertEqual(save.read_bytes(), b"save")
        self.assertTrue(overlay.is_file())
        receipt_path = next((self.local / "deployments").glob("*/receipt.json"))
        receipt = json.loads(receipt_path.read_text())
        self.assertEqual(len(receipt["instances"][0]["prepared_hot_sync_overlays"]), 1)
        self.assertEqual(receipt["instances"][1]["prepared_hot_sync_overlays"], [])
        self.assertFalse(old.exists())
        self.assertEqual(len(list((self.local / "backups").glob("*/old-pinkki.jar"))), 1)

    def test_failed_build_does_not_install(self):
        with patch.object(deploy, "check_instance", side_effect=[(t, {}) for t in self.targets]), \
                patch.object(deploy, "ensure_stopped"), \
                patch.object(deploy.subprocess, "run", side_effect=subprocess.CalledProcessError(1, ["build"])):
            with self.assertRaises(subprocess.CalledProcessError):
                deploy.deploy(["clean", "compat"])
        self.assertFalse(any(t.joinpath("mods").exists() for t in self.targets))
        receipt = next((self.local / "deployments").glob("*/receipt.json"))
        self.assertEqual(json.loads(receipt.read_text())["status"], "failed")

    def test_existing_dependency_not_replaced_by_deploy(self):
        dependencies = []
        for target in self.targets:
            jar = target / "mods/existing-gecko.jar"
            self.jar(jar, "geckolib", "4.8.4")
            dependencies.append((jar, jar.read_bytes()))
        with patch.object(deploy, "check_instance", side_effect=[(t, {}) for t in self.targets]), \
                patch.object(deploy, "ensure_stopped"), patch.object(deploy, "runtime_jar") as download, \
                patch.object(deploy.subprocess, "run"):
            deploy.deploy(["clean", "compat"])
            download.assert_not_called()
        for jar, content in dependencies:
            self.assertEqual(jar.read_bytes(), content)

    def test_download_hash_mismatch_rejected(self):
        from io import BytesIO
        config = {"filename": "gecko.jar", "version": "4.4.9", "sha512": "invalid",
                  "url": "https://example.invalid/gecko.jar"}
        with patch.object(deploy, "urlopen", return_value=BytesIO(b"wrong bytes")):
            with self.assertRaisesRegex(ValueError, "SHA512"):
                deploy.runtime_jar(config)
        self.assertFalse((self.local / "runtime-cache/gecko.jar").exists())
        self.assertEqual(list((self.local / "runtime-cache").glob("*.tmp")), [])

    def test_symlink_outside_project_rejected(self):
        (self.targets[0] / "mods").symlink_to(self.root.parent, target_is_directory=True)
        with self.assertRaises(ValueError):
            deploy.dependency_plan(self.targets[0], (4, 4, 9))


if __name__ == "__main__":
    unittest.main()

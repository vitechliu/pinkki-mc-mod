#!/usr/bin/env python3
"""Validate the distributable, including metadata and shared/loader classes. Python 3.11+."""
import hashlib
import json
from pathlib import Path
import shutil
import struct
import sys
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parent.parent
TARGETS = {"1.20.1": ("forge", 61), "1.19.1": ("forge", 61), "1.21.1": ("neoforge", 65)}


def read_properties(path):
    return dict(
        line.split("=", 1)
        for line in path.read_text().splitlines()
        if "=" in line and not line.startswith("#")
    )


def main():
    mc = sys.argv[1]
    loader, major = TARGETS[mc]
    props = read_properties(ROOT / "mod.properties")
    name = f"{props['mod_id']}-{loader}-{mc}-{props['mod_version']}.jar"
    jar = ROOT / "versions" / mc / "build/libs" / name
    with zipfile.ZipFile(jar) as archive:
        assert archive.testzip() is None, "Corrupt archive"
        metadata_path = "META-INF/neoforge.mods.toml" if loader == "neoforge" else "META-INF/mods.toml"
        metadata_text = archive.read(metadata_path).decode()
        assert "${" not in metadata_text, "Unexpanded metadata"
        metadata = tomllib.loads(metadata_text)
        assert metadata["mods"][0]["modId"] == props["mod_id"]
        assert metadata["mods"][0]["version"] == props["mod_version"]
        assert metadata["license"] == "MIT"
        deps = {item["modId"]: item for item in metadata["dependencies"][props["mod_id"]]}
        assert deps["minecraft"]["versionRange"] == f"[{mc}]"
        assert loader in deps
        if mc == "1.20.1":
            target_props = read_properties(ROOT / "versions" / mc / "gradle.properties")
            assert deps["geckolib"]["mandatory"] is True
            assert deps["geckolib"]["side"] == "BOTH"
            assert deps["geckolib"]["versionRange"] == target_props["geckolib_version_range"]
            assert deps["forge"]["versionRange"] == f"[{target_props['forge_min_version']},48)"
            assert not any(
                path.startswith(("software/bernie/", "com/eliotlash/", "META-INF/jarjar/"))
                for path in archive.namelist()
            ), "Animation dependencies must remain external"
        assert "LICENSE" in archive.namelist()
        if loader == "neoforge":
            assert "META-INF/mods.toml" not in archive.namelist(), "Forge metadata leaked into NeoForge JAR"
        prefix = "io/github/vitech/pinkkicore"
        for path in [f"{prefix}/core/ModInfo.class", f"{prefix}/{loader}/PinkkiCore.class"]:
            assert struct.unpack(">H", archive.read(path)[6:8])[0] <= major, "Wrong Java target"
        json.loads(archive.read("pack.mcmeta"))
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    if len(sys.argv) > 2:
        output = Path(sys.argv[2])
        output.mkdir(parents=True, exist_ok=True)
        shutil.copy2(jar, output / name)
        (output / f"{name}.sha256").write_text(f"{digest}  {name}\n")
    print(f"OK {name} SHA256={digest}")


if __name__ == "__main__":
    main()

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
            recipe = json.loads(archive.read("data/pinkkicore/recipes/guaba.json"))
            assert recipe["type"] == "minecraft:crafting_shapeless"
            assert sorted(recipe["ingredients"], key=lambda item: item["item"]) == [
                {"item": "minecraft:melon_slice"}, {"item": "minecraft:mud"}
            ]
            assert recipe["result"] == {"item": "pinkkicore:guaba", "count": 1}
            unlock = json.loads(archive.read("data/pinkkicore/advancements/recipes/misc/guaba.json"))
            assert unlock["rewards"]["recipes"] == ["pinkkicore:guaba"]
            assert unlock["requirements"] == [["has_melon_slice", "has_mud", "has_the_recipe"]]
            for language, item_name, tooltip in [
                ("zh_cn", "瓜巴", "一切瓜巴的起源"),
                ("en_us", "Guaba", "The origin of all Guaba"),
            ]:
                strings = json.loads(archive.read(f"assets/pinkkicore/lang/{language}.json"))
                assert strings["item.pinkkicore.guaba"] == item_name
                assert strings["tooltip.pinkkicore.guaba"] == tooltip
            model = json.loads(archive.read("assets/pinkkicore/models/item/guaba.json"))
            assert model == {"parent": "minecraft:item/generated", "textures": {"layer0": "pinkkicore:item/guaba"}}
            texture = archive.read("assets/pinkkicore/textures/item/guaba.png")
            assert texture[:8] == b"\x89PNG\r\n\x1a\n"
            assert struct.unpack(">II", texture[16:24]) == (16, 16)
            assert texture == (ROOT / "wiki/public/images/guaba.png").read_bytes(), "Wiki icon differs from mod"
            for item_class in ("GuabaItem", "ModItems"):
                assert f"io/github/vitech/pinkkicore/forge/item/{item_class}.class" in archive.namelist()
            silent = json.loads(archive.read("data/pinkkicore/silentgear_materials/guaba.json"))
            assert silent["crafting_items"]["main"] == {"item": "pinkkicore:guaba"}
            assert silent["traits"] == {"main": [], "rod": []}
            definition = json.loads(archive.read("data/pinkkicore/tinkering/materials/definition/guaba.json"))
            optional_condition = {"type": "forge:mod_loaded", "modid": "tconstruct"}
            assert definition["condition"] == optional_condition
            mapping = json.loads(archive.read("data/pinkkicore/recipes/compat/tconstruct/guaba_material.json"))
            assert mapping["conditions"] == [optional_condition], "Missing-mod recipe must be skipped"
            assert mapping["ingredient"] == {"item": "pinkkicore:guaba"}
            assert mapping["material"] == "pinkkicore:guaba" and mapping["needed"] == mapping["value"] == 1
            for section in ("stats", "traits"):
                json.loads(archive.read(f"data/pinkkicore/tinkering/materials/{section}/guaba.json"))
            json.loads(archive.read("assets/pinkkicore/tinkering/materials/guaba.json"))
            tetra = json.loads(archive.read("data/tetra/materials/wood/pinkkicore_guaba.json"))
            assert tetra["key"] == "pinkkicore_guaba" and tetra["effects"] == {}
            assert tetra["material"] == {"items": ["pinkkicore:guaba"], "count": 1}
            assert not any(path.startswith(("pinkkicore/test/", "net/silentchaos512/", "slimeknights/", "se/mickelus/"))
                           for path in archive.namelist()), "Test probe or optional mod code leaked into release"
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

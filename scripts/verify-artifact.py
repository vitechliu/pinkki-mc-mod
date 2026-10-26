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
            assert deps['jei']['mandatory'] is False and deps['jei']['side'] == 'CLIENT'
            assert deps['jei']['versionRange'] == target_props['jei_version_range']
            assert not any(path.startswith('mezz/jei/') for path in archive.namelist()), 'JEI must remain external'
            for plugin_class in ('GuabaJeiPlugin', 'GuabaConversionCategory'):
                assert f'io/github/vitech/pinkkicore/forge/client/jei/{plugin_class}.class' in archive.namelist()
            assert not any(
                path.startswith(("software/bernie/", "com/eliotlash/", "META-INF/jarjar/"))
                for path in archive.namelist()
            ), "Animation dependencies must remain external"
            import runpy
            guide_generator = runpy.run_path(str(ROOT / 'scripts/generate-guide.py'))
            assert json.loads(archive.read('assets/pinkkicore/guide/zh_cn.json')) == guide_generator['compile_book'](), 'Guide prose differs from Wiki'
            for guide_class in ('guide/GuideBook', 'guide/GuideItem', 'client/guide/GuideScreen', 'client/guide/GuideClientEvents'):
                assert f'io/github/vitech/pinkkicore/forge/{guide_class}.class' in archive.namelist()
            assert not {'patchouli', 'guideme', 'mantle'} & set(deps), 'Guide must not require a framework'
            assert not any(path.startswith(('vazkii/patchouli/', 'guideme/', 'slimeknights/mantle/')) for path in archive.namelist())
            guide_recipe = json.loads(archive.read('data/pinkkicore/recipes/guide_book.json'))
            assert guide_recipe['result']['item'] == 'pinkkicore:guide_book'
            assert guide_recipe['ingredients'] == [{'item': 'minecraft:book'}, {'item': 'pinkkicore:guaba'}]
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
            thunder_texture = archive.read("assets/pinkkicore/textures/item/thunder_guaba.png")
            assert thunder_texture[:8] == b"\x89PNG\r\n\x1a\n"
            assert struct.unpack(">II", thunder_texture[16:24]) == (16, 16)
            assert thunder_texture == (ROOT / "designs/thunder-guaba/thunder_guaba.png").read_bytes()
            assert thunder_texture == (ROOT / "wiki/public/images/thunder_guaba.png").read_bytes()
            thunder_model = json.loads(archive.read("assets/pinkkicore/models/item/thunder_guaba.json"))
            assert thunder_model == {"parent": "minecraft:item/generated", "textures": {"layer0": "pinkkicore:item/thunder_guaba"}}
            for language, item_name in [("zh_cn", "雷霆瓜巴"), ("en_us", "Thunder Guaba")]:
                strings = json.loads(archive.read(f"assets/pinkkicore/lang/{language}.json"))
                assert strings["item.pinkkicore.thunder_guaba"] == item_name
            assert "io/github/vitech/pinkkicore/forge/item/GuabaLightningConversion.class" in archive.namelist()
            for item_class in ("GuabaItem", "ModItems"):
                assert f"io/github/vitech/pinkkicore/forge/item/{item_class}.class" in archive.namelist()
            for source, resource in [
                ("bingwa.geo.json", "geo/bingwa.geo.json"),
                ("bingwa.animation.json", "animations/bingwa.animation.json"),
                ("textures/bingwa.png", "textures/entity/bingwa.png"),
            ]:
                assert archive.read(f"assets/pinkkicore/{resource}") == (ROOT / "designs/bingwa" / source).read_bytes(), "Bingwa source/export mismatch"
            animations = json.loads(archive.read("assets/pinkkicore/animations/bingwa.animation.json"))["animations"]
            assert set(animations) == {f"animation.bingwa.{name}" for name in ("idle", "walk", "jump", "swim", "float", "croak", "tongue")}
            egg = json.loads(archive.read("assets/pinkkicore/models/item/bingwa_spawn_egg.json"))
            assert egg == {"parent": "minecraft:item/template_spawn_egg"}
            loot = json.loads(archive.read("data/pinkkicore/loot_tables/entities/bingwa.json"))
            assert loot == {"type": "minecraft:entity", "pools": []}
            for language, entity_name, egg_name in [("zh_cn", "饼蛙", "饼蛙刷怪蛋"), ("en_us", "Bingwa", "Bingwa Spawn Egg")]:
                strings = json.loads(archive.read(f"assets/pinkkicore/lang/{language}.json"))
                assert strings["entity.pinkkicore.bingwa"] == entity_name
                assert strings["item.pinkkicore.bingwa_spawn_egg"] == egg_name
            for path in ("entity/BingwaEntity", "entity/ModEntities", "client/BingwaModel", "client/BingwaRenderer", "client/ClientEvents"):
                assert f"io/github/vitech/pinkkicore/forge/{path}.class" in archive.namelist()
            assert archive.read("assets/pinkkicore/textures/entity/pinkki.png") == (ROOT / "designs/pinkki-skin/pinkki-alex.png").read_bytes(), "Pinkki skin/source mismatch"
            assert json.loads(archive.read("assets/pinkkicore/models/item/pinkki_spawn_egg.json")) == {"parent": "minecraft:item/template_spawn_egg"}
            assert json.loads(archive.read("data/pinkkicore/loot_tables/entities/pinkki.json")) == {"type": "minecraft:entity", "pools": []}
            for language, entity_name, egg_name in [("zh_cn", "粉红妹", "粉红妹刷怪蛋"), ("en_us", "Pinkki", "Pinkki Spawn Egg")]:
                strings = json.loads(archive.read(f"assets/pinkkicore/lang/{language}.json"))
                assert strings["entity.pinkkicore.pinkki"] == entity_name
                assert strings["item.pinkkicore.pinkki_spawn_egg"] == egg_name
            for path in ("entity/PinkkiEntity", "client/PinkkiRenderer"):
                assert f"io/github/vitech/pinkkicore/forge/{path}.class" in archive.namelist()
            assert not any("pinkkicore/test/" in name for name in archive.namelist()), "Test probe leaked into release JAR"
            silent = json.loads(archive.read("data/pinkkicore/silentgear_materials/guaba.json"))
            assert silent["crafting_items"]["main"] == {"item": "pinkkicore:guaba"}
            trait_entry = {"name": "pinkkicore:guaba_touch", "level": 1}
            assert silent["traits"] == {"main": [trait_entry], "rod": [trait_entry]}
            trait = json.loads(archive.read("data/pinkkicore/silentgear_traits/guaba_touch.json"))
            assert trait["type"] == "silentgear:target_effect" and trait["max_level"] == 1
            assert trait["effects"]["tool"]["1"] == [{"effect": "pinkkicore:guaba", "amplifier": 0, "duration": 10.0}]
            modifier = json.loads(archive.read("data/pinkkicore/tinkering/modifiers/guaba_touch.json"))
            module = modifier["modules"][0]
            assert module["type"] == "tconstruct:mob_effect" and module["hooks"] == ["tconstruct:melee_hit"]
            assert module["effect"] == "pinkkicore:guaba" and module["level"] == {"flat": 1.0}
            assert module["time"] == {"flat": 200.0} and module["chance"] == {"flat": 1.0}
            traits = json.loads(archive.read("data/pinkkicore/tinkering/materials/traits/guaba.json"))
            assert traits["default"] == [trait_entry]
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
            assert tetra["key"] == "pinkkicore_guaba" and tetra["effects"] == {"pinkkicore_guaba_touch": 1}
            assert tetra["material"] == {"items": ["pinkkicore:guaba"], "count": 1}
            hit = json.loads(archive.read("data/tetra/item_effects/pinkkicore_guaba_touch.json"))
            assert hit["trigger"] == "tetra:apply_hit_effects" and hit["effect"] == "pinkkicore_guaba_touch"
            assert hit["outcome"] == {"type": "tetra:apply_effect", "effect": "pinkkicore:guaba", "entity": "target",
                                      "source": "attacker", "duration": 200, "amplifier": 0}
            assert not any(path.startswith(("pinkkicore/test/", "net/silentchaos512/", "slimeknights/", "se/mickelus/"))
                           for path in archive.namelist()), "Test probe or optional mod code leaked into release"
            for source, resource in [
                ("guaba_workbench.json", "models/block/guaba_workbench.json"),
                ("textures/guaba_workbench.png", "textures/block/guaba_workbench.png"),
            ]:
                expected = (ROOT / "designs/guaba-workbench" / source).read_bytes()
                actual = archive.read(f"assets/pinkkicore/{resource}")
                assert actual == expected, "Workbench source/export mismatch"
            state = json.loads(archive.read("assets/pinkkicore/blockstates/guaba_workbench.json"))
            assert set(state["variants"]) == {"facing="+side for side in ("north", "east", "south", "west")}
            conversion = json.loads(archive.read("data/pinkkicore/recipes/guaba_conversion/dirt_to_mud.json"))
            assert conversion == {"type": "pinkkicore:guaba_conversion", "ingredient": {"item": "minecraft:dirt"},
                                  "input_count": 1, "guaba_cost": 1, "result": {"item": "minecraft:mud", "count": 1}}
            sword_recipe = json.loads(archive.read('data/pinkkicore/recipes/guaba_conversion/diamond_sword_to_guaba_sword.json'))
            assert sword_recipe == {'type': 'pinkkicore:guaba_conversion', 'ingredient': {'item': 'minecraft:diamond_sword'},
                                    'input_count': 1, 'guaba_cost': 1, 'copy_input_tag': True,
                                    'result': {'item': 'pinkkicore:guaba_sword', 'count': 1}}
            assert json.loads(archive.read('assets/pinkkicore/models/item/guaba_sword.json')) == {
                'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'pinkkicore:item/guaba_sword'}}
            sword_icon = archive.read('assets/pinkkicore/textures/item/guaba_sword.png')
            assert struct.unpack('>II', sword_icon[16:24]) == (16, 16)
            assert sword_icon == (ROOT / 'wiki/public/images/guaba_sword.png').read_bytes()
            assert sword_icon == (ROOT / 'designs/guaba-sword/guaba_sword.png').read_bytes()
            assert 'io/github/vitech/pinkkicore/forge/item/GuabaSwordItem.class' in archive.namelist()
            bow_recipe = json.loads(archive.read('data/pinkkicore/recipes/guaba_conversion/bow_to_guaba_bow.json'))
            assert bow_recipe == {'type': 'pinkkicore:guaba_conversion', 'ingredient': {'item': 'minecraft:bow'},
                                  'input_count': 1, 'guaba_cost': 1, 'copy_input_tag': True,
                                  'result': {'item': 'pinkkicore:guaba_bow', 'count': 1}}
            bow_model = json.loads(archive.read('assets/pinkkicore/models/item/guaba_bow.json'))
            assert bow_model['parent'] == 'minecraft:item/bow'
            assert len(bow_model['overrides']) == 3
            for index, predicate in enumerate(({'pulling': 1}, {'pulling': 1, 'pull': .65}, {'pulling': 1, 'pull': .9})):
                assert bow_model['overrides'][index] == {'predicate': predicate, 'model': f'pinkkicore:item/guaba_bow_pulling_{index}'}
                assert json.loads(archive.read(f'assets/pinkkicore/models/item/guaba_bow_pulling_{index}.json')) == {
                    'parent': 'pinkkicore:item/guaba_bow', 'textures': {'layer0': f'pinkkicore:item/guaba_bow_pulling_{index}'}}
            for suffix in ('', '_pulling_0', '_pulling_1', '_pulling_2'):
                icon = archive.read(f'assets/pinkkicore/textures/item/guaba_bow{suffix}.png')
                assert struct.unpack('>II', icon[16:24]) == (16, 16)
                assert icon == (ROOT / f'designs/guaba-bow/guaba_bow{suffix}.png').read_bytes()
                if not suffix: assert icon == (ROOT / 'wiki/public/images/guaba_bow.png').read_bytes()
            assert 'io/github/vitech/pinkkicore/forge/item/GuabaBowItem.class' in archive.namelist()
            workbench_item = json.loads(archive.read("assets/pinkkicore/models/item/guaba_workbench.json"))
            assert workbench_item == {"parent": "pinkkicore:block/guaba_workbench"}
            workbench_loot = json.loads(archive.read("data/pinkkicore/loot_tables/blocks/guaba_workbench.json"))
            assert workbench_loot["pools"][0]["entries"][0]["name"] == "pinkkicore:guaba_workbench"
            for path in ("workbench/WorkbenchContent", "workbench/GuabaWorkbenchBlock", "workbench/GuabaWorkbenchEntity",
                         "workbench/GuabaWorkbenchMenu", "workbench/GuabaConversionRecipe", "workbench/RedstoneMode",
                         "workbench/SideMode", "client/GuabaWorkbenchScreen"):
                assert f"io/github/vitech/pinkkicore/forge/{path}.class" in archive.namelist()
            for language in ("zh_cn", "en_us"):
                strings = json.loads(archive.read(f"assets/pinkkicore/lang/{language}.json"))
                assert "block.pinkkicore.guaba_workbench" in strings
                for mode in range(4):
                    assert f"gui.pinkkicore.redstone.{mode}" in strings
                for side in ("down", "up", "north", "south", "west", "east"):
                    assert f"gui.pinkkicore.direction.{side}" in strings
            for path in ('effect/GuabaEffect', 'effect/GuabaPotions'):
                assert f'io/github/vitech/pinkkicore/forge/{path}.class' in archive.namelist()
            definition_path = 'data/pinkkicore/guaba_potions.json'
            assert archive.read(definition_path) == (ROOT / 'versions/1.20.1/src/main/resources' / definition_path).read_bytes()
            definition = json.loads(archive.read(definition_path))
            assert len(definition['levels']) == 4
            assert [(p['amplifier'], p['duration']) for p in definition['potions']] == [(0, 3600), (0, 9600), (1, 1800)]
            assert archive.read('assets/pinkkicore/textures/mob_effect/guaba.png') == texture
            for language in ('zh_cn', 'en_us'):
                strings = json.loads(archive.read(f'assets/pinkkicore/lang/{language}.json'))
                assert 'effect.pinkkicore.guaba' in strings
                for container in ('potion', 'splash_potion', 'lingering_potion', 'tipped_arrow'):
                    for variant in ('guaba', 'long_guaba', 'strong_guaba'):
                        assert f'item.minecraft.{container}.effect.{variant}' in strings
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

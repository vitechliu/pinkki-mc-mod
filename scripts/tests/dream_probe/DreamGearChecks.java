package io.github.vitech.pinkkicore.test;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.github.vitech.pinkkicore.test.DreamProbe.require;
import static io.github.vitech.pinkkicore.test.DreamProbe.tickPlayer;

final class DreamGearChecks {
    private static final String[] MATERIALS = {"cloud_dream", "guaba_cloud_dream", "nether_dream", "guaba_nether_dream"};

    private DreamGearChecks() {}

    private static Item item(String id) {
        return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)), id);
    }

    static final class SilentGear {
        static void run(ServerLevel overworld, ServerLevel nether) {
            for (String name : MATERIALS) {
                var definition = net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation("pinkkicore", name));
                require(definition != null && definition.getIngredient().test(new ItemStack(item("pinkkicore:" + name))),
                    "Silent Gear missing dream material " + name);
            }
            var iron = gear("silentgear:iron", "sword", net.silentchaos512.gear.api.item.GearType.SWORD, true);
            var cloud = gear("pinkkicore:cloud_dream", "sword", net.silentchaos512.gear.api.item.GearType.SWORD, true);
            var guabaCloud = gear("pinkkicore:guaba_cloud_dream", "sword", net.silentchaos512.gear.api.item.GearType.SWORD, true);
            var shadow = gear("pinkkicore:nether_dream", "sword", net.silentchaos512.gear.api.item.GearType.SWORD, true);
            var guabaShadow = gear("pinkkicore:guaba_nether_dream", "sword", net.silentchaos512.gear.api.item.GearType.SWORD, true);
            var cloudArmor = gear("pinkkicore:cloud_dream", "chestplate", net.silentchaos512.gear.api.item.GearType.CHESTPLATE, false);
            var shadowArmor = gear("pinkkicore:nether_dream", "chestplate", net.silentchaos512.gear.api.item.GearType.CHESTPLATE, false);
            verify("Silent Gear", overworld, nether, iron, cloud, guabaCloud, shadow, guabaShadow, cloudArmor, shadowArmor);
        }

        private static ItemStack gear(String material, String base, net.silentchaos512.gear.api.item.GearType type,
                                      boolean withRod) {
            var instance = net.silentchaos512.gear.gear.material.MaterialInstance.of(
                net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation(material)));
            var main = net.silentchaos512.gear.api.part.PartType.MAIN.makeCompoundPart(type, List.of(instance)).orElseThrow();
            var gear = (net.silentchaos512.gear.api.item.ICoreItem) item("silentgear:" + base);
            if (!withRod) {
                return gear.construct(List.of(main));
            }
            var rod = net.silentchaos512.gear.api.part.PartType.ROD.makeCompoundPart(type, List.of(instance)).orElseThrow();
            return gear.construct(List.of(main, rod));
        }
    }

    static final class Tinkers {
        static void run(ServerLevel overworld, ServerLevel nether) {
            for (String name : MATERIALS) {
                var id = new slimeknights.tconstruct.library.materials.definition.MaterialId("pinkkicore:" + name);
                var registry = slimeknights.tconstruct.library.materials.MaterialRegistry.getInstance();
                require(registry.getMaterial(id).isCraftable(), "unregistered Tinkers material " + name);
                var mapping = slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache.findRecipe(
                    new ItemStack(item("pinkkicore:" + name)));
                require(mapping != null && mapping.getMaterial().getId().equals(id) && mapping.getValue() == 1,
                    "wrong Tinkers ingredient mapping " + name);
            }
            var armorPos = new BlockPos(9, 200, 10);
            var builder = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("tconstruct:part_builder")));
            overworld.setBlockAndUpdate(armorPos, builder.defaultBlockState());
            var bench = (slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity) overworld.getBlockEntity(armorPos);
            require(bench != null, "Tinkers part builder missing");
            bench.setItem(1, new ItemStack(item("tconstruct:pattern")));
            bench.setItem(0, new ItemStack(item("pinkkicore:nether_dream"), 6));
            bench.selectRecipe(new slimeknights.tconstruct.library.recipe.partbuilder.Pattern(new ResourceLocation("tconstruct:chestplate_plating")));
            ItemStack plating = bench.calcResult(null);
            require(plating.is(item("tconstruct:chestplate_plating")), "Tinkers did not craft actual dream plating");
            var material = ((slimeknights.tconstruct.library.tools.part.IMaterialItem) plating.getItem()).getMaterial(plating);
            require(material.getId().toString().equals("pinkkicore:nether_dream"), "Tinkers plating lost material");

            var iron = gear("tconstruct:iron", "sword", 3);
            var cloud = gear("pinkkicore:cloud_dream", "sword", 3);
            var guabaCloud = gear("pinkkicore:guaba_cloud_dream", "sword", 3);
            var shadow = gear("pinkkicore:nether_dream", "sword", 3);
            var guabaShadow = gear("pinkkicore:guaba_nether_dream", "sword", 3);
            var cloudArmor = gear("pinkkicore:cloud_dream", "plate_chestplate", 2);
            var shadowArmor = gear("pinkkicore:nether_dream", "plate_chestplate", 2);
            verify("Tinkers", overworld, nether, iron, cloud, guabaCloud, shadow, guabaShadow, cloudArmor, shadowArmor);
        }

        private static ItemStack gear(String material, String base, int parts) {
            var materialId = new slimeknights.tconstruct.library.materials.definition.MaterialId(material);
            var type = slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(materialId);
            var materials = new slimeknights.tconstruct.library.materials.definition.IMaterial[parts];
            Arrays.fill(materials, type);
            return slimeknights.tconstruct.library.tools.helper.ToolBuildHandler.buildItemFromMaterials(
                (slimeknights.tconstruct.library.tools.item.IModifiable) item("tconstruct:" + base),
                slimeknights.tconstruct.library.tools.nbt.MaterialNBT.of(materials));
        }
    }

    static final class Tetra {
        static void run(ServerLevel overworld, ServerLevel nether) {
            for (String name : MATERIALS) {
                var definition = se.mickelus.tetra.data.DataManager.instance.materialData.getData(
                    new ResourceLocation("tetra:metal/pinkkicore_" + name));
                require(definition != null && definition.material.getApplicableItemStacks()[0].is(item("pinkkicore:" + name)),
                    "Tetra missing dream ingredient " + name);
            }
            verify("Tetra", overworld, nether, weapon("iron", overworld), weapon("pinkkicore_cloud_dream", overworld),
                weapon("pinkkicore_guaba_cloud_dream", overworld), weapon("pinkkicore_nether_dream", overworld),
                weapon("pinkkicore_guaba_nether_dream", overworld), ItemStack.EMPTY, ItemStack.EMPTY);
        }

        private static ItemStack weapon(String material, ServerLevel level) {
            var sword = new ItemStack(item("tetra:modular_sword"));
            for (String part : new String[]{"basic_blade", "basic_hilt"}) {
                var module = se.mickelus.tetra.module.ModuleRegistry.instance.getModule(new ResourceLocation("tetra:sword/" + part));
                var variant = part + "/" + material;
                require(Arrays.stream(module.getVariantData()).anyMatch(v -> variant.equals(v.key)), "Tetra variant missing " + variant);
                module.addModule(sword, variant, null);
            }
            ((se.mickelus.tetra.items.modular.IModularItem) sword.getItem()).assemble(sword, level, 0);
            return sword;
        }
    }

    private static void verify(String host, ServerLevel overworld, ServerLevel nether, ItemStack iron, ItemStack cloud,
                               ItemStack guabaCloud, ItemStack shadow, ItemStack guabaShadow, ItemStack cloudArmor,
                               ItemStack shadowArmor) {
        BlockPos position = new BlockPos(7, 101, 7);
        var player = DreamProbe.player(overworld, position);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        require(cloud.getMaxDamage() == iron.getMaxDamage() && guabaCloud.getMaxDamage() > cloud.getMaxDamage(),
            host + " base cloud durability differs from iron or guaba bonus is missing");
        player.setItemSlot(EquipmentSlot.MAINHAND, cloud);
        player.setItemSlot(EquipmentSlot.CHEST, cloudArmor);
        tickPlayer(player, 1);
        near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 1.08, host + " cloud speed");
        player.setItemSlot(EquipmentSlot.MAINHAND, guabaCloud);
        tickPlayer(player, 25);
        near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 1.08, host + " guaba cloud speed stacked");
        var target = EntityType.COW.create(overworld);
        require(target != null, "cow fixture");
        target.moveTo(player.getX() + 1, player.getY(), player.getZ(), 0, 0);
        var guaba = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("pinkkicore:guaba"));
        player.attack(target);
        require(target.hasEffect(Objects.requireNonNull(guaba)), host + " guaba cloud did not apply Guaba Touch");

        player.setItemSlot(EquipmentSlot.MAINHAND, shadow);
        player.setItemSlot(EquipmentSlot.CHEST, shadowArmor);
        tickPlayer(player, 1);
        double plainAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double plainArmor = player.getAttributeValue(Attributes.ARMOR);
        near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, host + " cloud speed leaked after removal");
        changeDimension(player, nether);
        near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), plainAttack + 2, host + " missing Nether damage");
        near(player.getAttributeValue(Attributes.ARMOR), plainArmor + (shadowArmor.isEmpty() ? 0 : 2), host + " missing Nether armor");
        player.setItemSlot(EquipmentSlot.MAINHAND, guabaShadow);
        tickPlayer(player, 1);
        double empoweredAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        require(empoweredAttack > plainAttack + 2, host + " guaba nether base damage missing");
        changeDimension(player, overworld);
        near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), empoweredAttack - 2, host + " Nether damage leaked");
        near(player.getAttributeValue(Attributes.ARMOR), plainArmor, host + " Nether armor leaked");
        System.out.println("DREAM_PROBE " + host + " material registry, attributes and Guaba Touch");
    }

    private static void changeDimension(ServerPlayer player, ServerLevel to) {
        var from = player.level().dimension();
        player.setServerLevel(to);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, from, to.dimension()));
    }

    private static void near(double value, double expected, String message) {
        require(Math.abs(value - expected) < 0.0001, message + " (expected " + expected + ", got " + value + ")");
    }
}

package io.github.vitech.pinkkicore.test;

import io.github.vitech.pinkkicore.forge.dream.DreamEquipment;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.github.vitech.pinkkicore.test.GalgameProbe.near;
import static io.github.vitech.pinkkicore.test.GalgameProbe.require;

final class GalgameGearChecks {
    private static final String SOUL = "pinkkicore:galgame_soul";

    private GalgameGearChecks() {}

    static final class SilentGear {
        static void run(ServerLevel level) {
            var types = new net.silentchaos512.gear.api.item.GearType[]{
                net.silentchaos512.gear.api.item.GearType.SWORD, net.silentchaos512.gear.api.item.GearType.CHESTPLATE,
                net.silentchaos512.gear.api.item.GearType.BOW, net.silentchaos512.gear.api.item.GearType.CROSSBOW,
                net.silentchaos512.gear.api.item.GearType.SLINGSHOT, net.silentchaos512.gear.api.item.GearType.TRIDENT
            };
            var material = net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation(SOUL));
            require(material != null && material.getIngredient().test(new ItemStack(ModItems.GALGAME_SOUL.get())),
                "Silent Gear material ingredient");
            ItemStack sword = gear(SOUL, "sword", types[0], true);
            ItemStack thunder = gear("pinkkicore:thunder_guaba", "sword", types[0], true);
            require(sword.getMaxDamage() > thunder.getMaxDamage(), "Silent Gear durability improvement");
            melee(level, "Silent Gear", sword);
            armor(level, "Silent Gear", gear(SOUL, "chestplate", types[1], false));
            shoot(level, "Silent Gear bow", gear(SOUL, "bow", types[2], false), false);
            shoot(level, "Silent Gear crossbow", gear(SOUL, "crossbow", types[3], false), true);
            shoot(level, "Silent Gear slingshot", gear(SOUL, "slingshot", types[4], false), false);
            shoot(level, "Silent Gear trident", gear(SOUL, "trident", types[5], true), false);
            System.out.println("GALGAME_PROBE Silent Gear sword, chestplate, bow, crossbow, slingshot and trident");
        }

        private static ItemStack gear(String material, String name, net.silentchaos512.gear.api.item.GearType type,
                                      boolean withRod) {
            var instance = net.silentchaos512.gear.gear.material.MaterialInstance.of(
                net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation(material)));
            var main = net.silentchaos512.gear.api.part.PartType.MAIN.makeCompoundPart(type, List.of(instance)).orElseThrow();
            var gear = (net.silentchaos512.gear.api.item.ICoreItem) item("silentgear:" + name);
            if (withRod) {
                var rod = net.silentchaos512.gear.api.part.PartType.ROD.makeCompoundPart(type, List.of(instance)).orElseThrow();
                return gear.construct(List.of(main, rod));
            }
            return gear.construct(List.of(main));
        }
    }

    static final class Tinkers {
        static void run(ServerLevel level) {
            var id = new slimeknights.tconstruct.library.materials.definition.MaterialId(SOUL);
            var registry = slimeknights.tconstruct.library.materials.MaterialRegistry.getInstance();
            require(registry.getMaterial(id).isCraftable(), "Tinkers material not craftable");
            var mapping = slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache.findRecipe(
                new ItemStack(ModItems.GALGAME_SOUL.get()));
            require(mapping != null && mapping.getMaterial().getId().equals(id), "Tinkers ingredient mapping");
            plating(level);
            var sword = gear("sword", 3);
            require(sword.getMaxDamage() > 1200, "Tinkers durability improvement");
            melee(level, "Tinkers", sword);
            armor(level, "Tinkers", gear("plate_chestplate", 2));
            shoot(level, "Tinkers longbow", gear("longbow", 3), false);
            System.out.println("GALGAME_PROBE Tinkers sword, armor plating, chestplate and longbow");
        }

        private static void plating(ServerLevel level) {
            var pos = new BlockPos(9, GalgameProbe.CENTER.getY(), 11);
            var builder = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("tconstruct:part_builder")));
            level.setBlockAndUpdate(pos, builder.defaultBlockState());
            var bench = (slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity) level.getBlockEntity(pos);
            require(bench != null, "Tinkers part builder");
            bench.setItem(1, new ItemStack(item("tconstruct:pattern")));
            bench.setItem(0, new ItemStack(ModItems.GALGAME_SOUL.get(), 6));
            bench.selectRecipe(new slimeknights.tconstruct.library.recipe.partbuilder.Pattern(new ResourceLocation("tconstruct:chestplate_plating")));
            var plate = bench.calcResult(null);
            require(plate.is(item("tconstruct:chestplate_plating"))
                && ((slimeknights.tconstruct.library.tools.part.IMaterialItem) plate.getItem()).getMaterial(plate).getId().toString().equals(SOUL),
                "Tinkers armor plating recipe");
        }

        private static ItemStack gear(String name, int parts) {
            var material = slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(
                new slimeknights.tconstruct.library.materials.definition.MaterialId(SOUL));
            var materials = new slimeknights.tconstruct.library.materials.definition.IMaterial[parts];
            Arrays.fill(materials, material);
            if (name.equals("longbow")) {
                materials[2] = slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(
                    new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct:string"));
            }
            return slimeknights.tconstruct.library.tools.helper.ToolBuildHandler.buildItemFromMaterials(
                (slimeknights.tconstruct.library.tools.item.IModifiable) item("tconstruct:" + name),
                slimeknights.tconstruct.library.tools.nbt.MaterialNBT.of(materials));
        }
    }

    static final class Tetra {
        static void run(ServerLevel level) {
            var material = se.mickelus.tetra.data.DataManager.instance.materialData.getData(
                new ResourceLocation("tetra:metal/pinkkicore_galgame_soul"));
            require(material != null && material.material.getApplicableItemStacks()[0].is(ModItems.GALGAME_SOUL.get()),
                "Tetra material ingredient");
            var sword = new ItemStack(item("tetra:modular_sword"));
            apply(sword, "sword/basic_blade", "basic_blade/pinkkicore_galgame_soul");
            apply(sword, "sword/basic_hilt", "basic_hilt/pinkkicore_galgame_soul");
            assemble(sword, level);
            melee(level, "Tetra", sword);

            var bow = new ItemStack(item("tetra:modular_bow"));
            apply(bow, "bow/straight_stave", "straight_stave/pinkkicore_galgame_soul");
            apply(bow, "bow/basic_string", "basic_string/string");
            assemble(bow, level);
            shoot(level, "Tetra bow", bow, false);
            var knife = new ItemStack(item("tetra:modular_sword"));
            apply(knife, "sword/throwing_knife", "throwing_knife/pinkkicore_galgame_soul");
            apply(knife, "sword/basic_hilt", "basic_hilt/iron");
            assemble(knife, level);
            var user = GalgameProbe.player(level);
            require(level.addFreshEntity(user), "Tetra knife shooter fixture");
            var thrown = new se.mickelus.tetra.items.modular.ThrownModularItemEntity(level, user, knife);
            require(level.addFreshEntity(thrown), "Tetra knife projectile fixture");
            hit(level, "Tetra knife", user, thrown);
            System.out.println("GALGAME_PROBE Tetra sword, modular bow and thrown knife");
        }

        private static void apply(ItemStack item, String path, String variant) {
            var module = se.mickelus.tetra.module.ModuleRegistry.instance.getModule(new ResourceLocation("tetra:" + path));
            require(Arrays.stream(module.getVariantData()).anyMatch(entry -> entry.key.equals(variant)),
                "Tetra missing module variant " + variant);
            module.addModule(item, variant, null);
        }

        private static void assemble(ItemStack item, ServerLevel level) {
            ((se.mickelus.tetra.items.modular.IModularItem) item.getItem()).assemble(item, level, 0);
        }
    }

    private static Item item(String id) {
        return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)), id);
    }

    private static void melee(ServerLevel level, String host, ItemStack weapon) {
        require(DreamEquipment.containsGalgameSoul(weapon), host + " valid weapon material");
        var player = GalgameProbe.player(level);
        var effect = GuabaPotions.GALGAME.get();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.attack(target(level));
        require(!player.hasEffect(effect), host + " unrelated weapon applied Galgame");
        player.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        GalgameProbe.tick(player);
        near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), .108, host + " held material speed");
        player.attack(target(level));
        require(player.getEffect(effect) != null && player.getEffect(effect).getAmplifier() == 0
            && player.getEffect(effect).getDuration() == 200, host + " melee Galgame I");
    }

    private static void armor(ServerLevel level, String host, ItemStack armor) {
        require(DreamEquipment.containsGalgameSoul(armor), host + " valid armor material");
        var player = GalgameProbe.player(level);
        var effect = GuabaPotions.GALGAME.get();
        player.setItemSlot(EquipmentSlot.CHEST, armor);
        GalgameProbe.tick(player);
        require(player.hasEffect(effect), host + " armor did not grant Galgame");
        near(player.getMaxHealth(), 22, host + " armor health");
        near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), .1188, host + " armor and Galgame speed");
        for (int i = 0; i < 220; i++) {
            player.doTick();
        }
        require(player.hasEffect(effect), host + " armor did not renew Galgame");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        GalgameProbe.tick(player);
        near(player.getMaxHealth(), 20, host + " armor removed health");
        for (int i = 0; i < 200; i++) {
            player.doTick();
        }
        require(!player.hasEffect(effect), host + " removed armor renewed Galgame");
    }

    private static void shoot(ServerLevel level, String label, ItemStack weapon, boolean chargedCrossbow) {
        require(DreamEquipment.containsGalgameSoul(weapon), label + " valid ranged material");
        var user = GalgameProbe.player(level);
        user.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        user.setItemSlot(EquipmentSlot.OFFHAND,
            new ItemStack(label.endsWith("slingshot") ? item("silentgear:pebble") : Items.ARROW, 4));
        var origin = new AABB(user.position(), user.position()).inflate(12);
        require(level.addFreshEntity(user), label + " shooter fixture");
        if (chargedCrossbow) {
            ListTag ammo = new ListTag();
            ammo.add(new ItemStack(Items.ARROW).save(new CompoundTag()));
            weapon.getOrCreateTag().put("ChargedProjectiles", ammo);
            CrossbowItem.setCharged(weapon, true);
            weapon.use(level, user, InteractionHand.MAIN_HAND);
        } else {
            weapon.use(level, user, InteractionHand.MAIN_HAND);
            require(user.isUsingItem(), label + " could not start draw");
            for (int tick = 0; tick < 40; tick++) {
                user.doTick();
            }
            user.releaseUsingItem();
        }
        var arrows = level.getEntitiesOfClass(AbstractArrow.class, origin, arrow -> arrow.getOwner() == user);
        require(arrows.size() == 1, label + " post-launch arrow count: " + arrows.size());
        hit(level, label, user, arrows.get(0));
    }

    private static void hit(ServerLevel level, String label, ServerPlayer user, AbstractArrow arrow) {
        var effect = GuabaPotions.GALGAME.get();
        require(!user.hasEffect(effect), label + " applied Galgame before hit");
        user.stopUsingItem();
        user.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        var victim = target(level);
        victim.setPos(GalgameProbe.CENTER.getX() + .5, GalgameProbe.CENTER.getY(), GalgameProbe.CENTER.getZ() + .5);
        require(level.addFreshEntity(victim), label + " target fixture");
        arrow.setPos(victim.getX() - 1.1, victim.getY() + .6, victim.getZ());
        arrow.setDeltaMovement(new Vec3(1.6, 0, 0));
        arrow.tick();
        require(victim.getHealth() < victim.getMaxHealth(), label + " projectile did not hit");
        require(user.hasEffect(effect) && user.getEffect(effect).getAmplifier() == 0 && !victim.hasEffect(effect),
            label + " hit did not buff shooter after weapon swap");
        arrow.discard();
        victim.discard();
        level.removePlayerImmediately(user, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    private static LivingEntity target(ServerLevel level) {
        var target = EntityType.COW.create(level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80);
        target.setHealth(80);
        target.setNoAi(true);
        target.moveTo(5, GalgameProbe.CENTER.getY(), 7.5, 0, 0);
        return target;
    }
}

package io.github.vitech.pinkkicore.forge.dream;

import net.minecraft.world.item.ItemStack;
import net.silentchaos512.gear.api.item.ICoreItem;
import net.silentchaos512.gear.util.GearData;
import net.silentchaos512.gear.util.GearHelper;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.items.modular.IModularItem;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** The outer bridge can load without the three optional mods. Each nested class has a single host. */
final class DreamMaterials {
    private DreamMaterials() {}

    static int silentGear(ItemStack stack) {
        return SilentGear.effects(stack);
    }

    static int tinkersConstruct(ItemStack stack) {
        return TinkersConstruct.effects(stack);
    }

    static int tetra(ItemStack stack) {
        return Tetra.effects(stack);
    }

    private static final class SilentGear {
        static int effects(ItemStack stack) {
            if (!(stack.getItem() instanceof ICoreItem) || GearHelper.isBroken(stack)) {
                return 0;
            }
            int effects = 0;
            for (var part : GearData.getConstructionParts(stack)) {
                for (var material : part.getMaterials()) {
                    effects |= DreamEquipment.forMaterial(material.getId());
                }
            }
            return effects;
        }
    }

    private static final class TinkersConstruct {
        static int effects(ItemStack stack) {
            if (!(stack.getItem() instanceof IModifiable)) {
                return 0;
            }
            ToolStack tool = ToolStack.from(stack);
            if (tool.isBroken()) {
                return 0;
            }
            int effects = 0;
            var materials = tool.getMaterials();
            for (int index = 0; index < materials.size(); index++) {
                effects |= DreamEquipment.forMaterial(materials.get(index).getId().getId());
            }
            return effects;
        }
    }

    private static final class Tetra {
        private static final ItemEffect CLOUD = ItemEffect.get("pinkkicore_cloud_dream");
        private static final ItemEffect NETHER = ItemEffect.get("pinkkicore_nether_dream");

        static int effects(ItemStack stack) {
            if (!(stack.getItem() instanceof IModularItem item) || item.isBroken(stack)) {
                return 0;
            }
            int cloud = item.getEffectLevel(stack, CLOUD) > 0 ? DreamEquipment.CLOUD : 0;
            int nether = item.getEffectLevel(stack, NETHER) > 0 ? DreamEquipment.NETHER : 0;
            return cloud | nether;
        }
    }
}

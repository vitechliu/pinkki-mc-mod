package io.github.vitech.pinkkicore.forge.dream;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class DreamEquipment {
    static final int CLOUD = 1;
    static final int NETHER = 2;
    private static final UUID LIGHTWEIGHT = UUID.fromString("d5230124-d8d8-4cee-8daa-49a6e1a09535");
    private static final UUID NETHER_ATTACK = UUID.fromString("2a3a6b58-94a1-4663-b677-522ecec0440f");
    private static final UUID NETHER_ARMOR = UUID.fromString("453a383d-1d9e-45dc-a895-18a76b001a00");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private DreamEquipment() {}

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void equipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSlot() != EquipmentSlot.OFFHAND) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void dimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    private static void refresh(ServerPlayer player) {
        boolean active = player.isAlive() && !player.isSpectator();
        boolean inNether = active && player.level().dimension().equals(Level.NETHER);
        ItemStack mainHand = player.getMainHandItem();
        int held = active ? effects(mainHand) : 0;
        boolean lightweight = (held & CLOUD) != 0;
        double armorBonus = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            int worn = active ? effects(armor) : 0;
            lightweight |= (worn & CLOUD) != 0;
            if (inNether && (worn & NETHER) != 0 && contributes(armor, slot, Attributes.ARMOR)) {
                armorBonus += 2;
            }
        }
        boolean netherAttack = inNether && (held & NETHER) != 0
            && contributes(mainHand, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
        update(player.getAttribute(Attributes.MOVEMENT_SPEED), LIGHTWEIGHT, "pinkkicore.dream_lightweight",
            lightweight ? 0.08 : 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        update(player.getAttribute(Attributes.ATTACK_DAMAGE), NETHER_ATTACK, "pinkkicore.nether_dream_attack",
            netherAttack ? 2 : 0, AttributeModifier.Operation.ADDITION);
        update(player.getAttribute(Attributes.ARMOR), NETHER_ARMOR, "pinkkicore.nether_dream_armor",
            armorBonus, AttributeModifier.Operation.ADDITION);
    }

    private static boolean contributes(ItemStack stack, EquipmentSlot slot, Attribute attribute) {
        return stack.getAttributeModifiers(slot).get(attribute).stream().anyMatch(modifier -> modifier.getAmount() > 0);
    }

    private static void update(AttributeInstance attribute, UUID id, String name, double amount,
                               AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null) {
            if (existing.getAmount() == amount) {
                return;
            }
            attribute.removeModifier(id);
        }
        if (amount != 0) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount, operation));
        }
    }

    private static int effects(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int effects = 0;
        if (ModList.get().isLoaded("silentgear")) {
            effects |= DreamMaterials.silentGear(stack);
        }
        if (ModList.get().isLoaded("tconstruct")) {
            effects |= DreamMaterials.tinkersConstruct(stack);
        }
        if (ModList.get().isLoaded("tetra")) {
            effects |= DreamMaterials.tetra(stack);
        }
        return effects;
    }

    static int forMaterial(ResourceLocation id) {
        if (!id.getNamespace().equals(ModInfo.ID)) {
            return 0;
        }
        return switch (id.getPath()) {
            case "cloud_dream", "guaba_cloud_dream" -> CLOUD;
            case "nether_dream", "guaba_nether_dream" -> NETHER;
            default -> 0;
        };
    }
}

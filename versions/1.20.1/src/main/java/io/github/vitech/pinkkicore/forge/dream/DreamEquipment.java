package io.github.vitech.pinkkicore.forge.dream;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.galgame.GalgameEquipment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
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
    static final int GALGAME = 4;
    static final int DEMON = 8;
    private static final UUID GALGAME_SPEED = UUID.fromString("79596ebc-bc0f-49ad-8c2e-5a064f039c61");
    private static final UUID GALGAME_HEALTH = UUID.fromString("0e2b8b25-5b60-46e0-8e56-db09ca307d56");
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
        boolean galgameArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            int worn = active ? effects(armor) : 0;
            lightweight |= (worn & CLOUD) != 0;
            galgameArmor |= (worn & GALGAME) != 0 && contributes(armor, slot, Attributes.ARMOR);
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
        boolean galgameHeld = (held & GALGAME) != 0 && isWeapon(mainHand);
        update(player.getAttribute(Attributes.MOVEMENT_SPEED), GALGAME_SPEED, "pinkkicore.galgame_soul_speed",
            galgameArmor || galgameHeld ? 0.08 : 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        boolean removingHealth = !galgameArmor && health != null && health.getModifier(GALGAME_HEALTH) != null;
        update(health, GALGAME_HEALTH, "pinkkicore.galgame_soul_health",
            galgameArmor ? 2 : 0, AttributeModifier.Operation.ADDITION);
        if (removingHealth && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (galgameArmor) {
            GalgameEquipment.refreshArmor(player);
        }
    }

    public static boolean containsGalgameSoul(ItemStack stack) {
        return (effects(stack) & GALGAME) != 0;
    }

    public static boolean qualifiesAsGalgameWeapon(ItemStack stack) {
        return isWeapon(stack) && containsGalgameSoul(stack);
    }

    public static boolean containsDemonSoul(ItemStack stack) {
        return (effects(stack) & DEMON) != 0;
    }

    public static boolean qualifiesAsDemonWeapon(ItemStack stack) {
        return isWeapon(stack) && containsDemonSoul(stack);
    }

    public static boolean wearsDemonArmor(net.minecraft.world.entity.LivingEntity wearer) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = wearer.getItemBySlot(slot);
            if (contributes(armor, slot, Attributes.ARMOR) && containsDemonSoul(armor)) return true;
        }
        return false;
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem
            || contributes(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
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
            case "galgame_soul" -> GALGAME;
            case "demon_soul" -> DEMON;
            default -> 0;
        };
    }
}

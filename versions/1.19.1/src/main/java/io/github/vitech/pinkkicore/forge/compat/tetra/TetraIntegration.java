package io.github.vitech.pinkkicore.forge.compat.tetra;

import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.items.modular.IModularItem;

/** Tetra 5 has material effects but no JSON item-effect outcomes. Loaded only with Tetra. */
public final class TetraIntegration {
    private TetraIntegration() {}
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, TetraIntegration::hit);
        MinecraftForge.EVENT_BUS.addListener(TetraIntegration::attributes);
    }
    private static void attributes(net.minecraftforge.event.ItemAttributeModifierEvent event) {
        if (event.getSlotType() != net.minecraft.world.entity.EquipmentSlot.OFFHAND
                || !(event.getItemStack().getItem() instanceof IModularItem item)) return;
        var speed = net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
        // Tetra 5 only exposes a sword's material attributes in the main hand.
        // Keep the thunder material's offhand movement behavior from the main target.
        if (!event.getModifiers().get(speed).isEmpty()) return;
        for (var modifier : item.getAttributeModifiersCollapsed(event.getItemStack()).get(speed)) {
            var id = java.util.UUID.nameUUIDFromBytes((modifier.getId()+":offhand").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            event.addModifier(speed, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                id, modifier.getName(), modifier.getAmount(), modifier.getOperation()));
        }
    }
    private static void hit(LivingDamageEvent event) {
        if (event.getEntity().level.isClientSide || event.getAmount() <= 0
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker) return;
        var stack = attacker.getMainHandItem();
        if (!(stack.getItem() instanceof IModularItem item)) return;
        if (item.getEffectLevel(stack, ItemEffect.get("pinkkicore_guaba_touch")) > 0)
            event.getEntity().addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(), 200, 0), attacker);
        if (item.getEffectLevel(stack, ItemEffect.get("pinkkicore_thunder_guaba_touch")) > 0)
            event.getEntity().addEffect(new MobEffectInstance(GuabaPotions.THUNDER_GUABA.get(), 200, 0), attacker);
    }
}

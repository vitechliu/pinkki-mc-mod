package io.github.vitech.pinkkicore.forge.effect;

import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Server-authoritative, independent rolls; transient modifiers never alter base attributes. */
public final class GuabaEffect extends MobEffect {
    private static final UUID SPEED = UUID.fromString("be04e213-58c6-463e-aad1-bbc1d078da60");
    private static final UUID ARMOR = UUID.fromString("925328ed-cc88-47e1-87dc-28d29fe14e75");
    private static final String NEXT_ROLL = "pinkkicore:guaba_next_roll";

    public GuabaEffect() { super(MobEffectCategory.BENEFICIAL, 0xA5AD35); }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) { return true; }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        var armor = entity.getAttribute(Attributes.ARMOR);
        // Missing transient modifiers also handles reloading an entity with a saved effect.
        if ((speed != null && speed.getModifier(SPEED) == null)
                || (armor != null && armor.getModifier(ARMOR) == null)
                || entity.level().getGameTime() >= entity.getPersistentData().getLong(NEXT_ROLL)) {
            roll(entity, amplifier);
        }
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        if (!entity.level().isClientSide) roll(entity, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        if (entity.level().isClientSide) return;
        remove(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED);
        remove(entity.getAttribute(Attributes.ARMOR), ARMOR);
        entity.getPersistentData().remove(NEXT_ROLL);
    }

    private static void remove(AttributeInstance attribute, UUID id) {
        if (attribute != null) attribute.removeModifier(id);
    }

    private static void replace(AttributeInstance attribute, UUID id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, "Guaba fluctuation", amount, operation));
    }

    private static void roll(LivingEntity entity, int amplifier) {
        var levels = GuabaPotions.SPEC.getAsJsonArray("levels");
        var bounds = levels.get(Math.max(0, Math.min(3, amplifier))).getAsJsonObject();
        double speedMin = bounds.get("speed_min").getAsDouble();
        double armorMin = bounds.get("armor_min").getAsDouble();
        double speed = speedMin + entity.getRandom().nextDouble() * (bounds.get("speed_max").getAsDouble() - speedMin);
        double armor = armorMin + entity.getRandom().nextDouble() * (bounds.get("armor_max").getAsDouble() - armorMin);
        replace(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, speed - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        replace(entity.getAttribute(Attributes.ARMOR), ARMOR, armor, AttributeModifier.Operation.ADDITION);
        entity.getPersistentData().putLong(NEXT_ROLL, entity.level().getGameTime() + 20);
    }
}

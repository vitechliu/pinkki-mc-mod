package io.github.vitech.pinkkicore.neoforge.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Server-authoritative, independent rolls; transient modifiers never alter base attributes. */
public class GuabaEffect extends MobEffect implements GuabaEffectFamily {
    private static final ResourceLocation GUABA_SPEED = ResourceLocation.parse("pinkkicore:effect_be04e213-58c6-463e-aad1-bbc1d078da60");
    private static final ResourceLocation GUABA_ARMOR = ResourceLocation.parse("pinkkicore:effect_925328ed-cc88-47e1-87dc-28d29fe14e75");
    private final ResourceLocation speedId;
    private final ResourceLocation armorId;
    private final String nextRoll;
    private final double amplitudeMultiplier;

    public GuabaEffect() {
        this(GUABA_SPEED, GUABA_ARMOR, "pinkkicore:guaba_next_roll", 0xA5AD35, 1);
    }

    protected GuabaEffect(ResourceLocation speedId, ResourceLocation armorId, String nextRoll, int color, double amplitudeMultiplier) {
        super(MobEffectCategory.NEUTRAL, color);
        this.speedId = speedId;
        this.armorId = armorId;
        this.nextRoll = nextRoll;
        this.amplitudeMultiplier = amplitudeMultiplier;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;
        var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        var armor = entity.getAttribute(Attributes.ARMOR);
        // Missing transient modifiers also handles reloading an entity with a saved effect.
        if ((speed != null && speed.getModifier(speedId) == null)
                || (armor != null && armor.getModifier(armorId) == null)
                || entity.level().getGameTime() >= entity.getPersistentData().getLong(nextRoll)) {
            roll(entity, amplifier);
        }
        return true;
    }

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) roll(entity, amplifier);
    }

    @Override public void removeAttributeModifiers(AttributeMap attributes) {
        remove(attributes.getInstance(Attributes.MOVEMENT_SPEED), speedId);
        remove(attributes.getInstance(Attributes.ARMOR), armorId);
    }

    private static void remove(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) attribute.removeModifier(id);
    }

    private static void replace(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private void roll(LivingEntity entity, int amplifier) {
        var levels = GuabaPotions.SPEC.getAsJsonArray("levels");
        var bounds = levels.get(Math.max(0, Math.min(3, amplifier))).getAsJsonObject();
        double speedMin = bounds.get("speed_min").getAsDouble();
        double armorMin = bounds.get("armor_min").getAsDouble();
        double speed = speedMin + entity.getRandom().nextDouble() * (bounds.get("speed_max").getAsDouble() - speedMin);
        double armor = armorMin + entity.getRandom().nextDouble() * (bounds.get("armor_max").getAsDouble() - armorMin);
        replace(entity.getAttribute(Attributes.MOVEMENT_SPEED), speedId, (speed - 1) * amplitudeMultiplier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        replace(entity.getAttribute(Attributes.ARMOR), armorId, armor * amplitudeMultiplier, AttributeModifier.Operation.ADD_VALUE);
        entity.getPersistentData().putLong(nextRoll, entity.level().getGameTime() + 20);
    }
}

package io.github.vitech.pinkkicore.neoforge.effect;

import net.minecraft.resources.ResourceLocation;
import io.github.vitech.pinkkicore.core.ModInfo;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

/** Uses an independent clock and UUIDs so ordinary Guaba and Thunder Guaba can coexist. */
@EventBusSubscriber(modid = ModInfo.ID, bus = EventBusSubscriber.Bus.GAME)
public final class ThunderGuabaEffect extends GuabaEffect {
    private static final String NEXT_LIGHTNING = "pinkkicore:thunder_guaba_next_lightning";

    public ThunderGuabaEffect() {
        super(ResourceLocation.parse("pinkkicore:effect_ec4f8a15-849c-48cb-b85b-13ca786b08ca"),
                ResourceLocation.parse("pinkkicore:effect_0cdd520a-ee1f-4907-91dc-a98037b8a014"),
                "pinkkicore:thunder_guaba_next_roll", 0x92D7F4,
                GuabaPotions.SPEC.getAsJsonObject("thunder").get("amplitude_multiplier").getAsDouble());
    }

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);
        if (!entity.level().isClientSide && !entity.getPersistentData().contains(NEXT_LIGHTNING)) schedule(entity);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return true;
        if (!entity.getPersistentData().contains(NEXT_LIGHTNING)) schedule(entity);
        if (level.getGameTime() >= entity.getPersistentData().getLong(NEXT_LIGHTNING)) {
            var lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(entity.position());
                level.addFreshEntity(lightning);
            }
            schedule(entity);
        }
        return true;
    }

    // Remove/Expired events distinguish actual removal from attribute replacement during upgrades.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect().value() instanceof ThunderGuabaEffect && !event.getEntity().level().isClientSide)
            event.getEntity().getPersistentData().remove(NEXT_LIGHTNING);
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect().value() instanceof ThunderGuabaEffect
                && !event.getEntity().level().isClientSide)
            event.getEntity().getPersistentData().remove(NEXT_LIGHTNING);
    }

    private static void schedule(LivingEntity entity) {
        var spec = GuabaPotions.SPEC.getAsJsonObject("thunder");
        int min = spec.get("lightning_min_ticks").getAsInt();
        int max = spec.get("lightning_max_ticks").getAsInt();
        entity.getPersistentData().putLong(NEXT_LIGHTNING,
                entity.level().getGameTime() + min + entity.getRandom().nextInt(max - min + 1));
    }
}

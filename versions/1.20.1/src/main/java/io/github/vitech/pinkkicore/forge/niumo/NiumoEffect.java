package io.github.vitech.pinkkicore.forge.niumo;

import io.github.vitech.pinkkicore.forge.compat.curios.CuriosIntegration;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.ModList;

public final class NiumoEffect extends InstantenousMobEffect {
    public NiumoEffect() {
        super(MobEffectCategory.HARMFUL, 0x72452F);
    }

    @Override public void applyInstantenousEffect(Entity direct, Entity source, LivingEntity target,
                                                    int amplifier, double strength) {
        if (strength > 0) apply(target);
    }

    @Override public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override public void applyEffectTick(LivingEntity target, int amplifier) {
        apply(target);
    }

    public static void apply(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive()
            || (ModList.get().isLoaded("curios") && CuriosIntegration.wearingNiumoHelmet(target))) {
            return;
        }
        for (var active : List.copyOf(target.getActiveEffects())) {
            if (active.getEffect().isBeneficial()) target.removeEffect(active.getEffect());
        }
        if (target.getType() == EntityType.COW && target instanceof Cow cow) convert(level, cow);
    }

    private static void convert(ServerLevel level, Cow cow) {
        var type = ModEntities.BULLMAN.get();
        var bull = type.create(level);
        if (bull == null) return;
        bull.moveTo(cow.getX(), cow.getY(), cow.getZ(), cow.getYRot(), cow.getXRot());
        if (!level.noCollision(cow, bull.getBoundingBox())
            || !ForgeEventFactory.canLivingConvert(cow, type, ignored -> {})) return;
        if (cow.hasCustomName()) {
            bull.setCustomName(cow.getCustomName());
            bull.setCustomNameVisible(cow.isCustomNameVisible());
        }
        if (cow.isPersistenceRequired() || cow.hasCustomName()) bull.setPersistenceRequired();
        if (!level.addFreshEntity(bull)) return;
        ForgeEventFactory.onLivingConvert(cow, bull);
        cow.discard();
        level.sendParticles(ParticleTypes.POOF, bull.getX(), bull.getY() + 1, bull.getZ(),
            18, 0.35, 0.55, 0.35, 0.02);
        level.playSound(null, bull.blockPosition(), SoundEvents.COW_AMBIENT, SoundSource.HOSTILE, 1, 1);
    }
}

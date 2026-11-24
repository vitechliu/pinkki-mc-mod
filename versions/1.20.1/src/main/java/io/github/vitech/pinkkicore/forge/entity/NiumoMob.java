package io.github.vitech.pinkkicore.forge.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class NiumoMob extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.niumo.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.niumo.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.niumo.attack");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    protected NiumoMob(EntityType<? extends NiumoMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes(boolean bull) {
        return createMobAttributes().add(Attributes.FOLLOW_RANGE, 35)
            .add(Attributes.MOVEMENT_SPEED, bull ? 0.23 : 0.46)
            .add(Attributes.ATTACK_DAMAGE, 3);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        if (this instanceof Bullman) {
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        }
    }

    @Override public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || !canAttack(living)) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("action", "attack");
        return hit;
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4,
            state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "action", 0,
            state -> software.bernie.geckolib.core.object.PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public static final class Bullman extends NiumoMob implements Enemy {
        public Bullman(EntityType<? extends NiumoMob> type, Level level) { super(type, level); }
        @Override protected boolean shouldDespawnInPeaceful() { return true; }
        @Override public SoundSource getSoundSource() { return SoundSource.HOSTILE; }
        @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.COW_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.COW_DEATH; }
    }

    public static final class LaoCai extends NiumoMob {
        public LaoCai(EntityType<? extends NiumoMob> type, Level level) { super(type, level); }

        @Override public boolean canAttackType(EntityType<?> type) {
            return type != EntityType.IRON_GOLEM && type != EntityType.VILLAGER
                && type != EntityType.WANDERING_TRADER && super.canAttackType(type);
        }

        @Override public boolean canAttack(LivingEntity target) {
            return !(target instanceof AbstractVillager) && !(target instanceof IronGolem) && super.canAttack(target);
        }

        @Override public void setTarget(LivingEntity target) {
            if (target == null || canAttack(target)) super.setTarget(target);
        }

        @Override protected void customServerAiStep() {
            var target = getTarget();
            double range = getAttributeValue(Attributes.FOLLOW_RANGE);
            if (target != null && (!canAttack(target) || target.level() != level() || distanceToSqr(target) > range * range)) {
                setTarget(null);
            }
            super.customServerAiStep();
        }
    }
}

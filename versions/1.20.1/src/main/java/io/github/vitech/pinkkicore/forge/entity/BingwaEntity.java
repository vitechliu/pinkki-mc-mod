package io.github.vitech.pinkkicore.forge.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Neutral amphibian. Only HurtByTargetGoal acquires enemies; no natural spawn registration. */
public class BingwaEntity extends PathfinderMob implements GeoEntity {
    private static final double TONGUE_REACH_SQR = 6.25;
    private static final RawAnimation IDLE = loop("idle"), WALK = loop("walk"),
        SWIM = loop("swim"), FLOAT = loop("float"), JUMP = loop("jump");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity tongueTarget;
    private int attackTicks;
    private int hopCooldown = 40;
    private int croakCooldown = 200;

    public BingwaEntity(EntityType<? extends BingwaEntity> type, Level level) {
        super(type, level);
        moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.5F, false);
        setPathfindingMalus(BlockPathTypes.WATER, 0);
        setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0);
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20)
            .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 3)
            .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true) {
            @Override protected double getAttackReachSqr(LivingEntity target) {
                return TONGUE_REACH_SQR;
            }
        });
        goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1, 40));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override public void travel(Vec3 input) {
        if (isEffectiveAi() && isInWater()) {
            moveRelative(0.04F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            if (getTarget() == null) setDeltaMovement(getDeltaMovement().add(0, -0.002, 0));
        } else {
            super.travel(input);
        }
    }

    @Override public boolean doHurtTarget(Entity target) {
        if (attackTicks > 0 || !(target instanceof LivingEntity living)) return false;
        tongueTarget = living;
        attackTicks = 16;
        triggerAnim("action", "tongue");
        playSound(SoundEvents.FROG_TONGUE, 0.8F, 1);
        return true;
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isNoAi() || !isAlive()) return;
        // Damage is authoritative on the server, after the tongue animation's wind-up.
        if (attackTicks > 0) {
            attackTicks--;
            if (attackTicks == 10 && tongueTarget != null && tongueTarget == getTarget()
                    && tongueTarget.isAlive() && canAttack(tongueTarget)
                    && distanceToSqr(tongueTarget) <= TONGUE_REACH_SQR && hasLineOfSight(tongueTarget)) {
                super.doHurtTarget(tongueTarget);
            }
            if (attackTicks == 0) tongueTarget = null;
        }
        if (--hopCooldown <= 0 && onGround() && !isInWater() && !navigation.isDone() && attackTicks == 0) {
            jumpFromGround();
            playSound(SoundEvents.FROG_LONG_JUMP, 0.5F, 1);
            hopCooldown = 60 + random.nextInt(60);
        }
        if (--croakCooldown <= 0 && onGround() && getTarget() == null && navigation.isDone() && attackTicks == 0) {
            triggerAnim("action", "croak");
            playSound(SoundEvents.FROG_AMBIENT, 0.6F, 1);
            croakCooldown = 200 + random.nextInt(200);
        }
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.FROG_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.FROG_DEATH; }

    private static RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop("animation.bingwa." + name);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BingwaEntity>(this, "movement", 3, state -> {
            RawAnimation motion = isInWater() ? (state.isMoving() ? SWIM : FLOAT)
                : (!onGround() ? JUMP : (state.isMoving() ? WALK : IDLE));
            return state.setAndContinue(motion);
        }));
        controllers.add(new AnimationController<BingwaEntity>(this, "action", 0, state -> PlayState.STOP)
            .triggerableAnim("tongue", RawAnimation.begin().thenPlay("animation.bingwa.tongue"))
            .triggerableAnim("croak", RawAnimation.begin().thenPlay("animation.bingwa.croak")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

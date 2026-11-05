package io.github.vitech.pinkkicore.forge.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-authoritative short flame stream. Particles never place fire or cause damage themselves. */
public class GuabaDragonEntity extends Monster implements GeoEntity {
    public static final int BREATH_WINDUP = 20, BREATH_DURATION = 60, BREATH_RECOVERY = 16;
    public static final int BREATH_TOTAL = BREATH_WINDUP + BREATH_DURATION + BREATH_RECOVERY;
    public static final int BREATH_COOLDOWN = 160;
    public static final double BREATH_RANGE = 6;
    private static final EntityDataAccessor<Integer> BREATH_TICKS = SynchedEntityData.defineId(
        GuabaDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MELEE_TICKS = SynchedEntityData.defineId(
        GuabaDragonEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation MELEE = RawAnimation.begin().thenPlay("animation.guaba_dragon.melee");
    private float breathYaw;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guaba_dragon.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guaba_dragon.walk");
    private static final RawAnimation BREATH = RawAnimation.begin().thenPlay("animation.guaba_dragon.breath_pose");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int breathCooldown = 40;
    private LivingEntity breathTarget;
    private Vec3 breathDirection = new Vec3(0, 0, 1);

    public GuabaDragonEntity(EntityType<? extends GuabaDragonEntity> type, Level level) {
        super(type, level);
        setCustomName(type.getDescription());
        setCustomNameVisible(true);
        setMaxUpStep(1);
        xpReward = 5;
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BREATH_TICKS, 0);
        entityData.define(MELEE_TICKS, 0);
    }

    public int getBreathTicks() { return entityData.get(BREATH_TICKS); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30)
            .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.ATTACK_DAMAGE, 4)
            .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.KNOCKBACK_RESISTANCE, 0.25);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FlameGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false) {
            @Override protected double getAttackReachSqr(LivingEntity target) { return 6.25; }
        });
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private boolean validVictim(LivingEntity victim) {
        return victim != null && victim.isAlive() && !victim.isRemoved() && canAttack(victim)
            && (!(victim instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    @Override public boolean doHurtTarget(Entity victim) {
        if (getBreathTicks() > 0) return false;
        boolean hit = super.doHurtTarget(victim);
        if (hit) entityData.set(MELEE_TICKS, 9);
        return hit;
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server)) return;
        if (entityData.get(MELEE_TICKS) > 0) entityData.set(MELEE_TICKS, entityData.get(MELEE_TICKS) - 1);
        if (breathCooldown > 0) breathCooldown--;
        int remaining = getBreathTicks();
        if (remaining == 0) return;
        if (isNoAi() || !isAlive() || !validVictim(breathTarget) || getTarget() != breathTarget
                || level().getDifficulty() == Difficulty.PEACEFUL) {
            finishBreath();
            return;
        }
        navigation.stop();
        setYRot(breathYaw);
        setYHeadRot(breathYaw);
        setYBodyRot(breathYaw);
        int elapsed = BREATH_TOTAL - remaining;
        // Aim once after wind-up: the stream is dodgeable during its three active seconds.
        if (elapsed == BREATH_WINDUP) {
            Vec3 mouth = getBreathOrigin();
            breathDirection = breathTarget.getBoundingBox().getCenter().subtract(mouth).normalize();
        }
        if (elapsed >= BREATH_WINDUP && elapsed < BREATH_WINDUP + BREATH_DURATION) {
            emitFlame(server, (elapsed - BREATH_WINDUP) % 20 == 0);
        }
        entityData.set(BREATH_TICKS, remaining - 1);
        if (remaining == 1) finishBreath();
    }

    private Vec3 forward() {
        return Vec3.directionFromRotation(0, getYRot());
    }

    public Vec3 getBreathOrigin() {
        return position().add(0, 1.95, 0).add(forward().scale(1.5));
    }

    private void emitFlame(ServerLevel server, boolean damageTick) {
        Vec3 start = getBreathOrigin();
        // Reach is measured from the mob, including the forward mouth offset.
        Vec3 end = start.add(breathDirection.scale(BREATH_RANGE - 1.5));
        // Include the throat-to-mouth segment: a wall next to the head cannot be bypassed.
        Vec3 throat = position().add(0, 1.95, 0);
        HitResult obstruction = server.clip(new ClipContext(throat, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, this));
        if (obstruction.getType() != HitResult.Type.MISS) {
            if (obstruction.getLocation().distanceToSqr(throat) <= start.distanceToSqr(throat)) return;
            end = obstruction.getLocation();
        }
        Vec3 stream = end.subtract(start);
        double length = stream.length();
        Vec3 direction = stream.normalize();
        for (double step = 0; step < length; step += 0.45) {
            Vec3 point = start.add(direction.scale(step));
            double spread = 0.06 + step * 0.035;
            server.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 2, spread, spread, spread, 0.01);
        }
        if (!damageTick) return;
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.4),
                entity -> entity != this && !(entity instanceof GuabaDragonEntity) && validVictim(entity))) {
            if (distanceToSqr(victim) <= BREATH_RANGE * BREATH_RANGE && hasLineOfSight(victim)
                    && (victim.getBoundingBox().inflate(0.35).contains(start)
                        || victim.getBoundingBox().inflate(0.35).clip(start, end).isPresent())) {
                victim.hurt(damageSources().inFire(), 2);
            }
        }
    }

    private void finishBreath() {
        entityData.set(BREATH_TICKS, 0);
        breathTarget = null;
        breathCooldown = BREATH_COOLDOWN;
    }

    private final class FlameGoal extends Goal {
        FlameGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            LivingEntity target = getTarget();
            return breathCooldown == 0 && validVictim(target) && !isInWater()
                && distanceToSqr(target) >= 9 && distanceToSqr(target) <= BREATH_RANGE * BREATH_RANGE
                && hasLineOfSight(target);
        }
        @Override public boolean canContinueToUse() {
            return getBreathTicks() > 0 && validVictim(breathTarget) && getTarget() == breathTarget && !isInWater();
        }
        @Override public void start() {
            breathTarget = getTarget();
            navigation.stop();
            // Lock horizontal facing to keep the mouth and server particle origin aligned.
            Vec3 delta = breathTarget.position().subtract(position());
            setYRot((float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90));
            breathYaw = getYRot();
            setYHeadRot(breathYaw);
            setYBodyRot(breathYaw);
            entityData.set(BREATH_TICKS, BREATH_TOTAL);
            playSound(SoundEvents.BLAZE_SHOOT, 0.8F, 0.7F);
        }
        @Override public void tick() {
            navigation.stop();
            setYRot(breathYaw);
            setYHeadRot(breathYaw);
            setYBodyRot(breathYaw);
        }
        @Override public void stop() { if (getBreathTicks() > 0) finishBreath(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
    }

    public static boolean canSpawn(EntityType<GuabaDragonEntity> type, ServerLevelAccessor level,
                                   MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        boolean nether = level.getLevel().dimension() == Level.NETHER && level.getBiome(pos).is(BiomeTags.IS_NETHER);
        boolean badlands = level.getLevel().dimension() == Level.OVERWORLD && level.getBiome(pos).is(BiomeTags.IS_BADLANDS);
        return (nether || badlands) && (nether || Monster.isDarkEnoughToSpawn(level, pos, random))
            && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GuabaBreathCooldown", Math.max(breathCooldown, getBreathTicks() > 0 ? BREATH_COOLDOWN : 0));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("CustomName", 8)) setCustomName(getType().getDescription());
        if (!tag.contains("CustomNameVisible", 1)) setCustomNameVisible(true);
        entityData.set(BREATH_TICKS, 0);
        breathTarget = null;
        entityData.set(MELEE_TICKS, 0);
        breathCooldown = Math.max(40, Math.min(BREATH_COOLDOWN, tag.getInt("GuabaBreathCooldown")));
    }

    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }
    @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable() {
        return net.minecraft.world.level.storage.loot.BuiltInLootTables.EMPTY;
    }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GuabaDragonEntity>(this, "body", 0, state ->
            state.setAndContinue(getBreathTicks() > 0 ? BREATH : entityData.get(MELEE_TICKS) > 0 ? MELEE : state.isMoving() ? WALK : IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

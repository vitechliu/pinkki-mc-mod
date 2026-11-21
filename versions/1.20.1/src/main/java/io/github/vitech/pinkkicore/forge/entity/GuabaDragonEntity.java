package io.github.vitech.pinkkicore.forge.entity;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-authoritative short flame stream. Particles never place fire or cause damage themselves. */
public class GuabaDragonEntity extends Monster implements GeoEntity {
    public static final int BREATH_WINDUP = GuabaDragonBreath.WINDUP, BREATH_DURATION = GuabaDragonBreath.DURATION,
        BREATH_RECOVERY = GuabaDragonBreath.RECOVERY;
    public static final int BREATH_TOTAL = BREATH_WINDUP + BREATH_DURATION + BREATH_RECOVERY;
    public static final int BREATH_COOLDOWN = GuabaDragonBreath.COOLDOWN;
    public static final double BREATH_RANGE = GuabaDragonBreath.RANGE;
    private static final EntityDataAccessor<Integer> BREATH_TICKS = SynchedEntityData.defineId(
        GuabaDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MELEE_TICKS = SynchedEntityData.defineId(
        GuabaDragonEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation MELEE = RawAnimation.begin().thenPlay("animation.guaba_dragon.melee");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guaba_dragon.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guaba_dragon.walk");
    private static final RawAnimation BREATH = RawAnimation.begin().thenPlay("animation.guaba_dragon.breath_pose");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private GuabaDragonBreath breath;

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
        breath = new GuabaDragonBreath(this, BREATH_TICKS, ParticleTypes.FLAME, SoundEvents.BLAZE_SHOOT) {
            @Override boolean validTarget(LivingEntity victim) {
                return level().getDifficulty() != Difficulty.PEACEFUL && validVictim(victim);
            }
            @Override boolean canHit(LivingEntity victim, LivingEntity target) {
                return !(victim instanceof GuabaDragonEntity) && validVictim(victim);
            }
            @Override void damage(LivingEntity victim) { victim.hurt(damageSources().inFire(), 2); }
        };
        goalSelector.addGoal(1, breath);
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
        if (breath != null) breath.serverStep(server);
    }

    public Vec3 getBreathOrigin() { return GuabaDragonBreath.origin(this); }

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
        if (breath != null) breath.save(tag);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("CustomName", 8)) setCustomName(getType().getDescription());
        if (!tag.contains("CustomNameVisible", 1)) setCustomNameVisible(true);
        if (breath != null) breath.load(tag);
        entityData.set(MELEE_TICKS, 0);
    }

    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GuabaDragonEntity>(this, "body", 0, state ->
            state.setAndContinue(getBreathTicks() > 0 ? BREATH : entityData.get(MELEE_TICKS) > 0 ? MELEE : state.isMoving() ? WALK : IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

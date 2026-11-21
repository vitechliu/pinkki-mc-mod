package io.github.vitech.pinkkicore.forge.entity;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class PetGuabaDragonEntity extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(
        PetGuabaDragonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> BREATH_TICKS = SynchedEntityData.defineId(
        PetGuabaDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MELEE_TICKS = SynchedEntityData.defineId(
        PetGuabaDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(
        PetGuabaDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guaba_dragon.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guaba_dragon.walk");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.guaba_dragon.melee");
    private static final RawAnimation BREATH = RawAnimation.begin().thenPlay("animation.guaba_dragon.breath_pose");
    private static final RawAnimation FLIGHT = RawAnimation.begin().thenLoop("animation.guaba_dragon.flight");
    private static final ResourceKey<DamageType> IRON_LIGHTNING = ResourceKey.create(Registries.DAMAGE_TYPE,
        new ResourceLocation("irons_spellbooks", "lightning_magic"));
    private static final AttributeModifier NETHER_BONUS = new AttributeModifier(
        UUID.fromString("bd3d8ca8-e67c-49f4-b6b3-c248cae75361"), "Nether Guaba dragon",
        2, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final double BITE_REACH_SQUARED = 6.25;
    private static final int FLIGHT_DURATION = 40;
    private static final int FLIGHT_COOLDOWN = 60;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private GuabaDragonBreath breath;
    private CloudPursuitGoal cloudPursuit;
    private int flightCooldown;

    public PetGuabaDragonEntity(EntityType<? extends PetGuabaDragonEntity> type, Level level) {
        super(type, level);
        setCustomNameVisible(true);
        setMaxUpStep(1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 30)
            .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.ATTACK_DAMAGE, 4)
            .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.KNOCKBACK_RESISTANCE, 0.25);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, GuabaDragonVariant.NORMAL.id());
        entityData.define(BREATH_TICKS, 0);
        entityData.define(MELEE_TICKS, 0);
        entityData.define(FLYING, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        cloudPursuit = new CloudPursuitGoal();
        goalSelector.addGoal(1, cloudPursuit);
        breath = new GuabaDragonBreath(this, BREATH_TICKS, ParticleTypes.FLAME, SoundEvents.BLAZE_SHOOT) {
            @Override boolean validTarget(LivingEntity victim) { return isCombatTarget(victim); }
            @Override boolean canHit(LivingEntity victim, LivingEntity lockedTarget) { return victim == lockedTarget; }
            @Override ParticleOptions particle() { return isThunder() ? ParticleTypes.ELECTRIC_SPARK : super.particle(); }
            @Override SoundEvent sound() { return isThunder() ? SoundEvents.TRIDENT_THUNDER : super.sound(); }
            @Override void damage(LivingEntity victim) {
                DamageSource source = isThunder() ? lightningDamage() : new DamageSource(damageSources().inFire().typeHolder(),
                    PetGuabaDragonEntity.this);
                victim.hurt(source, attackDamage() / 2);
            }
        };
        goalSelector.addGoal(2, breath);
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false) {
            @Override public boolean canUse() { return isCombatTarget(getTarget()) && super.canUse(); }
            @Override public boolean canContinueToUse() { return isCombatTarget(getTarget()) && super.canContinueToUse(); }
            @Override protected double getAttackReachSqr(LivingEntity target) { return BITE_REACH_SQUARED; }
        });
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1, 8, 3, false));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    public void initializePet(Player owner, GuabaDragonVariant variant) {
        entityData.set(VARIANT, variant.id());
        setOwnerUUID(owner.getUUID());
        setTame(true);
        setPersistenceRequired();
        if (variant == GuabaDragonVariant.PINK) getAttribute(Attributes.MAX_HEALTH).setBaseValue(120);
        if (variant == GuabaDragonVariant.THUNDER) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.69);
        if (variant == GuabaDragonVariant.CLOUD) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.46);
        updateNetherBonus();
        setHealth(getMaxHealth());
    }

    public GuabaDragonVariant getVariant() { return GuabaDragonVariant.byId(entityData.get(VARIANT)); }
    public boolean isFlying() { return entityData.get(FLYING); }
    public int getBreathTicks() { return entityData.get(BREATH_TICKS); }

    @Override protected Component getTypeName() { return getVariant().displayName(); }

    private boolean isThunder() { return getVariant() == GuabaDragonVariant.THUNDER; }
    private float attackDamage() { return (float)getAttributeValue(Attributes.ATTACK_DAMAGE); }

    private DamageSource lightningDamage() {
        return level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(IRON_LIGHTNING)
            .map(type -> new DamageSource(type, this))
            .orElseGet(() -> new DamageSource(damageSources().lightningBolt().typeHolder(), this));
    }

    private boolean isCombatTarget(LivingEntity victim) {
        LivingEntity owner = getOwner();
        return owner != null && owner.isAlive() && !owner.isSpectator()
            && victim != null && victim != this && !(victim instanceof Player) && victim.isAlive()
            && canAttack(victim) && !isAlliedTo(victim) && !owner.isAlliedTo(victim)
            && (!(victim instanceof OwnableEntity pet) || !getOwnerUUID().equals(pet.getOwnerUUID()));
    }

    @Override public boolean wantsToAttack(LivingEntity target, LivingEntity owner) { return isCombatTarget(target); }

    @Override public boolean doHurtTarget(Entity victim) {
        if (level().isClientSide || getBreathTicks() > 0 || !(victim instanceof LivingEntity living)
                || !isCombatTarget(living)) return false;
        boolean hit;
        if (isThunder()) {
            hit = victim.hurt(lightningDamage(), attackDamage());
            if (hit) setLastHurtMob(victim);
        } else {
            hit = super.doHurtTarget(victim);
        }
        if (hit) entityData.set(MELEE_TICKS, 9);
        return hit;
    }

    private void updateNetherBonus() {
        if (getVariant() != GuabaDragonVariant.NETHER) return;
        boolean shouldBoost = level().dimension().equals(Level.NETHER);
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health.hasModifier(NETHER_BONUS) == shouldBoost) return;
        AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (shouldBoost) {
            health.addTransientModifier(NETHER_BONUS);
            attack.addTransientModifier(NETHER_BONUS);
        } else {
            health.removeModifier(NETHER_BONUS.getId());
            attack.removeModifier(NETHER_BONUS.getId());
            setHealth(getHealth());
        }
    }

    @Override public void aiStep() {
        if (level() instanceof ServerLevel server) {
            updateNetherBonus();
            if (getTarget() != null && !isCombatTarget(getTarget())) setTarget(null);
            if (isFlying() && (isNoAi() || !isCombatTarget(getTarget()))) cloudPursuit.stop();
            if (flightCooldown > 0 && onGround() && !isFlying()) flightCooldown--;
            int biteTicks = entityData.get(MELEE_TICKS);
            if (biteTicks > 0) entityData.set(MELEE_TICKS, biteTicks - 1);
            if (breath != null) breath.serverStep(server);
            if (tickCount % 5 == 0) emitBodyParticles(server);
        }
        super.aiStep();
    }

    private void emitBodyParticles(ServerLevel server) {
        ParticleOptions particle = switch (getVariant()) {
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case CLOUD -> ParticleTypes.CLOUD;
            default -> null;
        };
        if (particle != null) {
            server.sendParticles(particle, getX(), getY() + (isFlying() ? 0.3 : 1.1), getZ(),
                isFlying() ? 5 : 2, 0.55, 0.35, 0.55, 0.015);
        }
    }

    private final class CloudPursuitGoal extends Goal {
        private int remaining;
        private double takeoffY;

        CloudPursuitGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }

        @Override public boolean canUse() {
            LivingEntity target = getTarget();
            if (getVariant() != GuabaDragonVariant.CLOUD || flightCooldown != 0 || !onGround() || isNoGravity()
                    || isInWater() || !isCombatTarget(target) || distanceToSqr(target) > 256) return false;
            if (target.getY() - getY() >= 1.5) return true;
            if (!hasLineOfSight(target) || distanceToSqr(target) <= BITE_REACH_SQUARED) return false;
            Path path = navigation.createPath(target, 0);
            return path == null || !path.canReach();
        }

        @Override public boolean canContinueToUse() {
            return remaining > 0 && !isNoAi() && !isInWater() && isCombatTarget(getTarget())
                && distanceToSqr(getTarget()) <= 256;
        }

        @Override public void start() {
            remaining = FLIGHT_DURATION;
            takeoffY = getY();
            navigation.stop();
            setNoGravity(true);
            entityData.set(FLYING, true);
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            if (!isFlying()) return;
            LivingEntity target = getTarget();
            if (--remaining <= 0 || !isCombatTarget(target)) {
                stop();
                return;
            }
            getLookControl().setLookAt(target);
            if (!onGround() && distanceToSqr(target) <= BITE_REACH_SQUARED && hasLineOfSight(target)) {
                doHurtTarget(target);
                stop();
                return;
            }
            Vec3 delta = target.position().subtract(position());
            Vec3 flat = new Vec3(delta.x, 0, delta.z);
            Vec3 step = flat.normalize().scale(Math.min(0.44, flat.length() * 0.22));
            double height = Mth.clamp(target.getY() + 0.6, takeoffY + 0.6, takeoffY + 4);
            double climb = Mth.clamp((height - getY()) * 0.3, -0.24, 0.26);
            setDeltaMovement(step.x, climb, step.z);
            resetFallDistance();
        }

        @Override public void stop() {
            if (!isFlying()) return;
            remaining = 0;
            entityData.set(FLYING, false);
            setNoGravity(false);
            Vec3 delta = getDeltaMovement();
            setDeltaMovement(delta.x * 0.25, Math.min(0, delta.y), delta.z * 0.25);
            flightCooldown = FLIGHT_COOLDOWN;
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("GuabaDragonVariant", getVariant().id());
        tag.putInt("GuabaFlightCooldown", isFlying() ? FLIGHT_COOLDOWN : flightCooldown);
        if (isFlying()) tag.remove("NoGravity");
        if (breath != null) breath.save(tag);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, GuabaDragonVariant.byId(tag.getString("GuabaDragonVariant")).id());
        flightCooldown = Mth.clamp(tag.getInt("GuabaFlightCooldown"), 0, FLIGHT_COOLDOWN);
        entityData.set(FLYING, false);
        entityData.set(MELEE_TICKS, 0);
        if (breath != null) breath.load(tag);
        updateNetherBonus();
        if (tag.contains("Health", Tag.TAG_ANY_NUMERIC)) setHealth(tag.getFloat("Health"));
        if (!tag.contains("CustomNameVisible", Tag.TAG_BYTE)) setCustomNameVisible(true);
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override protected ResourceLocation getDefaultLootTable() { return BuiltInLootTables.EMPTY; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PetGuabaDragonEntity>(this, "body", 0, state -> {
            if (getBreathTicks() > 0) return state.setAndContinue(BREATH);
            if (entityData.get(MELEE_TICKS) > 0) return state.setAndContinue(BITE);
            if (isFlying()) return state.setAndContinue(FLIGHT);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

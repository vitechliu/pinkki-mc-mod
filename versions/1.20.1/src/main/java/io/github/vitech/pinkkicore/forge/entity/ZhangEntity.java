package io.github.vitech.pinkkicore.forge.entity;

import io.github.vitech.pinkkicore.forge.galgame.GalgameAccessories;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public final class ZhangEntity extends Monster implements GeoEntity {
    private static final double CONTACT_MARGIN = 0.04;
    private static final int HIT_INTERVAL = 20;
    private static final int SEARCH_INTERVAL = 10;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.zhang.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.zhang.walk");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<Integer, Integer> nextHit = new HashMap<>();

    public ZhangEntity(EntityType<? extends ZhangEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes(double multiplier) {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40 * multiplier)
            .add(Attributes.ATTACK_DAMAGE, 14 * multiplier)
            .add(Attributes.MOVEMENT_SPEED, 0.25 * multiplier)
            .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Pursuit());
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target != this && target.isAlive() && !target.isRemoved() && !isAlliedTo(target)
            && !target.isSpectator() && (!(target instanceof Player player) || !player.isCreative())
            && !GalgameAccessories.isProtected(target) && super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive() || isNoAi()) {
            return;
        }
        if (getTarget() != null && !canAttack(getTarget())) {
            setTarget(null);
            getNavigation().stop();
        }
        nextHit.values().removeIf(expiry -> expiry <= tickCount);
        AABB contact = getBoundingBox().inflate(CONTACT_MARGIN);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, contact, this::canAttack)) {
            if (!nextHit.containsKey(victim.getId()) && doHurtTarget(victim)) {
                nextHit.put(victim.getId(), tickCount + HIT_INTERVAL);
            }
        }
    }

    public boolean isBig() {
        return getType() == ModEntities.BIG_ZHANG.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ZhangEntity>(this, "body", 3,
            state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private final class Pursuit extends Goal {
        private int searchAfter;
        private int repathAfter;

        Pursuit() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return findTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return findTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            if (tickCount >= repathAfter) {
                getNavigation().moveTo(target, 1.0);
                repathAfter = tickCount + 5;
            }
        }

        @Override
        public void start() {
            repathAfter = tickCount;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        private LivingEntity findTarget() {
            double radius = getAttributeValue(Attributes.FOLLOW_RANGE);
            LivingEntity current = getTarget();
            if (current != null && (!canAttack(current) || distanceToSqr(current) > radius * radius)) {
                setTarget(null);
            }
            if (tickCount >= searchAfter) {
                searchAfter = tickCount + SEARCH_INTERVAL;
                double closestDistance = radius * radius;
                LivingEntity closest = null;
                for (LivingEntity candidate : level().getEntitiesOfClass(
                    LivingEntity.class, getBoundingBox().inflate(radius), ZhangEntity.this::canAttack)) {
                    double distance = distanceToSqr(candidate);
                    if (distance <= closestDistance) {
                        closest = candidate;
                        closestDistance = distance;
                    }
                }
                if (closest != getTarget()) {
                    setTarget(closest);
                    repathAfter = tickCount;
                }
            }
            return getTarget();
        }
    }
}

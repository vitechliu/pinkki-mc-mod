package io.github.vitech.pinkkicore.forge.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-side aim, animation timing and collision for both dragon breath attacks. */
abstract class GuabaDragonBreath extends Goal {
    static final int WINDUP = 20, DURATION = 60, RECOVERY = 16;
    static final int TOTAL = WINDUP + DURATION + RECOVERY, COOLDOWN = 160;
    static final double RANGE = 6;
    private final Mob dragon;
    private final EntityDataAccessor<Integer> ticks;
    private final ParticleOptions particle;
    private final SoundEvent sound;
    private float yaw;
    private int cooldown = 40;
    private LivingEntity target;
    private Vec3 direction = Vec3.ZERO;

    GuabaDragonBreath(Mob dragon, EntityDataAccessor<Integer> ticks, ParticleOptions particle, SoundEvent sound) {
        this.dragon = dragon;
        this.ticks = ticks;
        this.particle = particle;
        this.sound = sound;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    abstract boolean validTarget(LivingEntity victim);
    abstract boolean canHit(LivingEntity victim, LivingEntity target);
    abstract void damage(LivingEntity victim);

    ParticleOptions particle() { return particle; }
    SoundEvent sound() { return sound; }

    private int remaining() { return dragon.getEntityData().get(ticks); }

    @Override public boolean canUse() {
        LivingEntity candidate = dragon.getTarget();
        return cooldown == 0 && validTarget(candidate) && !dragon.isInWater()
            && dragon.distanceToSqr(candidate) >= 9 && dragon.distanceToSqr(candidate) <= RANGE * RANGE
            && dragon.hasLineOfSight(candidate);
    }

    @Override public boolean canContinueToUse() {
        return remaining() > 0 && validTarget(target) && dragon.getTarget() == target && !dragon.isInWater();
    }

    @Override public void start() {
        target = dragon.getTarget();
        Vec3 delta = target.position().subtract(dragon.position());
        yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
        holdPosition();
        dragon.getEntityData().set(ticks, TOTAL);
        dragon.playSound(sound(), 0.8F, 0.7F);
    }

    private void holdPosition() {
        dragon.getNavigation().stop();
        dragon.setYRot(yaw);
        dragon.setYHeadRot(yaw);
        dragon.setYBodyRot(yaw);
    }

    @Override public void tick() { holdPosition(); }

    @Override public void stop() {
        if (remaining() == 0) return;
        dragon.getEntityData().set(ticks, 0);
        target = null;
        cooldown = COOLDOWN;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    void serverStep(ServerLevel server) {
        if (cooldown > 0) cooldown--;
        int remaining = remaining();
        if (remaining == 0) return;
        if (dragon.isNoAi() || !dragon.isAlive() || !canContinueToUse()) {
            stop();
            return;
        }
        holdPosition();
        int elapsed = TOTAL - remaining;
        // Aim once after wind-up, so targets can dodge the stream.
        if (elapsed == WINDUP) direction = target.getBoundingBox().getCenter().subtract(origin(dragon)).normalize();
        if (elapsed >= WINDUP && elapsed < WINDUP + DURATION) emit(server, (elapsed - WINDUP) % 20 == 0);
        if (remaining == 1) stop();
        else dragon.getEntityData().set(ticks, remaining - 1);
    }

    static Vec3 origin(Mob dragon) {
        Vec3 forward = Vec3.directionFromRotation(0, dragon.getYRot());
        return dragon.position().add(0, 1.95, 0).add(forward.scale(1.5));
    }

    private void emit(ServerLevel server, boolean damageTick) {
        Vec3 start = origin(dragon);
        Vec3 end = start.add(direction.scale(RANGE - 1.5));
        // Clip from the throat, including the space ahead of the collision box.
        Vec3 throat = dragon.position().add(0, 1.95, 0);
        HitResult obstruction = server.clip(new ClipContext(throat, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, dragon));
        if (obstruction.getType() != HitResult.Type.MISS) {
            if (obstruction.getLocation().distanceToSqr(throat) <= start.distanceToSqr(throat)) return;
            end = obstruction.getLocation();
        }
        double length = end.distanceTo(start);
        ParticleOptions particle = particle();
        for (double step = 0; step < length; step += 0.45) {
            Vec3 point = start.add(direction.scale(step));
            double spread = 0.06 + step * 0.035;
            server.sendParticles(particle, point.x, point.y, point.z, 2, spread, spread, spread, 0.01);
        }
        if (!damageTick) return;
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.4),
                entity -> entity != dragon && canHit(entity, target))) {
            AABB box = victim.getBoundingBox().inflate(0.35);
            if (dragon.distanceToSqr(victim) <= RANGE * RANGE && dragon.hasLineOfSight(victim)
                    && (box.contains(start) || box.clip(start, end).isPresent())) damage(victim);
        }
    }

    void save(CompoundTag tag) {
        tag.putInt("GuabaBreathCooldown", remaining() > 0 ? COOLDOWN : cooldown);
    }

    void load(CompoundTag tag) {
        dragon.getEntityData().set(ticks, 0);
        target = null;
        cooldown = Math.max(40, Math.min(COOLDOWN, tag.getInt("GuabaBreathCooldown")));
    }
}

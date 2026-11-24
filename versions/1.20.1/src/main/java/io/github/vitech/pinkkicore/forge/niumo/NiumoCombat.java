package io.github.vitech.pinkkicore.forge.niumo;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.compat.curios.CuriosIntegration;
import io.github.vitech.pinkkicore.forge.dream.DreamEquipment;
import io.github.vitech.pinkkicore.forge.entity.BingwaEntity;
import io.github.vitech.pinkkicore.forge.entity.NiumoMob;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class NiumoCombat {
    private static final String SHOT = ModInfo.ID + ".niumo_shooter";
    private static final String OWNER = ModInfo.ID + ".weapon_shot_owner";

    private NiumoCombat() {}

    private static boolean wearsWhiskers(LivingEntity living) {
        return ModList.get().isLoaded("curios") && CuriosIntegration.wearingNiumoWhiskers(living);
    }

    @SubscribeEvent
    public static void projectileJoined(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && !event.loadedFromDisk()
            && event.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner
            && wearsWhiskers(owner)) {
            recordShot(projectile, owner);
        }
    }

    public static void recordWeaponLaunch(Projectile projectile, LivingEntity shooter, boolean demonSoul) {
        if (projectile.level().isClientSide) return;
        // Some host crossbows leave the vanilla owner unset on fireworks.
        if (projectile.getOwner() == null) {
            projectile.getPersistentData().putUUID(OWNER, shooter.getUUID());
        }
        if (demonSoul || wearsWhiskers(shooter)) recordShot(projectile, shooter);
    }

    private static void recordShot(Projectile projectile, LivingEntity shooter) {
        projectile.getPersistentData().putUUID(SHOT, shooter.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || event.getAmount() <= 0) return;

        Entity direct = event.getSource().getDirectEntity();
        Entity source = event.getSource().getEntity();
        Projectile projectile = direct instanceof Projectile shot ? shot : null;
        LivingEntity attacker = source instanceof LivingEntity living ? living : null;
        if (projectile != null && attacker == null) {
            var data = projectile.getPersistentData();
            if (projectile.getOwner() instanceof LivingEntity living) {
                attacker = living;
            } else if (data.hasUUID(OWNER) && level.getEntity(data.getUUID(OWNER)) instanceof LivingEntity living) {
                attacker = living;
            }
        }
        if (attacker != null && attacker != victim && victim instanceof NiumoMob.LaoCai laoCai) {
            laoCai.setTarget(attacker);
        }
        boolean melee = attacker != null && direct == attacker &&
            (event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().is(DamageTypes.MOB_ATTACK)
                || event.getSource().is(DamageTypes.MOB_ATTACK_NO_AGGRO));
        if (attacker == victim || (!melee && projectile == null)) return;

        boolean offensive;
        if (projectile != null) {
            var data = projectile.getPersistentData();
            // The firing snapshot survives a weapon/accessory swap and an unloaded shooter.
            UUID marked = data.hasUUID(SHOT) ? data.getUUID(SHOT) : null;
            offensive = marked != null && (attacker == null || marked.equals(attacker.getUUID()));
        } else {
            offensive = attacker instanceof BingwaEntity || attacker instanceof NiumoMob
                || wearsWhiskers(attacker) || DreamEquipment.qualifiesAsDemonWeapon(attacker.getMainHandItem());
        }
        if (offensive && victim.getHealth() > event.getAmount()) NiumoEffect.apply(victim);
        if (attacker != null && DreamEquipment.wearsDemonArmor(victim)) NiumoEffect.apply(attacker);
    }
}

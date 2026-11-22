package io.github.vitech.pinkkicore.forge.galgame;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.compat.galgame.GalgameProjectiles;
import io.github.vitech.pinkkicore.forge.dream.DreamEquipment;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class GalgameEquipment {
    private GalgameEquipment() {}

    public static void refreshArmor(Player player) {
        var active = player.getEffect(GuabaPotions.GALGAME.get());
        if (active == null || (!active.isInfiniteDuration()
            && active.getDuration() <= GalgameEffect.EQUIPMENT_TIME / 2)) {
            GalgameEffect.grant(player, GalgameEffect.EQUIPMENT_TIME, 0);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getAmount() <= 0) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        LivingEntity user;
        if (direct instanceof Projectile projectile) {
            user = GalgameProjectiles.markedShooter(level, projectile);
        } else if (direct instanceof LivingEntity attacker && GalgameEffect.isDirectMelee(event.getSource())
            && DreamEquipment.qualifiesAsGalgameWeapon(attacker.getMainHandItem())) {
            user = attacker;
        } else {
            return;
        }
        if (user != null && user.isAlive() && !user.isSpectator() && user != event.getEntity()) {
            GalgameEffect.grant(user, GalgameEffect.EQUIPMENT_TIME, 0);
        }
    }
}

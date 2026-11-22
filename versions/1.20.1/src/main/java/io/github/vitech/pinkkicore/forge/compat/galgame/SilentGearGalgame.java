package io.github.vitech.pinkkicore.forge.compat.galgame;

import io.github.vitech.pinkkicore.forge.dream.DreamEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.silentchaos512.gear.item.gear.GearBowItem;
import net.silentchaos512.gear.item.gear.GearTridentItem;

public final class SilentGearGalgame {
    private static final String PENDING_BOW_SHOT = "pinkkicore.galgame_silent_gear_shot";

    private SilentGearGalgame() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, SilentGearGalgame::releaseBow);
        MinecraftForge.EVENT_BUS.addListener(SilentGearGalgame::spawn);
    }

    private static void releaseBow(ArrowLooseEvent event) {
        if (!event.getLevel().isClientSide && event.getBow().getItem() instanceof GearBowItem) {
            var player = event.getEntity();
            var data = player.getPersistentData();
            data.remove(PENDING_BOW_SHOT);
            if (event.getCharge() >= 3 && (event.hasAmmo() || player.getAbilities().instabuild)
                && DreamEquipment.containsGalgameSoul(event.getBow())) {
                data.putLong(PENDING_BOW_SHOT, event.getLevel().getGameTime());
            }
        }
    }

    private static void spawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)
            || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        if (arrow instanceof ThrownTrident trident) {
            if (trident.getPickupItem().getItem() instanceof GearTridentItem) {
                GalgameProjectiles.record(trident, owner, trident.getPickupItem());
            }
            return;
        }
        var data = owner.getPersistentData();
        if (data.contains(PENDING_BOW_SHOT) && data.getLong(PENDING_BOW_SHOT) == event.getLevel().getGameTime()
            && owner.getUseItem().getItem() instanceof GearBowItem) {
            data.remove(PENDING_BOW_SHOT);
            GalgameProjectiles.record(arrow, owner);
        }
    }
}

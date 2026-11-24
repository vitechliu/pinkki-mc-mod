package io.github.vitech.pinkkicore.forge.compat.galgame;

import io.github.vitech.pinkkicore.forge.dream.DreamEquipment;
import io.github.vitech.pinkkicore.forge.niumo.NiumoCombat;
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
    private static final String PENDING_MATERIALS = "pinkkicore.silent_gear_shot_materials";

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
            data.remove(PENDING_MATERIALS);
            if (event.getCharge() >= 3 && (event.hasAmmo() || player.getAbilities().instabuild)) {
                int materials = (DreamEquipment.containsGalgameSoul(event.getBow()) ? 1 : 0)
                    | (DreamEquipment.containsDemonSoul(event.getBow()) ? 2 : 0);
                if (materials != 0) {
                    data.putLong(PENDING_BOW_SHOT, event.getLevel().getGameTime());
                    data.putByte(PENDING_MATERIALS, (byte) materials);
                }
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
            int materials = data.getByte(PENDING_MATERIALS);
            data.remove(PENDING_MATERIALS);
            if ((materials & 1) != 0) GalgameProjectiles.record(arrow, owner);
            NiumoCombat.recordWeaponLaunch(arrow, owner, (materials & 2) != 0);
        }
    }
}

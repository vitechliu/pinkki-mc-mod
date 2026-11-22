package io.github.vitech.pinkkicore.forge.compat.galgame;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.dream.DreamEquipment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

public final class GalgameProjectiles {
    private static final String SHOOTER = ModInfo.ID + ".galgame_shooter";

    private GalgameProjectiles() {}

    public static void record(Projectile projectile, LivingEntity shooter, ItemStack launcher) {
        if (!projectile.level().isClientSide && DreamEquipment.containsGalgameSoul(launcher)) {
            record(projectile, shooter);
        }
    }

    static void record(Projectile projectile, LivingEntity shooter) {
        if (!projectile.level().isClientSide) {
            projectile.getPersistentData().putUUID(SHOOTER, shooter.getUUID());
        }
    }

    public static LivingEntity markedShooter(ServerLevel level, Projectile projectile) {
        var tag = projectile.getPersistentData();
        if (!tag.hasUUID(SHOOTER)) {
            return null;
        }
        var id = tag.getUUID(SHOOTER);
        if (projectile.getOwner() instanceof LivingEntity shooter && id.equals(shooter.getUUID())) {
            return shooter;
        }
        // Some crossbow fireworks are created without a vanilla owner.
        if (projectile.getOwner() == null && level.getEntity(id) instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }
}

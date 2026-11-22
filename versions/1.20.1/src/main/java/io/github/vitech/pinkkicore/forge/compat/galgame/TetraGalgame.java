package io.github.vitech.pinkkicore.forge.compat.galgame;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import se.mickelus.tetra.event.ModularProjectileSpawnEvent;
import se.mickelus.tetra.items.modular.ThrownModularItemEntity;

public final class TetraGalgame {
    private TetraGalgame() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TetraGalgame::shoot);
        MinecraftForge.EVENT_BUS.addListener(TetraGalgame::throwWeapon);
    }

    private static void shoot(ModularProjectileSpawnEvent event) {
        GalgameProjectiles.record(event.getProjectileEntity(), event.getShooter(), event.getFiringStack());
    }

    private static void throwWeapon(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && !event.loadedFromDisk()
            && event.getEntity() instanceof ThrownModularItemEntity projectile
            && projectile.getOwner() instanceof LivingEntity owner) {
            GalgameProjectiles.record(projectile, owner, projectile.getThrownStack());
        }
    }
}

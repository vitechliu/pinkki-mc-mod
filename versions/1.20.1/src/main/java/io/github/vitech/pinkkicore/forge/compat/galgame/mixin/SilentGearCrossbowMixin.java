package io.github.vitech.pinkkicore.forge.compat.galgame.mixin;

import io.github.vitech.pinkkicore.forge.compat.galgame.GalgameProjectiles;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.silentchaos512.gear.item.gear.GearCrossbowItem", remap = false)
public abstract class SilentGearCrossbowMixin {
    // The two stores are the arrow and firework branches, both before the crossbow takes damage.
    @ModifyVariable(method = {"shootProjectile", "m_40894_"}, at = @At("STORE"), index = 11, require = 2, remap = false)
    private static Projectile pinkkicore$recordShot(Projectile projectile, Level level, LivingEntity shooter,
            InteractionHand hand, ItemStack crossbow, ItemStack ammo, float sound, boolean creative,
            float speed, float inaccuracy, float angle) {
        GalgameProjectiles.record(projectile, shooter, crossbow);
        return projectile;
    }
}

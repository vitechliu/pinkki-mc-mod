package io.github.vitech.pinkkicore.forge.compat.galgame;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.niumo.NiumoCombat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

public final class TinkersGalgame {
    private TinkersGalgame() {}

    public static void register(IEventBus bus) {
        var modifiers = ModifierDeferredRegister.create(ModInfo.ID);
        modifiers.register("galgame_shot", () -> new MaterialShot(false));
        modifiers.register("demon_shot", () -> new MaterialShot(true));
        modifiers.register(bus);
    }

    private static final class MaterialShot extends Modifier implements ProjectileLaunchModifierHook {
        private final boolean demonSoul;

        MaterialShot(boolean demonSoul) {
            this.demonSoul = demonSoul;
        }

        @Override
        protected void registerHooks(ModuleHookMap.Builder builder) {
            builder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_SHOT, ModifierHooks.PROJECTILE_THROWN);
        }

        @Override
        public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter,
                                       Projectile projectile, AbstractArrow arrow, ModDataNBT data, boolean primary) {
            if (shooter == null) return;
            if (!demonSoul) GalgameProjectiles.record(projectile, shooter);
            NiumoCombat.recordWeaponLaunch(projectile, shooter, demonSoul);
        }
    }
}

package io.github.vitech.pinkkicore.forge.galgame;

import io.github.vitech.pinkkicore.forge.compat.curios.CuriosIntegration;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

public final class GalgameAccessories {
    public static final String SLOT = "pinkki";

    private GalgameAccessories() {}

    public static boolean isProtected(LivingEntity entity) {
        return entity instanceof Player && ModList.get().isLoaded("curios")
            && CuriosIntegration.wearingPinkki(entity, ModItems.FAVORITE_PERSON.get());
    }

    public static boolean hasHeart(Player player) {
        return ModList.get().isLoaded("curios")
            && CuriosIntegration.wearingPinkki(player, ModItems.GALGAME_HEART.get());
    }
}

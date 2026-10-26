package io.github.vitech.pinkkicore.forge.client.guide;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModInfo.ID, value = Dist.CLIENT)
public final class GuideClientEvents {
    private GuideClientEvents() {}
    @SubscribeEvent public static void useGuide(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide && event.getItemStack().is(ModItems.GUIDE_BOOK.get()))
            Minecraft.getInstance().setScreen(new GuideScreen());
    }
}

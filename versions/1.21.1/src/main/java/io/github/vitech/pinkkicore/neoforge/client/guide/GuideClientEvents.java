package io.github.vitech.pinkkicore.neoforge.client.guide;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.neoforge.item.ModItems;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ModInfo.ID, value = Dist.CLIENT)
public final class GuideClientEvents {
    private GuideClientEvents() {}
    @SubscribeEvent public static void useGuide(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide && event.getItemStack().is(ModItems.GUIDE_BOOK.get()))
            Minecraft.getInstance().setScreen(new GuideScreen());
    }
}

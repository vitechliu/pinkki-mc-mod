package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ModInfo.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(WorkbenchContent.MENU.get(), GuabaWorkbenchScreen::new);
            MenuScreens.register(io.github.vitech.pinkkicore.forge.charging.ChargingContent.MENU.get(), GuabaChargingScreen::new);
            ItemProperties.register(ModItems.GUABA_BOW.get(), new ResourceLocation("pull"), (stack, level, entity, seed) ->
                entity == null || entity.getUseItem() != stack ? 0F : (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20F);
            ItemProperties.register(ModItems.GUABA_BOW.get(), new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1F : 0F);
        });
    }
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PINKKI.get(), PinkkiRenderer::new);
        event.registerEntityRenderer(ModEntities.BINGWA.get(), BingwaRenderer::new);
    }
}

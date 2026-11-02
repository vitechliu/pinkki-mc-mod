package io.github.vitech.pinkkicore.neoforge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.neoforge.entity.ModEntities;
import io.github.vitech.pinkkicore.neoforge.workbench.WorkbenchContent;
import io.github.vitech.pinkkicore.neoforge.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ModInfo.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.GUABA_BOW.get(), ResourceLocation.parse("pull"), (stack, level, entity, seed) ->
                entity == null || entity.getUseItem() != stack ? 0F : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20F);
            ItemProperties.register(ModItems.GUABA_BOW.get(), ResourceLocation.parse("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1F : 0F);
        });
    }
    @SubscribeEvent public static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(WorkbenchContent.MENU.get(), GuabaWorkbenchScreen::new);
        event.register(io.github.vitech.pinkkicore.neoforge.charging.ChargingContent.MENU.get(), GuabaChargingScreen::new);
    }
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GUABA_YINGRONG.get(), GuabaYingrongRenderer::new);
        event.registerEntityRenderer(ModEntities.PINKKI.get(), PinkkiRenderer::new);
        event.registerEntityRenderer(ModEntities.BINGWA.get(), BingwaRenderer::new);
    }
}

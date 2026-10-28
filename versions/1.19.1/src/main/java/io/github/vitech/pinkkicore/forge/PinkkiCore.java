package io.github.vitech.pinkkicore.forge;

import com.mojang.logging.LogUtils;
import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib3.GeckoLib;

/** Version-specific Forge entry point. Registers the 1.19.1 gameplay content. */
@Mod(ModInfo.ID)
public final class PinkkiCore {
    public PinkkiCore() {
        GeckoLib.initialize();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(bus);
        ModItems.register(bus);
        io.github.vitech.pinkkicore.forge.ore.PinkOreContent.register(bus);
        WorkbenchContent.register(bus);
        io.github.vitech.pinkkicore.forge.charging.ChargingContent.register(bus);
        GuabaPotions.register(bus);
        bus.addListener(this::onCommonSetup);
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            io.github.vitech.pinkkicore.forge.compat.curios.CuriosIntegration.register();
            bus.addListener(io.github.vitech.pinkkicore.forge.compat.curios.CuriosIntegration::registerSlots);
        }
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(GuabaPotions::registerBrewing);
        event.enqueueWork(io.github.vitech.pinkkicore.forge.item.ChargedCreeperEggItem::registerDispenser);
        if (net.minecraftforge.fml.ModList.get().isLoaded("tetra")) {
            event.enqueueWork(io.github.vitech.pinkkicore.forge.compat.tetra.TetraIntegration::register);
        }
        LogUtils.getLogger().info(ModInfo.startupMessage());
    }
}

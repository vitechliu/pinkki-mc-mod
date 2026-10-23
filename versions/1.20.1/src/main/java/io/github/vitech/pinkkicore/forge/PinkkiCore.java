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
import software.bernie.geckolib.GeckoLib;

/** Version-specific Forge entry point. Registers the 1.20.1 gameplay content. */
@Mod(ModInfo.ID)
public final class PinkkiCore {
    public PinkkiCore() {
        GeckoLib.initialize();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(bus);
        ModItems.register(bus);
        WorkbenchContent.register(bus);
        GuabaPotions.register(bus);
        bus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(GuabaPotions::registerBrewing);
        LogUtils.getLogger().info(ModInfo.startupMessage());
    }
}

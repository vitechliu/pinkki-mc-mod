package io.github.vitech.pinkkicore.forge;

import com.mojang.logging.LogUtils;
import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

/** Version-specific Forge entry point. No gameplay content is registered. */
@Mod(ModInfo.ID)
public final class PinkkiCore {
    public PinkkiCore() {
        GeckoLib.initialize();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LogUtils.getLogger().info(ModInfo.startupMessage());
    }
}

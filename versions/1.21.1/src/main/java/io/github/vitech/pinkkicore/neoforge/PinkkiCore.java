package io.github.vitech.pinkkicore.neoforge;

import com.mojang.logging.LogUtils;
import io.github.vitech.pinkkicore.core.ModInfo;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** NeoForge adapter; shared code does not depend on this loader. */
@Mod(ModInfo.ID)
public final class PinkkiCore {
    public PinkkiCore(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LogUtils.getLogger().info(ModInfo.startupMessage());
    }
}

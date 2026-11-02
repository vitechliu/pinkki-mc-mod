package io.github.vitech.pinkkicore.neoforge;

import com.mojang.logging.LogUtils;
import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.neoforge.item.ModItems;
import io.github.vitech.pinkkicore.neoforge.entity.ModEntities;
import io.github.vitech.pinkkicore.neoforge.workbench.WorkbenchContent;
import io.github.vitech.pinkkicore.neoforge.effect.GuabaPotions;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** NeoForge 1.21.1 entry point. Optional integrations load only when their host is present. */
@Mod(ModInfo.ID)
public final class PinkkiCore {
    public PinkkiCore(net.neoforged.bus.api.IEventBus bus) {
        ModEntities.register(bus);
        ModItems.register(bus);
        io.github.vitech.pinkkicore.neoforge.ore.PinkOreContent.register(bus);
        WorkbenchContent.register(bus);
        io.github.vitech.pinkkicore.neoforge.charging.ChargingContent.register(bus);
        GuabaPotions.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GuabaPotions::registerBrewing);
        bus.addListener(io.github.vitech.pinkkicore.neoforge.workbench.MachineCapabilities::register);
        bus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(io.github.vitech.pinkkicore.neoforge.item.ChargedCreeperEggItem::registerDispenser);
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            event.enqueueWork(io.github.vitech.pinkkicore.neoforge.compat.curios.CuriosIntegration::register);
        }
        LogUtils.getLogger().info(ModInfo.startupMessage());
    }
}

package io.github.vitech.pinkkicore.neoforge.workbench;
import io.github.vitech.pinkkicore.neoforge.charging.ChargingContent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
public final class MachineCapabilities {
    private MachineCapabilities() {}
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WorkbenchContent.ENTITY.get(), (entity, side) -> entity.handler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ChargingContent.ENTITY.get(), (entity, side) -> entity.handler(side));
    }
}

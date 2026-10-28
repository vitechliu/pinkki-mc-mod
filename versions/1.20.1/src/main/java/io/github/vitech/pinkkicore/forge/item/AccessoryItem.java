package io.github.vitech.pinkkicore.forge.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

/** Plain item keeps Curios optional and its API out of the normal loading path. */
public final class AccessoryItem extends Item {
    private final String id;
    public AccessoryItem(String id) { super(new Properties().stacksTo(1)); this.id = id; }
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.pinkkicore." + id));
        if (!ModList.get().isLoaded("curios")) lines.add(Component.translatable("tooltip.pinkkicore.requires_curios"));
    }
}

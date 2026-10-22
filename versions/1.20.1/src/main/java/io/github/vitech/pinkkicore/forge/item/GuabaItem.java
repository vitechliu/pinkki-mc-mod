package io.github.vitech.pinkkicore.forge.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A plain crafting material; deliberately has no food or use behavior. */
public final class GuabaItem extends Item {
    public GuabaItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.pinkkicore.guaba").withStyle(ChatFormatting.GRAY));
    }
}

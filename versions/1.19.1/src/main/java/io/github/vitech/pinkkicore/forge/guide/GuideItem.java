package io.github.vitech.pinkkicore.forge.guide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Dist.CLIENT event subscriber owns opening the Screen; no client classes here. */
public final class GuideItem extends Item {
    public GuideItem() { super(new Item.Properties().tab(net.minecraft.world.item.CreativeModeTab.TAB_TOOLS).stacksTo(1)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}

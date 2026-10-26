package io.github.vitech.pinkkicore.forge.item;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.BlockSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;

/** A distinct egg item without replacing vanilla's Creeper-to-egg registry mapping. */
public final class ChargedCreeperEggItem extends Item {
    public ChargedCreeperEggItem() { super(new Item.Properties()); }
    private static ItemStack prepared(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.getOrCreateTagElement("EntityTag").putBoolean("powered", true);
        return copy;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var clicked = context.getClickedPos();
        var pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
            ? clicked : clicked.relative(context.getClickedFace());
        var creeper = EntityType.CREEPER.spawn(level, prepared(context.getItemInHand()), context.getPlayer(), pos,
            MobSpawnType.SPAWN_EGG, true, !pos.equals(clicked) && context.getClickedFace() == Direction.UP);
        if (creeper != null) {
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.gameEvent(context.getPlayer(), net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, pos);
        }
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Vanilla liquid ray trace and permissions, operating on this item stack.
        player.getItemInHand(hand).getOrCreateTagElement("EntityTag").putBoolean("powered", true);
        return Items.CREEPER_SPAWN_EGG.use(level, player, hand);
    }
    public static void registerDispenser() {
        DispenserBlock.registerBehavior(ModItems.CHARGED_CREEPER_EGG.get(), new DefaultDispenseItemBehavior() {
            @Override protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction face = source.getBlockState().getValue(DispenserBlock.FACING);
                var pos = source.getPos().relative(face);
                var creeper = EntityType.CREEPER.spawn(source.getLevel(), prepared(stack), null, pos,
                    MobSpawnType.DISPENSER, face != Direction.UP, false);
                if (creeper != null) stack.shrink(1);
                return stack;
            }
        });
    }
}

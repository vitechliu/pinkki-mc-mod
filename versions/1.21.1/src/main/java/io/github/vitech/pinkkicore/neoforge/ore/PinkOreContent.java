package io.github.vitech.pinkkicore.neoforge.ore;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Basic ore processing only; material effects and equipment are intentionally undefined. */
public final class PinkOreContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ModInfo.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ModInfo.ID);

    public static final DeferredHolder<Block, Block> ORE = BLOCKS.register("pink_ore",
            () -> new DropExperienceBlock(net.minecraft.util.valueproviders.ConstantInt.of(0), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)));
    public static final DeferredHolder<Block, Block> DEEPSLATE_ORE = BLOCKS.register("deepslate_pink_ore",
            () -> new DropExperienceBlock(net.minecraft.util.valueproviders.ConstantInt.of(0), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE)));
    public static final DeferredHolder<Block, Block> BLOCK = BLOCKS.register("pink_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredHolder<Block, Block> RAW_BLOCK = BLOCKS.register("raw_pink_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK)));
    public static final DeferredHolder<Item, Item> ORE_ITEM = blockItem("pink_ore", ORE);
    public static final DeferredHolder<Item, Item> DEEPSLATE_ORE_ITEM = blockItem("deepslate_pink_ore", DEEPSLATE_ORE);
    public static final DeferredHolder<Item, Item> BLOCK_ITEM = blockItem("pink_block", BLOCK);
    public static final DeferredHolder<Item, Item> RAW_BLOCK_ITEM = blockItem("raw_pink_block", RAW_BLOCK);
    public static final DeferredHolder<Item, Item> RAW_PINK = ITEMS.register("raw_pink", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> INGOT = ITEMS.register("pink_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NUGGET = ITEMS.register("pink_nugget", () -> new Item(new Item.Properties()));

    private PinkOreContent() {}

    private static DeferredHolder<Item, Item> blockItem(String name, DeferredHolder<Block, Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(PinkOreContent::creativeContents);
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ORE_ITEM.get());
            event.accept(DEEPSLATE_ORE_ITEM.get());
            event.accept(RAW_BLOCK_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(BLOCK_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_PINK.get());
            event.accept(INGOT.get());
            event.accept(NUGGET.get());
        }
    }
}

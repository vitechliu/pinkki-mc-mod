package io.github.vitech.pinkkicore.forge.ore;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Basic ore processing only; material effects and equipment are intentionally undefined. */
public final class PinkOreContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModInfo.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModInfo.ID);

    public static final RegistryObject<Block> ORE = BLOCKS.register("pink_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE)));
    public static final RegistryObject<Block> DEEPSLATE_ORE = BLOCKS.register("deepslate_pink_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE)));
    public static final RegistryObject<Block> BLOCK = BLOCKS.register("pink_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Block> RAW_BLOCK = BLOCKS.register("raw_pink_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_IRON_BLOCK)));
    public static final RegistryObject<Item> ORE_ITEM = blockItem("pink_ore", ORE);
    public static final RegistryObject<Item> DEEPSLATE_ORE_ITEM = blockItem("deepslate_pink_ore", DEEPSLATE_ORE);
    public static final RegistryObject<Item> BLOCK_ITEM = blockItem("pink_block", BLOCK);
    public static final RegistryObject<Item> RAW_BLOCK_ITEM = blockItem("raw_pink_block", RAW_BLOCK);
    public static final RegistryObject<Item> RAW_PINK = ITEMS.register("raw_pink", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INGOT = ITEMS.register("pink_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NUGGET = ITEMS.register("pink_nugget", () -> new Item(new Item.Properties()));

    private PinkOreContent() {}

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(PinkOreContent::creativeContents);
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ORE_ITEM);
            event.accept(DEEPSLATE_ORE_ITEM);
            event.accept(RAW_BLOCK_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(BLOCK_ITEM);
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_PINK);
            event.accept(INGOT);
            event.accept(NUGGET);
        }
    }
}

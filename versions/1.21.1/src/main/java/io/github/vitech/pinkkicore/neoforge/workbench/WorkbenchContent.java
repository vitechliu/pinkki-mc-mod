package io.github.vitech.pinkkicore.neoforge.workbench;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class WorkbenchContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ModInfo.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ModInfo.ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModInfo.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ModInfo.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ModInfo.ID);
    private static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_TYPE, ModInfo.ID);
    public static final DeferredHolder<Block, Block> BLOCK = BLOCKS.register("guaba_workbench", GuabaWorkbenchBlock::new);
    public static final DeferredHolder<Item, Item> ITEM = ITEMS.register("guaba_workbench", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuabaWorkbenchEntity>> ENTITY = ENTITIES.register("guaba_workbench",
        () -> BlockEntityType.Builder.of(GuabaWorkbenchEntity::new, BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GuabaWorkbenchMenu>> MENU = MENUS.register("guaba_workbench",
        () -> IMenuTypeExtension.create(GuabaWorkbenchMenu::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<GuabaConversionRecipe>> RECIPE = RECIPES.register("guaba_conversion",
        () -> new RecipeType<>() { @Override public String toString() { return "pinkkicore:guaba_conversion"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GuabaConversionRecipe>> SERIALIZER = SERIALIZERS.register("guaba_conversion", GuabaConversionRecipe.Serializer::new);

    private WorkbenchContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        SERIALIZERS.register(bus); RECIPES.register(bus);
    }
}

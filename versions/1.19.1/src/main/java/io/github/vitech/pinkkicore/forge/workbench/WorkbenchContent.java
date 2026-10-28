package io.github.vitech.pinkkicore.forge.workbench;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WorkbenchContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModInfo.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModInfo.ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModInfo.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ModInfo.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ModInfo.ID);
    private static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, ModInfo.ID);
    public static final RegistryObject<Block> BLOCK = BLOCKS.register("guaba_workbench", GuabaWorkbenchBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register("guaba_workbench", () -> new BlockItem(BLOCK.get(), new Item.Properties().tab(net.minecraft.world.item.CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockEntityType<GuabaWorkbenchEntity>> ENTITY = ENTITIES.register("guaba_workbench",
        () -> BlockEntityType.Builder.of(GuabaWorkbenchEntity::new, BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<GuabaWorkbenchMenu>> MENU = MENUS.register("guaba_workbench",
        () -> IForgeMenuType.create(GuabaWorkbenchMenu::new));
    public static final RegistryObject<RecipeType<GuabaConversionRecipe>> RECIPE = RECIPES.register("guaba_conversion",
        () -> new RecipeType<>() { @Override public String toString() { return "pinkkicore:guaba_conversion"; } });
    public static final RegistryObject<RecipeSerializer<GuabaConversionRecipe>> SERIALIZER = SERIALIZERS.register("guaba_conversion", GuabaConversionRecipe.Serializer::new);

    private WorkbenchContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        SERIALIZERS.register(bus); RECIPES.register(bus);
    }
}

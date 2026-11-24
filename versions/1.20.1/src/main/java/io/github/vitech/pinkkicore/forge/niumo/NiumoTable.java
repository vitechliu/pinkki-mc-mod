package io.github.vitech.pinkkicore.forge.niumo;

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

public final class NiumoTable {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModInfo.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModInfo.ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModInfo.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ModInfo.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ModInfo.ID);
    private static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, ModInfo.ID);
    public static final RegistryObject<Block> BLOCK = BLOCKS.register("niumo_table", NiumoTableBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register("niumo_table", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<NiumoTableEntity>> ENTITY = ENTITIES.register("niumo_table",
        () -> BlockEntityType.Builder.of(NiumoTableEntity::new, BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<NiumoTableMenu>> MENU = MENUS.register("niumo_table",
        () -> IForgeMenuType.create(NiumoTableMenu::new));
    public static final RegistryObject<RecipeType<NiumoRecipe>> RECIPE = RECIPES.register("niumo_table",
        () -> new RecipeType<>() { @Override public String toString() { return "pinkkicore:niumo_table"; } });
    public static final RegistryObject<RecipeSerializer<NiumoRecipe>> SERIALIZER = SERIALIZERS.register("niumo_table", NiumoRecipe.Serializer::new);

    private NiumoTable() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        SERIALIZERS.register(bus); RECIPES.register(bus);
    }
}

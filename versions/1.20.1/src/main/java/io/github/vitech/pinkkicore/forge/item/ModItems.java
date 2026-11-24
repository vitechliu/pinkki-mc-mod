package io.github.vitech.pinkkicore.forge.item;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.galgame.GalgameBlocks;
import io.github.vitech.pinkkicore.forge.dream.GuabaBedBlock;
import io.github.vitech.pinkkicore.forge.entity.GuabaDragonVariant;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModInfo.ID);
    public static final RegistryObject<Item> CHARGED_CREEPER_EGG = ITEMS.register("charged_creeper_spawn_egg", ChargedCreeperEggItem::new);
    public static final RegistryObject<Item> GUIDE_BOOK = ITEMS.register("guide_book", io.github.vitech.pinkkicore.forge.guide.GuideItem::new);
    public static final RegistryObject<Item> GUABA_BOW = ITEMS.register("guaba_bow", GuabaBowItem::new);
    public static final RegistryObject<Item> GUABA_SWORD = ITEMS.register("guaba_sword", GuabaSwordItem::new);
    public static final RegistryObject<Item> GUABA = ITEMS.register("guaba", GuabaItem::new);
    public static final RegistryObject<Item> THUNDER_GUABA = ITEMS.register("thunder_guaba",
        () -> new Item(new Item.Properties().stacksTo(64).fireResistant()));
    public static final RegistryObject<Item> CLOUD_DREAM = ITEMS.register("cloud_dream",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GUABA_CLOUD_DREAM = ITEMS.register("guaba_cloud_dream",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NETHER_DREAM = ITEMS.register("nether_dream",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GUABA_NETHER_DREAM = ITEMS.register("guaba_nether_dream",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GUABA_BED = ITEMS.register("guaba_bed",
        () -> new BedItem(GuabaBedBlock.BLOCK.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BINGWA_SPAWN_EGG = ITEMS.register("bingwa_spawn_egg", () ->
        new ForgeSpawnEggItem(ModEntities.BINGWA, 0xEFE4BD, 0xE89328, new Item.Properties()));

    public static final RegistryObject<Item> PINKKI_SPAWN_EGG = ITEMS.register("pinkki_spawn_egg", () ->
        new ForgeSpawnEggItem(ModEntities.PINKKI, 0xF4BFD0, 0xCFF4EB, new Item.Properties()));

    public static final RegistryObject<Item> GUABA_YINGRONG_SPAWN_EGG = ITEMS.register("guaba_yingrong_spawn_egg", () ->
        new ForgeSpawnEggItem(ModEntities.GUABA_YINGRONG, 0x302C21, 0xCBA45A, new Item.Properties()));

    public static final RegistryObject<Item> GUABA_HOOD = ITEMS.register("guaba_hood", () -> new AccessoryItem("guaba_hood"));
    public static final RegistryObject<Item> SWORD_IN_STONE = ITEMS.register("sword_in_stone",
        () -> new BlockItem(GalgameBlocks.SWORD_IN_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GALGAME_ARCADE = ITEMS.register("galgame_arcade",
        () -> new BlockItem(GalgameBlocks.GALGAME_ARCADE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ZHANG_FAV = ITEMS.register("zhang_fav", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NIUMO = ITEMS.register("pinkki_nm", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEMON_SOUL = ITEMS.register("demon_soul", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NIUMO_HELMET = ITEMS.register("niumo_helmet", () -> new AccessoryItem("niumo_helmet"));
    public static final RegistryObject<Item> NIUMO_WHISKERS = ITEMS.register("niumo_whiskers", () -> new AccessoryItem("niumo_whiskers"));
    public static final RegistryObject<Item> GALGAME_SOUL = ITEMS.register("galgame_soul", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FAVORITE_PERSON = ITEMS.register("favorite_person",
        () -> new AccessoryItem("favorite_person"));
    public static final RegistryObject<Item> GALGAME_HEART = ITEMS.register("galgame_heart",
        () -> new AccessoryItem("galgame_heart"));
    public static final RegistryObject<Item> PINK_BLESSING = ITEMS.register("pink_blessing", () -> new AccessoryItem("pink_blessing"));

    public static final RegistryObject<Item> GUABA_DRAGON_SPAWN_EGG = ITEMS.register("guaba_dragon_spawn_egg", () ->
        new ForgeSpawnEggItem(ModEntities.GUABA_DRAGON, 0xB13022, 0x1E689A, new Item.Properties()));

    public static final RegistryObject<Item> GUABA_DRAGON_EGG = dragonEgg(GuabaDragonVariant.NORMAL);
    public static final RegistryObject<Item> PINK_GUABA_DRAGON_EGG = dragonEgg(GuabaDragonVariant.PINK);
    public static final RegistryObject<Item> THUNDER_GUABA_DRAGON_EGG = dragonEgg(GuabaDragonVariant.THUNDER);
    public static final RegistryObject<Item> CLOUD_GUABA_DRAGON_EGG = dragonEgg(GuabaDragonVariant.CLOUD);
    public static final RegistryObject<Item> NETHER_GUABA_DRAGON_EGG = dragonEgg(GuabaDragonVariant.NETHER);

    private static RegistryObject<Item> dragonEgg(GuabaDragonVariant variant) {
        return ITEMS.register(variant.eggId(), () -> new GuabaDragonEggItem(variant));
    }

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeContents);
    }

    private static void addCreativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(BINGWA_SPAWN_EGG);
            event.accept(GUABA_DRAGON_SPAWN_EGG);
            event.accept(GUABA_DRAGON_EGG);
            event.accept(PINK_GUABA_DRAGON_EGG);
            event.accept(THUNDER_GUABA_DRAGON_EGG);
            event.accept(CLOUD_GUABA_DRAGON_EGG);
            event.accept(NETHER_GUABA_DRAGON_EGG);
            event.accept(CHARGED_CREEPER_EGG);
            event.accept(PINKKI_SPAWN_EGG);
            event.accept(GUABA_YINGRONG_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(GUIDE_BOOK);
        if (event.getTabKey() == CreativeModeTabs.COMBAT) { event.accept(GUABA_HOOD); event.accept(PINK_BLESSING); event.accept(GUABA_SWORD); event.accept(GUABA_BOW); }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ZHANG_FAV);
            event.accept(NIUMO);
            event.accept(DEMON_SOUL);
            event.accept(GALGAME_SOUL);
            event.accept(GUABA);
            event.accept(THUNDER_GUABA);
            event.accept(CLOUD_DREAM);
            event.accept(GUABA_CLOUD_DREAM);
            event.accept(NETHER_DREAM);
            event.accept(GUABA_NETHER_DREAM);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FAVORITE_PERSON);
            event.accept(NIUMO_HELMET);
            event.accept(NIUMO_WHISKERS);
            event.accept(GALGAME_HEART);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SWORD_IN_STONE);
            event.accept(io.github.vitech.pinkkicore.forge.niumo.NiumoTable.ITEM);
            event.accept(GALGAME_ARCADE);
            event.accept(WorkbenchContent.ITEM);
            event.accept(GUABA_BED);
            event.accept(io.github.vitech.pinkkicore.forge.charging.ChargingContent.ITEM);
        }
    }
}

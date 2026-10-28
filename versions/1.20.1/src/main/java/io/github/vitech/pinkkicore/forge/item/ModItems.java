package io.github.vitech.pinkkicore.forge.item;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.CreativeModeTabs;
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

    public static final RegistryObject<Item> BINGWA_SPAWN_EGG = ITEMS.register("bingwa_spawn_egg", () ->
        new ForgeSpawnEggItem(ModEntities.BINGWA, 0xEFE4BD, 0xE89328, new Item.Properties()));

    public static final RegistryObject<Item> PINKKI_SPAWN_EGG = ITEMS.register("pinkki_spawn_egg", () ->
        new ForgeSpawnEggItem(ModEntities.PINKKI, 0xF4BFD0, 0xCFF4EB, new Item.Properties()));

    public static final RegistryObject<Item> GUABA_YINGRONG_SPAWN_EGG = ITEMS.register("guaba_yingrong_spawn_egg", () ->
        new ForgeSpawnEggItem(ModEntities.GUABA_YINGRONG, 0x302C21, 0xCBA45A, new Item.Properties()));

    public static final RegistryObject<Item> GUABA_HOOD = ITEMS.register("guaba_hood", () -> new AccessoryItem("guaba_hood"));
    public static final RegistryObject<Item> PINK_BLESSING = ITEMS.register("pink_blessing", () -> new AccessoryItem("pink_blessing"));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeContents);
    }

    private static void addCreativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(BINGWA_SPAWN_EGG);
            event.accept(CHARGED_CREEPER_EGG);
            event.accept(PINKKI_SPAWN_EGG);
            event.accept(GUABA_YINGRONG_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(GUIDE_BOOK);
        if (event.getTabKey() == CreativeModeTabs.COMBAT) { event.accept(GUABA_HOOD); event.accept(PINK_BLESSING); event.accept(GUABA_SWORD); event.accept(GUABA_BOW); }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(GUABA);
            event.accept(THUNDER_GUABA);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(WorkbenchContent.ITEM);
            event.accept(io.github.vitech.pinkkicore.forge.charging.ChargingContent.ITEM);
        }
    }
}

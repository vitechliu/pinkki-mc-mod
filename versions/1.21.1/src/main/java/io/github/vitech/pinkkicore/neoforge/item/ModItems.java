package io.github.vitech.pinkkicore.neoforge.item;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.neoforge.entity.ModEntities;
import io.github.vitech.pinkkicore.neoforge.workbench.WorkbenchContent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ModInfo.ID);
    public static final DeferredHolder<Item, Item> CHARGED_CREEPER_EGG = ITEMS.register("charged_creeper_spawn_egg", ChargedCreeperEggItem::new);
    public static final DeferredHolder<Item, Item> GUIDE_BOOK = ITEMS.register("guide_book", io.github.vitech.pinkkicore.neoforge.guide.GuideItem::new);
    public static final DeferredHolder<Item, Item> GUABA_BOW = ITEMS.register("guaba_bow", GuabaBowItem::new);
    public static final DeferredHolder<Item, Item> GUABA_SWORD = ITEMS.register("guaba_sword", GuabaSwordItem::new);
    public static final DeferredHolder<Item, Item> GUABA = ITEMS.register("guaba", GuabaItem::new);
    public static final DeferredHolder<Item, Item> THUNDER_GUABA = ITEMS.register("thunder_guaba",
        () -> new Item(new Item.Properties().stacksTo(64).fireResistant()));

    public static final DeferredHolder<Item, Item> BINGWA_SPAWN_EGG = ITEMS.register("bingwa_spawn_egg", () ->
        new DeferredSpawnEggItem(ModEntities.BINGWA, 0xEFE4BD, 0xE89328, new Item.Properties()));

    public static final DeferredHolder<Item, Item> PINKKI_SPAWN_EGG = ITEMS.register("pinkki_spawn_egg", () ->
        new DeferredSpawnEggItem(ModEntities.PINKKI, 0xF4BFD0, 0xCFF4EB, new Item.Properties()));

    public static final DeferredHolder<Item, Item> GUABA_YINGRONG_SPAWN_EGG = ITEMS.register("guaba_yingrong_spawn_egg", () ->
        new DeferredSpawnEggItem(ModEntities.GUABA_YINGRONG, 0x302C21, 0xCBA45A, new Item.Properties()));

    public static final DeferredHolder<Item, Item> GUABA_HOOD = ITEMS.register("guaba_hood", () -> new AccessoryItem("guaba_hood"));
    public static final DeferredHolder<Item, Item> PINK_BLESSING = ITEMS.register("pink_blessing", () -> new AccessoryItem("pink_blessing"));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeContents);
    }

    private static void addCreativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(BINGWA_SPAWN_EGG.get());
            event.accept(CHARGED_CREEPER_EGG.get());
            event.accept(PINKKI_SPAWN_EGG.get());
            event.accept(GUABA_YINGRONG_SPAWN_EGG.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(GUIDE_BOOK.get());
        if (event.getTabKey() == CreativeModeTabs.COMBAT) { event.accept(GUABA_HOOD.get()); event.accept(PINK_BLESSING.get()); event.accept(GUABA_SWORD.get()); event.accept(GUABA_BOW.get()); }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(GUABA.get());
            event.accept(THUNDER_GUABA.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(WorkbenchContent.ITEM.get());
            event.accept(io.github.vitech.pinkkicore.neoforge.charging.ChargingContent.ITEM.get());
        }
    }
}

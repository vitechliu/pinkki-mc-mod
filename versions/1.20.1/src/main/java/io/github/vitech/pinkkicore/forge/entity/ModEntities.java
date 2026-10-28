package io.github.vitech.pinkkicore.forge.entity;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModInfo.ID);
    public static final RegistryObject<EntityType<BingwaEntity>> BINGWA = ENTITIES.register("bingwa", () ->
        EntityType.Builder.of(BingwaEntity::new, MobCategory.CREATURE)
            .sized(1.35F, 1.05F).clientTrackingRange(8).build(ModInfo.ID + ":bingwa"));

    public static final RegistryObject<EntityType<PinkkiEntity>> PINKKI = ENTITIES.register("pinkki", () ->
        EntityType.Builder.of(PinkkiEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F).clientTrackingRange(10).build(ModInfo.ID + ":pinkki"));

    public static final RegistryObject<EntityType<GuabaYingrongEntity>> GUABA_YINGRONG = ENTITIES.register("guaba_yingrong", () ->
        EntityType.Builder.of(GuabaYingrongEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F).clientTrackingRange(10).build(ModInfo.ID + ":guaba_yingrong"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::attributes);
        bus.addListener(WildlifeSpawns::register);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GUABA_YINGRONG.get(), GuabaYingrongEntity.createAttributes().build());
        event.put(PINKKI.get(), PinkkiEntity.createAttributes().build());
        event.put(BINGWA.get(), BingwaEntity.createAttributes().build());
    }
}

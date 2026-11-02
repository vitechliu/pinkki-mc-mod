package io.github.vitech.pinkkicore.neoforge.entity;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ModInfo.ID);
    public static final DeferredHolder<EntityType<?>, EntityType<BingwaEntity>> BINGWA = ENTITIES.register("bingwa", () ->
        EntityType.Builder.of(BingwaEntity::new, MobCategory.CREATURE)
            .sized(1.35F, 1.05F).clientTrackingRange(8).build(ModInfo.ID + ":bingwa"));

    public static final DeferredHolder<EntityType<?>, EntityType<PinkkiEntity>> PINKKI = ENTITIES.register("pinkki", () ->
        EntityType.Builder.of(PinkkiEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F).clientTrackingRange(10).build(ModInfo.ID + ":pinkki"));

    public static final DeferredHolder<EntityType<?>, EntityType<GuabaYingrongEntity>> GUABA_YINGRONG = ENTITIES.register("guaba_yingrong", () ->
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

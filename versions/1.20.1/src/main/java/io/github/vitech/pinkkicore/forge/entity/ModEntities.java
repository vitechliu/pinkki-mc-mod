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

    public static final RegistryObject<EntityType<GuabaDragonEntity>> GUABA_DRAGON = ENTITIES.register("guaba_dragon", () ->
        EntityType.Builder.of(GuabaDragonEntity::new, MobCategory.MONSTER)
            .sized(1.3F, 2.9F).fireImmune().clientTrackingRange(10).build(ModInfo.ID + ":guaba_dragon"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::attributes);
        bus.addListener(WildlifeSpawns::register);
        bus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) ->
            event.register(GUABA_DRAGON.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GuabaDragonEntity::canSpawn, net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE));
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GUABA_YINGRONG.get(), GuabaYingrongEntity.createAttributes().build());
        event.put(PINKKI.get(), PinkkiEntity.createAttributes().build());
        event.put(GUABA_DRAGON.get(), GuabaDragonEntity.createAttributes().build());
        event.put(BINGWA.get(), BingwaEntity.createAttributes().build());
    }
}

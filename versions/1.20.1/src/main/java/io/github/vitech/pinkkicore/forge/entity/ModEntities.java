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

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(BINGWA.get(), BingwaEntity.createAttributes().build());
    }
}

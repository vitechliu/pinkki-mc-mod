package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.GuabaDragonVariant;
import io.github.vitech.pinkkicore.forge.entity.PetGuabaDragonEntity;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GuabaDragonModel<T extends Mob & GeoEntity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation(ModInfo.ID, "geo/guaba_dragon.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModInfo.ID, "textures/entity/guaba_dragon.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ModInfo.ID, "animations/guaba_dragon.animation.json");
    @Override public ResourceLocation getModelResource(T entity) { return MODEL; }
    private static final ResourceLocation PINK = variantTexture(GuabaDragonVariant.PINK);
    private static final ResourceLocation CLOUD = variantTexture(GuabaDragonVariant.CLOUD);
    private static final ResourceLocation NETHER = variantTexture(GuabaDragonVariant.NETHER);

    private static ResourceLocation variantTexture(GuabaDragonVariant variant) {
        return new ResourceLocation(ModInfo.ID, "textures/entity/" + variant.id() + ".png");
    }

    @Override public ResourceLocation getTextureResource(T entity) {
        if (!(entity instanceof PetGuabaDragonEntity pet)) return TEXTURE;
        return switch (pet.getVariant()) {
            case PINK -> PINK;
            case CLOUD -> CLOUD;
            case NETHER -> NETHER;
            default -> TEXTURE;
        };
    }
    @Override public ResourceLocation getAnimationResource(T entity) { return ANIMATION; }
}

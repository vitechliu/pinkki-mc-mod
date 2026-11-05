package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.GuabaDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GuabaDragonModel extends GeoModel<GuabaDragonEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(ModInfo.ID, "geo/guaba_dragon.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModInfo.ID, "textures/entity/guaba_dragon.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ModInfo.ID, "animations/guaba_dragon.animation.json");
    @Override public ResourceLocation getModelResource(GuabaDragonEntity entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(GuabaDragonEntity entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(GuabaDragonEntity entity) { return ANIMATION; }
}

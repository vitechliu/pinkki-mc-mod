package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.BingwaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BingwaModel extends GeoModel<BingwaEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(ModInfo.ID, "geo/bingwa.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModInfo.ID, "textures/entity/bingwa.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ModInfo.ID, "animations/bingwa.animation.json");
    @Override public ResourceLocation getModelResource(BingwaEntity entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(BingwaEntity entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(BingwaEntity entity) { return ANIMATION; }
}

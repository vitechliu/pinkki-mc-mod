package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.NiumoMob;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class NiumoMobRenderer extends GeoEntityRenderer<NiumoMob> {
    public NiumoMobRenderer(EntityRendererProvider.Context context, String id) {
        super(context, new Model(id));
        shadowRadius = 0.35F;
    }

    private static final class Model extends GeoModel<NiumoMob> {
        private final ResourceLocation geometry;
        private final ResourceLocation texture;
        private final ResourceLocation animation;

        Model(String id) {
            geometry = new ResourceLocation(ModInfo.ID, "geo/" + id + ".geo.json");
            texture = new ResourceLocation(ModInfo.ID, "textures/entity/" + id + ".png");
            animation = new ResourceLocation(ModInfo.ID, "animations/" + id + ".animation.json");
        }

        @Override public ResourceLocation getModelResource(NiumoMob entity) { return geometry; }
        @Override public ResourceLocation getTextureResource(NiumoMob entity) { return texture; }
        @Override public ResourceLocation getAnimationResource(NiumoMob entity) { return animation; }
    }
}

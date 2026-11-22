package io.github.vitech.pinkkicore.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.entity.ZhangEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class ZhangRenderer extends GeoEntityRenderer<ZhangEntity> {
    public ZhangRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        shadowRadius = 0.35F;
    }

    @Override
    public void render(ZhangEntity entity, float yaw, float tick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float scale = entity.isBig() ? 2F : 1F;
        shadowRadius = 0.35F * scale;
        withScale(scale);
        super.render(entity, yaw, tick, pose, buffers, light);
    }

    private static final class Model extends GeoModel<ZhangEntity> {
        private static final ResourceLocation GEOMETRY = new ResourceLocation(ModInfo.ID, "geo/zhang.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(ModInfo.ID, "textures/entity/zhang.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(ModInfo.ID, "animations/zhang.animation.json");

        @Override public ResourceLocation getModelResource(ZhangEntity entity) { return GEOMETRY; }
        @Override public ResourceLocation getTextureResource(ZhangEntity entity) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(ZhangEntity entity) { return ANIMATION; }
    }
}

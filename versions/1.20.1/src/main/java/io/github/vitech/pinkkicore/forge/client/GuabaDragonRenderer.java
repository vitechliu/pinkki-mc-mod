package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.forge.entity.GuabaDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GuabaDragonRenderer extends GeoEntityRenderer<GuabaDragonEntity> {
    public GuabaDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new GuabaDragonModel());
        shadowRadius = 0.8F;
    }
}

package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.forge.entity.BingwaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BingwaRenderer extends GeoEntityRenderer<BingwaEntity> {
    public BingwaRenderer(EntityRendererProvider.Context context) {
        super(context, new BingwaModel());
        shadowRadius = 0.65F;
    }
}

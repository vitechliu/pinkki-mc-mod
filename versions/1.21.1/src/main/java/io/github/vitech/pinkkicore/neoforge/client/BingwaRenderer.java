package io.github.vitech.pinkkicore.neoforge.client;

import io.github.vitech.pinkkicore.neoforge.entity.BingwaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BingwaRenderer extends GeoEntityRenderer<BingwaEntity> {
    public BingwaRenderer(EntityRendererProvider.Context context) {
        super(context, new BingwaModel());
        shadowRadius = 0.65F;
    }
}

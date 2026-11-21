package io.github.vitech.pinkkicore.forge.client;

import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GuabaDragonRenderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T> {
    public GuabaDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new GuabaDragonModel<>());
        shadowRadius = 0.8F;
    }
}

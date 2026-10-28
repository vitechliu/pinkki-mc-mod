package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.forge.entity.PinkkiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class PinkkiRenderer extends HumanoidNpcRenderer<PinkkiEntity> {
    public PinkkiRenderer(EntityRendererProvider.Context context) {
        super(context, "pinkki");
    }
}

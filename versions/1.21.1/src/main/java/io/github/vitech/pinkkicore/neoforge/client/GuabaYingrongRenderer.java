package io.github.vitech.pinkkicore.neoforge.client;

import io.github.vitech.pinkkicore.neoforge.entity.GuabaYingrongEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class GuabaYingrongRenderer extends HumanoidNpcRenderer<GuabaYingrongEntity> {
    public GuabaYingrongRenderer(EntityRendererProvider.Context context) {
        super(context, "guaba_yingrong");
    }
}

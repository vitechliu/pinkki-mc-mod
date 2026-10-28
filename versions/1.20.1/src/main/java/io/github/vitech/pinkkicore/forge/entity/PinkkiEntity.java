package io.github.vitech.pinkkicore.forge.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class PinkkiEntity extends HumanoidNpcEntity {
    public PinkkiEntity(EntityType<? extends PinkkiEntity> type, Level level) {
        super(type, level);
    }
}

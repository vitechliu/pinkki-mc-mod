package io.github.vitech.pinkkicore.forge.item;

import io.github.vitech.pinkkicore.forge.entity.GuabaDragonVariant;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class GuabaDragonEggItem extends Item {
    private final GuabaDragonVariant variant;

    public GuabaDragonEggItem(GuabaDragonVariant variant) {
        super(new Properties());
        this.variant = variant;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player owner = context.getPlayer();
        BlockPos ground = context.getClickedPos();
        if (owner == null || context.getClickedFace() != Direction.UP
                || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos pos = ground.above();
        if (!server.mayInteract(owner, ground) || !owner.mayUseItemAt(pos, Direction.UP, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        var dragon = ModEntities.PET_GUABA_DRAGON.get().create(server);
        if (dragon == null) return InteractionResult.FAIL;
        dragon.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, owner.getYRot(), 0);
        var box = dragon.getBoundingBox();
        if (!server.getWorldBorder().isWithinBounds(box) || box.maxY > server.getMaxBuildHeight()
                || !server.noCollision(dragon, box) || server.containsAnyLiquid(box)
                || !server.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).isEmpty()) {
            return InteractionResult.FAIL;
        }
        dragon.initializePet(owner, variant);
        if (!server.addFreshEntity(dragon)) return InteractionResult.FAIL;
        if (!owner.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}

package io.github.vitech.pinkkicore.forge.dream;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Optional;

public final class GuabaBedBlock extends BedBlock {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModInfo.ID);
    public static final RegistryObject<Block> BLOCK = BLOCKS.register("guaba_bed", GuabaBedBlock::new);

    private GuabaBedBlock() {
        super(DyeColor.YELLOW, BlockBehaviour.Properties.copy(Blocks.YELLOW_BED));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    static boolean usesShortSleep(Level level) {
        return !BedBlock.canSetSpawn(level) || !level.dimensionType().natural();
    }

    static boolean isIntact(LevelReader level, BlockPos head, BlockState state) {
        if (!(state.getBlock() instanceof BedBlock) || state.getValue(PART) != BedPart.HEAD) {
            return false;
        }
        Direction facing = state.getValue(FACING);
        BlockPos foot = head.relative(facing.getOpposite());
        BlockState other = level.getBlockState(foot);
        return other.is(state.getBlock()) && other.getValue(PART) == BedPart.FOOT && other.getValue(FACING) == facing;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!usesShortSleep(level)) {
            return super.use(state, level, pos, player, hand, hit);
        }
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer sleeper) || !sleeper.isAlive() || sleeper.isSleeping() || sleeper.isSpectator()) {
            return InteractionResult.CONSUME;
        }

        BlockPos head = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
        if (!level.hasChunkAt(head)) {
            return InteractionResult.CONSUME;
        }
        BlockState bed = level.getBlockState(head);
        if (!bed.is(this) || !isIntact(level, head, bed)) {
            return InteractionResult.CONSUME;
        }
        if (bed.getValue(OCCUPIED)) {
            sleeper.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            return InteractionResult.SUCCESS;
        }

        Player.BedSleepingProblem problem = ForgeEventFactory.onPlayerSleepInBed(sleeper, Optional.of(head));
        if (problem == null) {
            problem = checkShortSleep(sleeper, level, head, bed);
        }
        if (problem != null) {
            Component message = problem.getMessage();
            if (message != null) {
                sleeper.displayClientMessage(message, true);
            }
            return InteractionResult.SUCCESS;
        }

        // Vanilla rejects startSleepInBed in dimensions marked non-natural.
        DreamSleep.beginShortSleep(sleeper, head);
        if (!ForgeEventFactory.fireSleepingTimeCheck(sleeper, Optional.of(head))) {
            DreamSleep.cancel(sleeper);
            sleeper.displayClientMessage(Component.translatable("block.minecraft.bed.no_sleep"), true);
            return InteractionResult.SUCCESS;
        }
        sleeper.setRespawnPosition(level.dimension(), head, sleeper.getYRot(), false, true);
        sleeper.startSleeping(head);
        sleeper.awardStat(Stats.SLEEP_IN_BED);
        CriteriaTriggers.SLEPT_IN_BED.trigger(sleeper);
        sleeper.serverLevel().updateSleepingPlayerList();
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static Player.BedSleepingProblem checkShortSleep(ServerPlayer player, Level level, BlockPos head, BlockState bed) {
        BlockPos foot = head.relative(bed.getValue(FACING).getOpposite());
        if (!inRange(player, head) && !inRange(player, foot)) {
            return Player.BedSleepingProblem.TOO_FAR_AWAY;
        }
        if (!level.getBlockState(head.above()).getCollisionShape(level, head.above()).isEmpty()
            || !level.getBlockState(foot.above()).getCollisionShape(level, foot.above()).isEmpty()) {
            return Player.BedSleepingProblem.OBSTRUCTED;
        }
        if (!player.isCreative()) {
            Vec3 center = Vec3.atBottomCenterOf(head);
            AABB area = new AABB(center, center).inflate(8, 5, 8);
            if (!level.getEntitiesOfClass(Monster.class, area, monster -> monster.isPreventingPlayerRest(player)).isEmpty()) {
                return Player.BedSleepingProblem.NOT_SAFE;
            }
        }
        return null;
    }

    private static boolean inRange(Player player, BlockPos pos) {
        Vec3 center = Vec3.atBottomCenterOf(pos);
        return Math.abs(player.getX() - center.x) <= 3 && Math.abs(player.getY() - center.y) <= 2
            && Math.abs(player.getZ() - center.z) <= 3;
    }

    @Override
    public Optional<Vec3> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader level, BlockPos pos,
                                            float orientation, @Nullable LivingEntity entity) {
        if (!isIntact(level, pos, state)) {
            return Optional.empty();
        }
        return BedBlock.findStandUpPosition(type, level, pos, state.getValue(FACING), orientation);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}

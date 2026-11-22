package io.github.vitech.pinkkicore.forge.galgame;

import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.entity.ZhangEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SwordInStoneBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape BASE = box(0, 0, 1, 16, 3, 15);
    private static final VoxelShape TURNED_BASE = box(1, 0, 0, 15, 3, 16);
    private static final VoxelShape SWORD = Shapes.or(BASE, box(4, 3, 6, 12, 16, 10));
    private static final VoxelShape TURNED_SWORD = Shapes.or(TURNED_BASE, box(6, 3, 4, 10, 16, 12));

    public SwordInStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? TURNED_SWORD : SWORD;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? TURNED_BASE : BASE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!server.mayInteract(player, pos) || !player.getAbilities().mayBuild) {
            return InteractionResult.CONSUME;
        }
        if (server.getDifficulty() == Difficulty.PEACEFUL) {
            player.displayClientMessage(Component.translatable("message.pinkkicore.sword_in_stone.peaceful"), true);
            return InteractionResult.CONSUME;
        }
        EntityType<ZhangEntity> type = server.random.nextInt(20) == 0
            ? ModEntities.BIG_ZHANG.get() : ModEntities.LITTLE_ZHANG.get();
        ZhangEntity zhang = candidate(server, pos, state, type);
        if (zhang == null && type == ModEntities.BIG_ZHANG.get()) {
            zhang = candidate(server, pos, state, ModEntities.LITTLE_ZHANG.get());
        }
        if (zhang == null) {
            player.displayClientMessage(Component.translatable("message.pinkkicore.sword_in_stone.space"), true);
            return InteractionResult.CONSUME;
        }
        zhang.setPersistenceRequired();
        if (server.addFreshEntity(zhang) && server.getBlockState(pos).is(this)) {
            server.destroyBlock(pos, false, player);
        }
        return InteractionResult.CONSUME;
    }

    private static ZhangEntity candidate(ServerLevel level, BlockPos pos, BlockState state,
                                         EntityType<ZhangEntity> type) {
        ZhangEntity zhang = type.create(level);
        if (zhang == null) {
            return null;
        }
        zhang.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state.getValue(FACING).toYRot(), 0);
        var bounds = zhang.getBoundingBox();
        if (!level.getWorldBorder().isWithinBounds(bounds) || bounds.maxY > level.getMaxBuildHeight()
            || !level.getEntities(zhang, bounds, entity -> entity.isAlive() && entity.blocksBuilding).isEmpty()) {
            return null;
        }
        // The sword is the one collision that will be removed by a successful summon.
        var collisions = new BlockCollisions<>(level, zhang, bounds, false, (position, shape) -> position.immutable());
        while (collisions.hasNext()) {
            if (!collisions.next().equals(pos)) {
                return null;
            }
        }
        return zhang;
    }
}

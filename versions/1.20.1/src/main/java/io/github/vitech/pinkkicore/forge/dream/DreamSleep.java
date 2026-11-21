package io.github.vitech.pinkkicore.forge.dream;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class DreamSleep {
    private static final int CLOUD_MIN_Y = 192;
    private static final int CLOUD_MAX_Y = 196;
    private static final int CLOUD_SLEEP_TICKS = 100;
    private static final int SHORT_SLEEP_TICKS = 60;
    private static final Map<ServerPlayer, Sleep> SLEEPING = new IdentityHashMap<>();

    private record Sleep(ResourceKey<Level> dimension, BlockPos head, Block bed, boolean shortSleep) {
        boolean at(ServerPlayer player, BlockPos pos) {
            return player.level().dimension().equals(dimension) && head.equals(pos);
        }

        boolean intact(ServerPlayer player) {
            if (!player.level().hasChunkAt(head)) {
                return false;
            }
            BlockState state = player.level().getBlockState(head);
            return state.is(bed) && state.getValue(BedBlock.OCCUPIED)
                && GuabaBedBlock.isIntact(player.level(), head, state);
        }
    }

    private DreamSleep() {}

    static void beginShortSleep(ServerPlayer player, BlockPos head) {
        // Clear the vanilla wake animation timer before a subsequent short sleep.
        if (player.getSleepTimer() != 0) {
            player.stopSleepInBed(true, false);
        }
        SLEEPING.put(player, new Sleep(player.level().dimension(), head.immutable(), GuabaBedBlock.BLOCK.get(), true));
    }

    static void cancel(ServerPlayer player) {
        SLEEPING.remove(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void allowShortSleep(SleepingTimeCheckEvent event) {
        if (event.getResult() != Event.Result.DEFAULT || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Sleep sleep = SLEEPING.get(player);
        if (sleep != null && sleep.shortSleep() && event.getSleepingLocation().filter(pos -> sleep.at(player, pos)).isPresent()) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        BlockPos head = player.getSleepingPos().orElse(null);
        if (!player.isAlive() || player.hasDisconnected() || head == null) {
            cancel(player);
            return;
        }
        Sleep sleep = SLEEPING.get(player);
        if (sleep == null) {
            watchCloudSleep(player, head);
            return;
        }
        if (!sleep.at(player, head) || !sleep.intact(player)) {
            cancel(player);
            if (sleep.shortSleep()) {
                player.stopSleepInBed(true, true);
            }
        } else if (sleep.shortSleep() && player.getSleepTimer() >= SHORT_SLEEP_TICKS) {
            cancel(player);
            player.stopSleepInBed(false, true);
            if (sleep.dimension().equals(Level.NETHER)) {
                reward(player, ModItems.NETHER_DREAM.get());
            }
        }
    }

    private static void watchCloudSleep(ServerPlayer player, BlockPos head) {
        if (!player.level().dimension().equals(Level.OVERWORLD)
            || head.getY() < CLOUD_MIN_Y || head.getY() > CLOUD_MAX_Y) {
            return;
        }
        BlockState state = player.level().getBlockState(head);
        if (state.getBlock() instanceof BedBlock && GuabaBedBlock.isIntact(player.level(), head, state)) {
            SLEEPING.put(player, new Sleep(Level.OVERWORLD, head.immutable(), state.getBlock(), false));
        }
    }

    @SubscribeEvent
    public static void wake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sleep sleep = SLEEPING.remove(player);
            if (sleep != null && !sleep.shortSleep() && !event.wakeImmediately()
                && player.isAlive() && !player.hasDisconnected() && player.getSleepTimer() >= CLOUD_SLEEP_TICKS
                && player.getSleepingPos().filter(pos -> sleep.at(player, pos)).isPresent() && sleep.intact(player)) {
                reward(player, ModItems.CLOUD_DREAM.get());
            }
        }
    }

    private static void reward(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.25, player.getZ(), stack);
            drop.setTarget(player.getUUID());
            drop.setDefaultPickUpDelay();
            player.serverLevel().addFreshEntity(drop);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SLEEPING.remove(player) != null && player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void travel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SLEEPING.remove(player) != null && player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        SLEEPING.clear();
    }
}

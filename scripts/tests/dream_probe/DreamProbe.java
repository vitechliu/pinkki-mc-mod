package io.github.vitech.pinkkicore.test;

import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.dream.GuabaBedBlock;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.workbench.GuabaConversionRecipe;
import io.github.vitech.pinkkicore.forge.workbench.GuabaWorkbenchEntity;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod("pinkkicore_dream_probe")
public final class DreamProbe {
    private final List<ItemEntity> drops = new ArrayList<>();

    public DreamProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::itemSpawn);
    }

    private void itemSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item) {
            drops.add(item);
        }
    }

    private void started(ServerStartedEvent event) {
        try {
            MinecraftServer server = event.getServer();
            ServerLevel overworld = server.overworld();
            ServerLevel nether = server.getLevel(Level.NETHER);
            ServerLevel end = server.getLevel(Level.END);
            require(nether != null && end != null, "test server needs Nether and End");
            overworld.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            overworld.getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(101, server);
            overworld.setDayTime(14000);
            overworld.updateSkyBrightness();
            for (int y : new int[]{191, 192, 196, 197}) {
                cloud(overworld, y, y == 196 ? GuabaBedBlock.BLOCK.get() : Blocks.WHITE_BED, 100,
                    y == 192 || y == 196);
            }
            cloud(overworld, 194, Blocks.WHITE_BED, 99, false);
            interruptedCloud(overworld, nether);
            shortSleep(nether);
            shortSleep(end);
            recipes(overworld);
            vanillaBedStillExplodes(nether);
            if (ModList.get().isLoaded("silentgear")) DreamGearChecks.SilentGear.run(overworld, nether);
            if (ModList.get().isLoaded("tconstruct")) DreamGearChecks.Tinkers.run(overworld, nether);
            if (ModList.get().isLoaded("tetra")) DreamGearChecks.Tetra.run(overworld, nether);
            System.out.println("DREAM_PROBE PASS");
        } catch (Throwable error) {
            System.out.println("DREAM_PROBE FAIL " + error.getMessage());
            error.printStackTrace();
        }
    }

    private static void cloud(ServerLevel level, int height, Block bed, int ticks, boolean shouldReward) {
        BlockPos head = new BlockPos(5, height, 5);
        prepare(level, head);
        place(level, head, bed);
        ServerPlayer player = player(level, head);
        use(level, head, player);
        require(player.isSleeping(), "could not sleep at Y=" + height);
        tickPlayer(player, ticks);
        player.stopSleepInBed(false, true);
        require(player.getInventory().countItem(ModItems.CLOUD_DREAM.get()) == (shouldReward ? 1 : 0),
            "cloud reward differs at Y=" + height + " after " + ticks + " ticks");
        player.stopSleepInBed(true, true);
        require(player.getInventory().countItem(ModItems.CLOUD_DREAM.get()) == (shouldReward ? 1 : 0), "duplicate cloud reward");
    }

    private static void interruptedCloud(ServerLevel overworld, ServerLevel nether) {
        BlockPos head = new BlockPos(5, 195, 5);
        prepare(overworld, head);
        place(overworld, head, Blocks.WHITE_BED);
        ServerPlayer disconnected = player(overworld, head);
        use(overworld, head, disconnected);
        tickPlayer(disconnected, 100);
        disconnected.disconnect();
        require(!disconnected.isSleeping() && disconnected.getInventory().countItem(ModItems.CLOUD_DREAM.get()) == 0,
            "disconnect after 100 ticks granted a dream or left the player asleep");

        ServerPlayer traveller = player(overworld, head);
        use(overworld, head, traveller);
        tickPlayer(traveller, 100);
        traveller.teleportTo(nether, 5.5, 100.0, 5.5, 0, 0);
        require(traveller.level() == nether && !traveller.isSleeping()
            && traveller.getInventory().countItem(ModItems.CLOUD_DREAM.get()) == 0, "dimension travel granted a dream");
        require(!overworld.getBlockState(head).getValue(BedBlock.OCCUPIED), "dimension travel left the source bed occupied");
        nether.removePlayerImmediately(traveller, Entity.RemovalReason.DISCARDED);
    }

    private void shortSleep(ServerLevel level) {
        BlockPos head = new BlockPos(5, 100, 5);
        prepare(level, head);
        ServerPlayer sleeper = player(level, head);
        sleeper.setYRot(180);
        sleeper.getInventory().setItem(0, new ItemStack(ModItems.GUABA_BED.get()));
        BlockPos support = head.south().below();
        var context = new UseOnContext(sleeper, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atBottomCenterOf(support.above()), Direction.UP, support, false));
        require(ModItems.GUABA_BED.get().useOn(context).consumesAction(), "bed item placement failed");
        require(level.getBlockState(head).getValue(BedBlock.PART) == BedPart.HEAD, "bed did not place the head");
        require(level.getBlockState(head.south()).getValue(BedBlock.PART) == BedPart.FOOT, "bed did not place the foot");
        long dayTime = level.getDayTime();
        use(level, head.south(), sleeper);
        require(sleeper.isSleeping(), "foot interaction did not start short sleep in " + level.dimension().location());
        require(sleeper.getRespawnDimension().equals(level.dimension()) && head.equals(sleeper.getRespawnPosition()),
            "short sleep did not set local respawn");

        ServerPlayer other = player(level, head);
        use(level, head, other);
        require(!other.isSleeping(), "occupied bed accepted another player");
        tickPlayer(sleeper, 59);
        require(sleeper.isSleeping(), "short sleep ended early");
        tickPlayer(sleeper, 1);
        require(!sleeper.isSleeping() && level.getDayTime() == dayTime, "short sleep did not end or changed time");
        int expected = level.dimension().equals(Level.NETHER) ? 1 : 0;
        require(sleeper.getInventory().countItem(ModItems.NETHER_DREAM.get()) == expected, "wrong short sleep reward");

        use(level, head, sleeper);
        tickPlayer(sleeper, 10);
        sleeper.stopSleepInBed(true, true);
        require(sleeper.getInventory().countItem(ModItems.NETHER_DREAM.get()) == expected, "early wake rewarded");
        use(level, head, sleeper);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(sleeper));
        require(!sleeper.isSleeping(), "logout left short sleep occupied");
        require(sleeper.getInventory().countItem(ModItems.NETHER_DREAM.get()) == expected, "logout rewarded");

        if (expected == 1) {
            drops.clear();
            fillInventory(other);
            use(level, head, other);
            require(other.isSleeping(), "full-inventory player could not enter bed");
            tickPlayer(other, 60);
            var drop = drops.stream().filter(item -> item.getItem().is(ModItems.NETHER_DREAM.get())).toList();
            require(drop.size() == 1 && drop.get(0).getItem().getCount() == 1, "full inventory dream drop");
            CompoundTag tag = drop.get(0).saveWithoutId(new CompoundTag());
            require(tag.hasUUID("Owner") && tag.getUUID("Owner").equals(other.getUUID()), "dream drop ownership");
            drop.get(0).discard();
        }

        sleeper.kill();
        ServerPlayer respawned = level.getServer().getPlayerList().respawn(sleeper, false);
        require(respawned.level() == level && respawned.position().distanceToSqr(Vec3.atBottomCenterOf(head)) < 20,
            "actual death respawn did not return beside " + level.dimension().location() + " bed");
        use(level, head, other);
        require(other.isSleeping(), "did not enter sleep before destroying bed");
        drops.clear();
        require(level.destroyBlock(head.south(), true), "could not destroy foot");
        tickPlayer(other, 65);
        require(!other.isSleeping() && level.getBlockState(head).isAir(), "bed removal left sleeper or other half");
        require(drops.stream().filter(item -> item.getItem().is(ModItems.GUABA_BED.get())).count() == 1,
            "breaking the foot did not drop exactly one bed");
        require(other.getInventory().countItem(ModItems.NETHER_DREAM.get()) == 0
            && drops.stream().noneMatch(item -> item.getItem().is(ModItems.NETHER_DREAM.get())),
            "destroyed bed paid a dream");
        respawned.setRespawnPosition(level.dimension(), head, 0, false, false);
        respawned.kill();
        var fallback = level.getServer().getPlayerList().respawn(respawned, false);
        require(fallback.level().dimension().equals(Level.OVERWORLD), "missing bed did not fall back to world spawn");
        level.getServer().getPlayerList().remove(fallback);
        place(level, head, GuabaBedBlock.BLOCK.get());
        drops.clear();
        require(level.destroyBlock(head, true) && level.getBlockState(head.south()).isAir(), "head removal left the foot");
        require(drops.stream().filter(item -> item.getItem().is(ModItems.GUABA_BED.get())).count() == 1,
            "breaking the head did not drop exactly one bed");
        System.out.println("DREAM_PROBE short sleep, reward, bed drop and death respawn " + level.dimension().location());
    }

    private static void fillInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static void recipes(ServerLevel level) {
        var manager = level.getRecipeManager();
        var bedCraft = manager.byKey(new ResourceLocation("pinkkicore:guaba_bed")).orElseThrow();
        var bedProcess = (GuabaConversionRecipe) manager.byKey(new ResourceLocation(
            "pinkkicore:guaba_conversion/bed_to_guaba_bed")).orElseThrow();
        int count = 0;
        for (Block block : net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues()) {
            if (block instanceof BedBlock && block != GuabaBedBlock.BLOCK.get()) {
                var bed = new ItemStack(block);
                require(bedCraft.getIngredients().get(0).test(bed), "missing bed in crafting ingredient: " + bed);
                require(bedProcess.getIngredients().get(0).test(bed), "missing bed in processing ingredient: " + bed);
                count++;
            }
        }
        require(count == 16, "expected all 16 vanilla colors in the fixture");
        var self = new ItemStack(ModItems.GUABA_BED.get());
        require(!bedCraft.getIngredients().get(0).test(self) && !bedProcess.getIngredients().get(0).test(self),
            "guaba bed matched its own recipe");
        craft(level, Items.BLUE_BED, ModItems.GUABA_BED.get());
        craft(level, ModItems.CLOUD_DREAM.get(), ModItems.GUABA_CLOUD_DREAM.get());
        craft(level, ModItems.NETHER_DREAM.get(), ModItems.GUABA_NETHER_DREAM.get());

        BlockPos machinePos = new BlockPos(9, 200, 9);
        var state = WorkbenchContent.BLOCK.get().defaultBlockState();
        level.setBlockAndUpdate(machinePos, state);
        var machine = (GuabaWorkbenchEntity) level.getBlockEntity(machinePos);
        require(machine != null, "workbench missing");
        machine.inventory.setStackInSlot(0, new ItemStack(ModItems.GUABA.get()));
        machine.inventory.setStackInSlot(1, new ItemStack(Items.MAGENTA_BED));
        for (int tick = 0; tick < 20; tick++) {
            GuabaWorkbenchEntity.serverTick(level, machinePos, state, machine);
        }
        require(machine.inventory.getStackInSlot(2).is(ModItems.GUABA_BED.get())
            && machine.inventory.getStackInSlot(0).isEmpty() && machine.inventory.getStackInSlot(1).isEmpty(),
            "workbench did not convert one bed with one guaba");
    }

    private static void craft(ServerLevel level, Item ingredient, Item result) {
        var player = player(level, new BlockPos(9, 200, 9));
        var grid = new TransientCraftingContainer(player.inventoryMenu, 2, 2);
        grid.setItem(0, new ItemStack(ModItems.GUABA.get()));
        grid.setItem(3, new ItemStack(ingredient));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
        var output = recipe.assemble(grid, level.registryAccess());
        require(output.is(result) && output.getCount() == 1, "wrong shapeless recipe output");
    }

    private static void vanillaBedStillExplodes(ServerLevel level) {
        BlockPos head = new BlockPos(9, 110, 9);
        prepare(level, head);
        place(level, head, Blocks.RED_BED);
        use(level, head, player(level, head));
        require(level.getBlockState(head).isAir(), "original Nether bed no longer explodes");
    }

    private static void prepare(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 2, 2))) {
            level.setBlockAndUpdate(pos, (pos.getY() == center.getY() - 1 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        level.getEntitiesOfClass(Monster.class, new AABB(center).inflate(9, 6, 9)).forEach(Entity::discard);
    }

    private static void place(ServerLevel level, BlockPos head, Block bed) {
        var state = bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        level.setBlock(head.south(), state.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        level.setBlock(head, state.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
    }

    private static void use(ServerLevel level, BlockPos pos, ServerPlayer player) {
        level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    static ServerPlayer player(ServerLevel level, BlockPos pos) {
        var id = UUID.randomUUID();
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(id, "Dream" + id.toString().substring(0, 8)));
        player.moveTo(pos.getX() + 1.5, pos.getY(), pos.getZ() + 1.5, 0, 0);
        player.setOnGround(true);
        new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player);
        return player;
    }

    static void tickPlayer(ServerPlayer player, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            player.doTick();
        }
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

package io.github.vitech.pinkkicore.test;

import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.entity.NiumoMob;
import io.github.vitech.pinkkicore.forge.entity.NiumoSpawns;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.niumo.NiumoEffect;
import io.github.vitech.pinkkicore.forge.niumo.NiumoSounds;
import io.github.vitech.pinkkicore.forge.niumo.NiumoTable;
import io.github.vitech.pinkkicore.forge.niumo.NiumoTableEntity;
import io.github.vitech.pinkkicore.forge.niumo.NiumoTableMenu;
import io.github.vitech.pinkkicore.forge.workbench.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.StructureTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import static io.github.vitech.pinkkicore.test.GalgameProbe.CENTER;
import static io.github.vitech.pinkkicore.test.GalgameProbe.near;
import static io.github.vitech.pinkkicore.test.GalgameProbe.require;

final class NiumoChecks {
    private NiumoChecks() {}

    static void run(ServerLevel level) {
        sounds(level);
        table(level);
        splashAndCow(level);
        creatures(level);
        if (ModList.get().isLoaded("curios")) Curios.run(level);
        System.out.println("GALGAME_PROBE Niumo recipes, transfer, effect, conversion, combat and loot");
    }

    private static void sounds(ServerLevel level) {
        var probe = new SoundProbe();
        level.setBlockAndUpdate(CENTER, NiumoTable.BLOCK.get().defaultBlockState());
        var table = (NiumoTableEntity) level.getBlockEntity(CENTER);
        var player = GalgameProbe.player(level);
        var menu = (NiumoTableMenu) table.createMenu(1, player.getInventory(), player);
        MinecraftForge.EVENT_BUS.register(probe);
        try {
            require(!menu.clickMenuButton(player, 0) && probe.blocks == 0, "empty table produced sound");
            table.inventory.setStackInSlot(0, new ItemStack(ModItems.GUABA.get()));
            require(menu.clickMenuButton(player, 0) && probe.blocks == 1, "successful table should make one sound");
            table.inventory.setStackInSlot(1, ItemStack.EMPTY);
            require(level.addFreshEntity(player), "potion thrower fixture");
            player.setItemInHand(InteractionHand.MAIN_HAND,
                PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), GuabaPotions.NIUMO_SPLASH.get()));
            Items.SPLASH_POTION.use(level, player, InteractionHand.MAIN_HAND);
            require(probe.players == 1, "Niumo throws should make one sound at launch");
            player.setItemInHand(InteractionHand.MAIN_HAND, PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WATER));
            Items.SPLASH_POTION.use(level, player, InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), GuabaPotions.NIUMO_SPLASH.get()));
            Items.LINGERING_POTION.use(level, player, InteractionHand.MAIN_HAND);
            require(probe.players == 1, "another potion played the Niumo sound");
            System.out.println("GALGAME_PROBE Niumo sound on table success and potion release");
        } finally {
            level.getEntitiesOfClass(ThrownPotion.class, player.getBoundingBox().inflate(5),
                potion -> potion.getOwner() == player).forEach(Entity::discard);
            if (level.getPlayerByUUID(player.getUUID()) != null) level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            level.removeBlock(CENTER, false);
            MinecraftForge.EVENT_BUS.unregister(probe);
        }
    }

    public static final class SoundProbe {
        private int blocks;
        private int players;

        @SubscribeEvent
        public void sound(PlayLevelSoundEvent.AtPosition event) {
            if (event.getSound() == null || !NiumoSounds.RANDOM_ID.equals(event.getSound().value().getLocation())) return;
            if (event.getSource() == SoundSource.BLOCKS) blocks++;
            if (event.getSource() == SoundSource.PLAYERS) players++;
        }
    }

    private static void table(ServerLevel level) {
        level.setBlockAndUpdate(CENTER, NiumoTable.BLOCK.get().defaultBlockState());
        var table = (NiumoTableEntity) level.getBlockEntity(CENTER);
        var player = GalgameProbe.player(level);
        var menu = (NiumoTableMenu) table.createMenu(1, player.getInventory(), player);
        require(!menu.clickMenuButton(player, NiumoTableMenu.CRAFT), "empty input accepted");
        ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
        table.inventory.setStackInSlot(0, potion);
        require(!menu.clickMenuButton(player, 0) && table.inventory.getStackInSlot(0) == potion,
            "other potion accepted or consumed");
        table.inventory.setStackInSlot(0, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
        require(menu.clickMenuButton(player, 0), "water bottle was not processed");
        var result = table.inventory.getStackInSlot(1);
        require(result.is(Items.SPLASH_POTION) && PotionUtils.getPotion(result) == GuabaPotions.NIUMO_SPLASH.get(),
            "splash NBT missing");
        var up = table.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        var down = table.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
        require(up.extractItem(1, 1, true).isEmpty() && down.extractItem(1, 1, false).is(Items.SPLASH_POTION),
            "default sided extraction");
        require(up.insertItem(0, new ItemStack(ModItems.GUABA.get(), 2), false).isEmpty(), "top insertion");
        require(menu.clickMenuButton(player, 0) && menu.clickMenuButton(player, 0) && !menu.clickMenuButton(player, 0)
            && table.inventory.getStackInSlot(1).is(ModItems.NIUMO.get())
            && table.inventory.getStackInSlot(1).getCount() == 2, "repeated clicks or stacking");
        table.inventory.setStackInSlot(0, new ItemStack(Items.GOLDEN_APPLE, 7));
        table.inventory.setStackInSlot(1, ItemStack.EMPTY);
        require(!menu.clickMenuButton(player, 0) && table.inventory.getStackInSlot(0).getCount() == 7,
            "seven apples accepted");
        table.inventory.setStackInSlot(0, new ItemStack(Items.GOLDEN_APPLE, 8));
        table.inventory.setStackInSlot(1, new ItemStack(Items.DIRT));
        require(!menu.clickMenuButton(player, 0) && table.inventory.getStackInSlot(0).getCount() == 8, "mismatched output");
        table.inventory.setStackInSlot(1, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64));
        require(!menu.clickMenuButton(player, 0) && table.inventory.getStackInSlot(0).getCount() == 8, "full output");
        table.inventory.setStackInSlot(1, ItemStack.EMPTY);
        require(menu.clickMenuButton(player, 0) && table.inventory.getStackInSlot(0).isEmpty()
            && table.inventory.getStackInSlot(1).is(Items.ENCHANTED_GOLDEN_APPLE), "eight-apple recipe");
        for (ItemStack[] pair : new ItemStack[][]{
            {new ItemStack(Items.RABBIT), new ItemStack(Items.BEEF)},
            {new ItemStack(Items.COOKED_MUTTON), new ItemStack(Items.COOKED_BEEF)},
            {new ItemStack(ModItems.GALGAME_SOUL.get()), new ItemStack(ModItems.DEMON_SOUL.get())},
        }) {
            table.inventory.setStackInSlot(0, pair[0]);
            table.inventory.setStackInSlot(1, ItemStack.EMPTY);
            require(menu.clickMenuButton(player, 0)
                && ItemStack.matches(table.inventory.getStackInSlot(1), pair[1]), "conversion " + pair[0]);
        }
        require(menu.clickMenuButton(player, NiumoTableMenu.FIRST_SIDE + Direction.UP.ordinal()), "side update");
        require(table.sideMode(Direction.UP) == SideMode.OUTPUT && !up.insertItem(0, new ItemStack(Items.BEEF), true).isEmpty(),
            "side mode didn't change");
        table.inventory.setStackInSlot(0, new ItemStack(Items.BEEF, 2));
        var loaded = new NiumoTableEntity(CENTER, table.getBlockState());
        loaded.load(table.saveWithFullMetadata());
        require(loaded.sideMode(Direction.UP) == SideMode.OUTPUT && loaded.inventory.getStackInSlot(0).getCount() == 2
            && loaded.inventory.getStackInSlot(1).is(ModItems.DEMON_SOUL.get()), "table save/load");
        player.setPos(CENTER.getX() + 32, CENTER.getY(), CENTER.getZ());
        require(!menu.clickMenuButton(player, 0) && table.inventory.getStackInSlot(0).getCount() == 2, "distant click");
        level.removeBlock(CENTER, false);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new AABB(CENTER).inflate(1));
        require(drops.stream().anyMatch(drop -> drop.getItem().is(Items.BEEF))
            && drops.stream().anyMatch(drop -> drop.getItem().is(ModItems.DEMON_SOUL.get())), "broken table lost items");
        drops.forEach(Entity::discard);
    }

    private static void splashAndCow(ServerLevel level) {
        var sheep = EntityType.SHEEP.create(level);
        sheep.setPos(7.5, CENTER.getY(), 7.5);
        buff(sheep);
        sheep.addEffect(new MobEffectInstance(MobEffects.POISON, 300));
        sheep.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(), 300));
        require(level.addFreshEntity(sheep), "splash target placement");
        var potion = new ThrownPotion(level, 6, CENTER.getY() + .5, 7.5);
        potion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), GuabaPotions.NIUMO_SPLASH.get()));
        potion.setDeltaMovement(1.7, 0, 0);
        require(level.addFreshEntity(potion), "splash placement");
        potion.tick();
        require(!sheep.hasEffect(MobEffects.MOVEMENT_SPEED) && !sheep.hasEffect(MobEffects.REGENERATION)
            && sheep.hasEffect(MobEffects.POISON) && sheep.hasEffect(GuabaPotions.GUABA.get()), "real splash effect filtering");
        sheep.discard();

        var cow = EntityType.COW.create(level);
        cow.setBaby(true);
        cow.setCustomName(Component.literal("Niumo probe calf"));
        cow.setPos(7.5, CENTER.getY(), 7.5);
        require(level.addFreshEntity(cow), "calf fixture");
        level.setBlockAndUpdate(CENTER.above(2), Blocks.STONE.defaultBlockState());
        NiumoEffect.apply(cow);
        require(cow.isAlive(), "collision consumed the cow");
        level.removeBlock(CENTER.above(2), false);
        NiumoEffect.apply(cow);
        var bulls = level.getEntitiesOfClass(NiumoMob.Bullman.class, new AABB(CENTER).inflate(1));
        require(cow.isRemoved() && bulls.size() == 1
            && bulls.get(0).getCustomName().getString().equals("Niumo probe calf"), "named calf conversion");
        bulls.get(0).discard();
        var mooshroom = EntityType.MOOSHROOM.create(level);
        NiumoEffect.apply(mooshroom);
        require(mooshroom.isAlive(), "mooshroom was converted");
    }

    private static void creatures(ServerLevel level) {
        var bull = ModEntities.BULLMAN.get().create(level);
        var lao = ModEntities.LAO_CAI.get().create(level);
        near(bull.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), .23, "bull speed");
        near(lao.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), .46, "Lao Cai speed");
        var target = EntityType.SHEEP.create(level);
        buff(target);
        require(bull.doHurtTarget(target) && !target.hasEffect(MobEffects.MOVEMENT_SPEED), "bull on-hit");
        require(!lao.canAttack(EntityType.VILLAGER.create(level)) && !lao.canAttack(EntityType.IRON_GOLEM.create(level)),
            "Lao Cai can hurt neutral villagers or golems");
        var attacker = EntityType.SHEEP.create(level);
        lao.setPos(8, CENTER.getY(), 7.5);
        attacker.setPos(9, CENTER.getY(), 7.5);
        require(lao.getTarget() == null, "Lao Cai acquired a target without a hit");
        require(lao.hurt(level.damageSources().mobAttack(attacker), 1) && lao.getTarget() == attacker,
            "Lao Cai did not retaliate");
        var distant = EntityType.SKELETON.create(level);
        var projectile = new Arrow(level, distant);
        lao.invulnerableTime = 0;
        require(lao.hurt(level.damageSources().arrow(projectile, distant), 1) && lao.getTarget() == distant,
            "Lao Cai did not retaliate against the shooter");

        var frog = ModEntities.BINGWA.get().create(level);
        frog.setPos(7, CENTER.getY(), 7.5);
        target.setPos(8.5, CENTER.getY(), 7.5);
        target.invulnerableTime = 0;
        frog.setTarget(target);
        buff(target);
        require(frog.doHurtTarget(target) && target.hasEffect(MobEffects.MOVEMENT_SPEED), "frog cast dispelled too early");
        for (int tick = 0; tick < 6; tick++) frog.aiStep();
        require(!target.hasEffect(MobEffects.MOVEMENT_SPEED), "frog tongue did not dispel");
        for (int tick = 0; tick < 10; tick++) frog.aiStep();
        buff(target);
        target.setPos(14, CENTER.getY(), 7.5);
        require(frog.doHurtTarget(target), "frog second windup");
        for (int tick = 0; tick < 6; tick++) frog.aiStep();
        require(target.hasEffect(MobEffects.MOVEMENT_SPEED), "frog missed tongue dispelled");
        loot(level, bull, new ItemStack(Items.LEATHER));
        loot(level, lao, new ItemStack(Items.POPPY));
    }

    private static void loot(ServerLevel level, NiumoMob entity, ItemStack secondary) {
        var params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, entity.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
        var loot = level.getServer().getLootData().getLootTable(entity.getLootTable());
        int extra = 0, potions = 0;
        var seeds = RandomSource.create(20261124);
        for (int sample = 0; sample < 300; sample++) {
            var stacks = loot.getRandomItems(params, seeds.nextLong());
            require(stacks.stream().filter(stack -> stack.is(ModItems.NIUMO.get())).mapToInt(ItemStack::getCount).sum() == 1,
                "missing guaranteed Niumo drop");
            extra += (int) stacks.stream().filter(stack -> stack.is(secondary.getItem())).count();
            potions += (int) stacks.stream().filter(stack -> stack.is(Items.SPLASH_POTION)
                && PotionUtils.getPotion(stack) == GuabaPotions.NIUMO_SPLASH.get()).count();
        }
        require(extra > 100 && extra < 200, "secondary loot probability " + extra);
        require(entity instanceof NiumoMob.LaoCai ? potions > 30 && potions < 90 : potions == 0, "splash loot " + potions);
    }

    static void spawns(ServerLevel level) {
        long oldTime = level.getDayTime();
        var random = RandomSource.create(47);
        level.setDayTime(6000);
        level.updateSkyBrightness();
        require(!NiumoSpawns.bullman(ModEntities.BULLMAN.get(), level, MobSpawnType.NATURAL, CENTER, random), "daytime bull");
        level.setDayTime(18000);
        level.updateSkyBrightness();
        boolean night = false;
        for (int i = 0; i < 50; i++) night |= NiumoSpawns.bullman(ModEntities.BULLMAN.get(), level, MobSpawnType.NATURAL, CENTER, random);
        require(night, "outdoor nighttime bull placement");
        level.setBlockAndUpdate(CENTER.above(2), Blocks.STONE.defaultBlockState());
        require(!NiumoSpawns.bullman(ModEntities.BULLMAN.get(), level, MobSpawnType.NATURAL, CENTER, random), "covered bull placement");
        level.removeBlock(CENTER.above(2), false);
        level.setDayTime(oldTime);
        level.updateSkyBrightness();

        var village = level.findNearestMapStructure(StructureTags.VILLAGE, CENTER, 32, false);
        require(village != null, "no village for spawn fixture");
        var startChunk = level.getChunkAt(village);
        var structures = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var start = startChunk.getAllStarts().values().stream().filter(entry -> entry.isValid()
            && structures.wrapAsHolder(entry.getStructure()).is(StructureTags.VILLAGE))
            .findFirst().orElseThrow(() -> new AssertionError("Locate did not return a village start chunk: " + village));
        var bounds = start.getBoundingBox();
        BlockPos candidate = null;
        search:
        for (int x = bounds.minX(); x <= bounds.maxX(); x += 3) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z += 3) {
                BlockPos ground = new BlockPos(x, 0, z);
                level.getChunkAt(ground);
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ground);
                if (bounds.isInside(surface) && canSpawnLao(level, surface, random)) {
                    candidate = surface;
                    break search;
                }
            }
        }
        require(candidate != null, "no eligible village surface inside " + bounds);
        // Natural spawning runs in entity-visible chunks. Make the generated test chunk
        // equivalent before inserting the fixture from the startup callback.
        var chunk = new ChunkPos(candidate);
        var chunks = level.getChunkSource();
        chunks.addRegionTicket(TicketType.START, chunk, 2, Unit.INSTANCE);
        var resident = ModEntities.LAO_CAI.get().create(level);
        try {
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while (!level.isPositionEntityTicking(candidate) && System.nanoTime() < deadline) {
                if (!chunks.pollTask()) Thread.yield();
            }
            require(level.isPositionEntityTicking(candidate), "village fixture chunk never started ticking");
            resident.setPos(Vec3.atBottomCenterOf(candidate));
            require(level.addFreshEntity(resident), "village resident fixture");
            require(level.getEntitiesOfClass(NiumoMob.LaoCai.class, new AABB(candidate).inflate(4)).contains(resident),
                "resident not visible to the spawn predicate");
            require(!canSpawnLao(level, candidate, random), "Lao Cai ignored local cap");
            require(!canSpawnLao(level, CENTER, random), "Lao Cai spawned outside a village");
        } finally {
            resident.discard();
            chunks.removeRegionTicket(TicketType.START, chunk, 2, Unit.INSTANCE);
        }
        System.out.println("GALGAME_PROBE Niumo daytime, indoor, actual village and local spawn cap");
    }

    private static boolean canSpawnLao(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 150; i++) {
            if (NiumoSpawns.laoCai(ModEntities.LAO_CAI.get(), level, MobSpawnType.NATURAL, pos, random)) return true;
        }
        return false;
    }

    static void buff(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300));
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300));
    }

    private static final class Curios {
        static void run(ServerLevel level) {
            var wearer = GalgameProbe.player(level);
            var slots = CuriosApi.getCuriosInventory(wearer).resolve().orElseThrow();
            var helmet = new ItemStack(ModItems.NIUMO_HELMET.get());
            for (String slot : new String[]{"head", "pinkki"}) {
                var equipped = slots.getStacksHandler(slot).orElseThrow().getStacks();
                require(equipped.isItemValid(0, helmet), "helmet cannot equip in " + slot);
                equipped.setStackInSlot(0, helmet.copy());
                buff(wearer);
                NiumoEffect.apply(wearer);
                require(wearer.hasEffect(MobEffects.MOVEMENT_SPEED), "helmet did not protect " + slot);
                equipped.setStackInSlot(0, ItemStack.EMPTY);
            }
            NiumoEffect.apply(wearer);
            require(!wearer.hasEffect(MobEffects.MOVEMENT_SPEED), "unequipped helmet protected wearer");
            var necklace = slots.getStacksHandler("necklace").orElseThrow().getStacks();
            var whiskers = new ItemStack(ModItems.NIUMO_WHISKERS.get());
            require(necklace.getSlots() == 1 && necklace.isItemValid(0, whiskers), "necklace slot");
            necklace.setStackInSlot(0, whiskers);
            var sheep = EntityType.SHEEP.create(level);
            buff(sheep);
            sheep.hurt(level.damageSources().playerAttack(wearer), 1);
            require(!sheep.hasEffect(MobEffects.MOVEMENT_SPEED), "whiskers melee");

            require(level.addFreshEntity(wearer), "necklace shooter fixture");
            var arrow = new Arrow(level, wearer);
            require(level.addFreshEntity(arrow), "necklace arrow fixture");
            necklace.setStackInSlot(0, ItemStack.EMPTY);
            sheep.invulnerableTime = 0;
            buff(sheep);
            sheep.hurt(level.damageSources().arrow(arrow, wearer), 1);
            require(!sheep.hasEffect(MobEffects.MOVEMENT_SPEED), "necklace projectile did not snapshot");
            arrow.discard();
            level.removePlayerImmediately(wearer, Entity.RemovalReason.DISCARDED);
        }
    }
}

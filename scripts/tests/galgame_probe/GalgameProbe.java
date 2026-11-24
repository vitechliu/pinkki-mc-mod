package io.github.vitech.pinkkicore.test;

import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.entity.ZhangEntity;
import io.github.vitech.pinkkicore.forge.galgame.GalgameBlocks;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.ore.PinkOreContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

@Mod("pinkkicore_galgame_probe")
public final class GalgameProbe {
    static final BlockPos CENTER = new BlockPos(7, 236, 7);
    private boolean rejectZhang;

    public GalgameProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityJoin);
    }

    private void onEntityJoin(EntityJoinLevelEvent event) {
        if (rejectZhang && event.getEntity() instanceof ZhangEntity) {
            event.setCanceled(true);
        }
    }

    private void started(ServerStartedEvent event) {
        try {
            ServerLevel level = event.getServer().overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
            event.getServer().setDifficulty(Difficulty.NORMAL, true);
            for (int x = -5; x <= 13; x++) {
                for (int z = 0; z <= 14; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, CENTER.getY() - 1, z), Blocks.STONE.defaultBlockState());
                }
            }
            NiumoChecks.run(level);
            recipes(level);
            summon(level);
            combat(level);
            galgame(level);
            if (ModList.get().isLoaded("curios")) {
                Curios.run(level);
            }
            if (ModList.get().isLoaded("silentgear")) {
                GalgameGearChecks.SilentGear.run(level);
            }
            if (ModList.get().isLoaded("tconstruct")) {
                GalgameGearChecks.Tinkers.run(level);
            }
            if (ModList.get().isLoaded("tetra")) {
                GalgameGearChecks.Tetra.run(level);
            }
            NiumoChecks.spawns(level);
            System.out.println("GALGAME_PROBE PASS");
        } catch (Throwable error) {
            System.out.println("GALGAME_PROBE FAIL " + error.getMessage());
            error.printStackTrace();
        }
    }

    private static void recipes(ServerLevel level) {
        Item guaba = ModItems.GUABA.get(), thunder = ModItems.THUNDER_GUABA.get();
        Item pink = PinkOreContent.INGOT.get(), fav = ModItems.ZHANG_FAV.get();
        recipe(level, ModItems.SWORD_IN_STONE.get(), "sword_in_stone", null, Items.STONE_SWORD, null,
            Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE, guaba, thunder, guaba);
        recipe(level, ModItems.GALGAME_ARCADE.get(), "galgame_arcade", pink, pink, pink, pink, fav, pink, pink, pink, pink);
        recipe(level, ModItems.FAVORITE_PERSON.get(), "favorite_person", null, Items.BLACK_DYE, null,
            fav, Items.ARMOR_STAND, fav, null, fav, null);
        recipe(level, ModItems.GALGAME_SOUL.get(), "galgame_soul", pink, Items.DIAMOND, pink,
            Items.EMERALD, ModItems.FAVORITE_PERSON.get(), Items.REDSTONE, pink, Items.GOLD_INGOT, pink);
        recipe(level, ModItems.GALGAME_HEART.get(), "galgame_heart", null, Items.NETHERITE_INGOT, null,
            ModItems.GALGAME_SOUL.get(), Items.HEART_OF_THE_SEA, Items.DIAMOND, null, Items.PINK_WOOL, null);
    }

    private static void recipe(ServerLevel level, Item item, String id, Item... ingredients) {
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(new ResourceLocation("pinkkicore", id)).orElseThrow();
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        }, 3, 3);
        for (int slot = 0; slot < ingredients.length; slot++) {
            if (ingredients[slot] != null) {
                grid.setItem(slot, new ItemStack(ingredients[slot]));
            }
        }
        require(recipe.matches(grid, level), "expected recipe does not match " + id);
        var result = recipe.assemble(grid, level.registryAccess());
        require(result.is(item) && result.getCount() == 1, "recipe result " + id);
        grid.setItem(4, new ItemStack(Items.DIRT));
        require(!recipe.matches(grid, level), "wrong recipe accepted " + id);
    }

    private void summon(ServerLevel level) {
        ServerPlayer player = player(level);
        var sword = GalgameBlocks.SWORD_IN_STONE.get().defaultBlockState();
        level.setBlockAndUpdate(CENTER, sword);
        player.getInventory().setItem(0, new ItemStack(Items.STICK));
        require(!use(level, CENTER, player).consumesAction(), "held items should not trigger sword");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        use(level, CENTER, player);
        require(level.getBlockState(CENTER).is(sword.getBlock()), "peaceful consumed sword");
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        long rareSeed = 0;
        while (RandomSource.create(rareSeed).nextInt(20) != 0) {
            rareSeed++;
        }
        long commonSeed = 0;
        while (RandomSource.create(commonSeed).nextInt(20) == 0) {
            commonSeed++;
        }
        level.random.setSeed(commonSeed);
        use(level, CENTER, player);
        findSummoned(level, false).discard();

        level.setBlockAndUpdate(CENTER, sword);
        level.random.setSeed(rareSeed);
        use(level, CENTER, player);
        findSummoned(level, true).discard();

        level.setBlockAndUpdate(CENTER, sword);
        level.setBlockAndUpdate(CENTER.above(2), Blocks.STONE.defaultBlockState());
        level.random.setSeed(rareSeed);
        use(level, CENTER, player);
        findSummoned(level, false).discard();

        level.setBlockAndUpdate(CENTER, sword);
        level.setBlockAndUpdate(CENTER.above(), Blocks.STONE.defaultBlockState());
        use(level, CENTER, player);
        require(level.getBlockState(CENTER).is(sword.getBlock()), "obstruction consumed sword");
        level.removeBlock(CENTER.above(), false);
        level.removeBlock(CENTER.above(2), false);
        rejectZhang = true;
        use(level, CENTER, player);
        rejectZhang = false;
        require(level.getBlockState(CENTER).is(sword.getBlock()), "rejected entity consumed sword");
        level.removeBlock(CENTER, false);
        System.out.println("GALGAME_PROBE recipes, both spawn rolls, small fallback, failed spawn and Peaceful");
    }

    private static ZhangEntity findSummoned(ServerLevel level, boolean big) {
        require(level.getBlockState(CENTER).isAir(), "successful summon retained sword");
        var entities = level.getEntitiesOfClass(ZhangEntity.class, new AABB(CENTER).inflate(1));
        require(entities.size() == 1 && entities.get(0).isBig() == big, "wrong summoned variant " + big);
        return entities.get(0);
    }

    private static void combat(ServerLevel level) {
        var small = ModEntities.LITTLE_ZHANG.get().create(level);
        var big = ModEntities.BIG_ZHANG.get().create(level);
        near(small.getMaxHealth(), 40, "small health");
        near(big.getMaxHealth(), 160, "big health");
        near(small.getAttributeValue(Attributes.ATTACK_DAMAGE), 14, "small attack");
        near(big.getAttributeValue(Attributes.ATTACK_DAMAGE), 56, "big attack");
        near(big.getAttributeValue(Attributes.MOVEMENT_SPEED), .25 * 4, "big speed");
        near(small.getAttributeValue(Attributes.FOLLOW_RANGE), 48, "follow range");
        near(big.getBbHeight(), small.getBbHeight() * 2, "big height");
        near(big.getBbWidth(), small.getBbWidth() * 2, "big width");
        require(small.canAttack(big) && big.canAttack(small), "same-family attack");
        loot(level, small, 1, 2);
        loot(level, big, 4, 10);

        var target = EntityType.COW.create(level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80);
        target.setHealth(80);
        target.setNoAi(true);
        target.moveTo(4.5, CENTER.getY(), 7.5, 0, 0);
        small.moveTo(7.5, CENTER.getY(), 7.5, 0, 0);
        require(level.addFreshEntity(target) && level.addFreshEntity(small), "targets failed to spawn");
        small.setOnGround(true);
        require(small.getNavigation().createPath(target, 0) != null,
            "invalid pursuit fixture floor=" + level.getBlockState(small.blockPosition().below())
                + " feet=" + level.getBlockState(small.blockPosition()) + " target=" + level.getBlockState(target.blockPosition()));
        for (int tick = 0; tick < 35; tick++) {
            small.tick();
        }
        require(small.getTarget() == target, "nearest suitable living entity was not selected");
        require(small.getX() < 7.25, "pursuit did not move toward target, mob=" + small.position()
            + " target=" + target.position() + " path=" + small.getNavigation().getPath()
            + " grounded=" + small.onGround() + " targetType=" + small.getTarget().getType());
        target.setHealth(80);
        target.invulnerableTime = 0;
        small.setPos(target.getX(), target.getY(), target.getZ());
        small.aiStep();
        near(target.getHealth(), 66, "contact amount");
        target.invulnerableTime = 0;
        small.setPos(target.getX(), target.getY(), target.getZ());
        small.aiStep();
        near(target.getHealth(), 66, "contact cooldown");
        target.discard();
        small.discard();
        System.out.println("GALGAME_PROBE pursuit, variant attributes, contact cooldown and loot tables");
    }

    private static void loot(ServerLevel level, LivingEntity entity, int min, int max) {
        var params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, entity.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
        var loot = level.getServer().getLootData().getLootTable(entity.getLootTable());
        for (int i = 0; i < 40; i++) {
            var stacks = loot.getRandomItems(params, i);
            var stack = stacks.get(0);
            require(stacks.size() == 1 && stack.is(ModItems.ZHANG_FAV.get()) && stack.getCount() >= min
                && stack.getCount() <= max, "invalid Zhang loot");
        }
    }

    private static void galgame(ServerLevel level) {
        var player = player(level);
        var effect = GuabaPotions.GALGAME.get();
        var arcade = GalgameBlocks.GALGAME_ARCADE.get();
        level.setBlockAndUpdate(CENTER, arcade.defaultBlockState());
        use(level, CENTER, player);
        require(player.getEffect(effect).getAmplifier() == 0 && player.getEffect(effect).getDuration() == 12000, "arcade I");
        near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 1.5, "arcade I attack");
        near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), .11, "arcade I speed");
        use(level, CENTER, player);
        use(level, CENTER, player);
        require(player.getEffect(effect).getAmplifier() == 1 && player.getEffect(effect).getDuration() == 12000, "arcade II cap");
        player.addEffect(new MobEffectInstance(effect, 500, 2));
        use(level, CENTER, player);
        require(player.getEffect(effect).getAmplifier() == 2, "arcade replaced stronger effect");
        var target = EntityType.COW.create(level);
        var attacker = EntityType.ZOMBIE.create(level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80);
        target.setHealth(80);
        target.addEffect(new MobEffectInstance(effect, 200, 2));
        target.hurt(level.damageSources().mobAttack(attacker), 10);
        near(target.getHealth(), 76, "level III direct damage multiplier");
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().fall(), 10);
        near(target.getHealth(), 66, "Galgame incorrectly reduced fall damage");
        target.invulnerableTime = 0;
        var arrow = EntityType.ARROW.create(level);
        target.hurt(level.damageSources().arrow(arrow, attacker), 10);
        near(target.getHealth(), 62, "level III projectile damage multiplier");
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().thorns(attacker), 10);
        near(target.getHealth(), 52, "Galgame incorrectly reduced reflected damage");
        level.removeBlock(CENTER, false);
        System.out.println("GALGAME_PROBE arcade duration, upgrade, attributes and source-specific damage reduction");
    }

    static final class Curios {
        static void run(ServerLevel level) {
            var player = player(level);
            var curios = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            var pinkki = curios.getStacksHandler("pinkki").orElseThrow();
            require(pinkki.getSlots() == 2, "pinkki slot count");
            var slots = pinkki.getStacks();
            var heart = new ItemStack(ModItems.GALGAME_HEART.get());
            var favorite = new ItemStack(ModItems.FAVORITE_PERSON.get());
            require(slots.isItemValid(0, heart) && slots.isItemValid(1, favorite), "pinkki item validation");
            var effect = GuabaPotions.GALGAME.get();
            player.addEffect(new MobEffectInstance(effect, 200));
            slots.setStackInSlot(0, heart);
            slots.setStackInSlot(1, favorite);
            tick(player);
            require(player.getEffect(effect).getAmplifier() == 1 && player.getEffect(effect).getDuration() == 200,
                "heart did not increase active level without losing time");

            var zhang = ModEntities.BIG_ZHANG.get().create(level);
            zhang.setTarget(player);
            require(!zhang.canAttack(player) && !zhang.doHurtTarget(player) && player.getHealth() == 20,
                "Favorite Person did not prevent targeted contact");
            GalgameBlocks.GALGAME_ARCADE.get().use(GalgameBlocks.GALGAME_ARCADE.get().defaultBlockState(),
                level, CENTER, player, InteractionHand.MAIN_HAND, hit(CENTER));
            require(player.getEffect(effect).getAmplifier() == 2 && player.getEffect(effect).getDuration() == 12000, "heart arcade III");
            slots.setStackInSlot(1, heart.copy());
            tick(player);
            require(player.getEffect(effect).getAmplifier() == 2, "duplicate heart added more than one level");
            slots.setStackInSlot(0, ItemStack.EMPTY);
            slots.setStackInSlot(1, ItemStack.EMPTY);
            tick(player);
            require(player.getEffect(effect).getAmplifier() == 1 && player.getEffect(effect).getDuration() == 12000, "heart unequip");
            require(zhang.canAttack(player), "Favorite Person protection remained after unequip");

            player.removeAllEffects();
            slots.setStackInSlot(0, heart.copy());
            player.addEffect(new MobEffectInstance(effect, 200));
            player.addEffect(new MobEffectInstance(effect, 20, 1));
            var saved = player.saveWithoutId(new CompoundTag());
            var loaded = player(level);
            loaded.load(saved);
            tick(loaded);
            require(loaded.getEffect(effect).getAmplifier() == 2 && loaded.getEffect(effect).getDuration() == 20,
                "saved heart overlay lost or doubled");
            for (int i = 0; i < 20; i++) {
                loaded.doTick();
            }
            require(loaded.getEffect(effect) != null && loaded.getEffect(effect).getAmplifier() == 1,
                "hidden Galgame effect lost its heart level");
            System.out.println("GALGAME_PROBE Curios dual slot, immunity, dynamic level, duplicate and save/load");
        }
    }

    static ServerPlayer player(ServerLevel level) {
        UUID id = UUID.randomUUID();
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(id, "Gal" + id.toString().substring(0, 8)));
        new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player);
        player.moveTo(CENTER.getX() - 3.5, CENTER.getY(), CENTER.getZ() + .5, -90, 0);
        player.setOnGround(true);
        return player;
    }

    static void tick(ServerPlayer player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }

    private static net.minecraft.world.InteractionResult use(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var state = level.getBlockState(pos);
        return state.use(level, player, InteractionHand.MAIN_HAND, hit(pos));
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    static void near(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < .0001, message + " (expected " + expected + ", got " + actual + ")");
    }

    static void require(boolean passed, String message) {
        if (!passed) {
            throw new AssertionError(message);
        }
    }
}

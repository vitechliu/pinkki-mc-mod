package pinkkicore.test;

import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.charging.ChargingContent;
import io.github.vitech.pinkkicore.forge.charging.GuabaChargingEntity;
import io.github.vitech.pinkkicore.forge.entity.GuabaDragonVariant;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.entity.PetGuabaDragonEntity;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.ore.PinkOreContent;
import io.github.vitech.pinkkicore.forge.workbench.GuabaWorkbenchEntity;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.ModList;

final class PetDragonChecks {
    private record VariantCase(Item egg, GuabaDragonVariant variant, double health, double speed) {}

    private ServerLevel level;
    private FakePlayer owner;
    private FakePlayer otherPlayer;
    private PetGuabaDragonEntity followPet, flyingPet, battlePet;
    private Cow elevatedTarget, target, bystander, ally;
    private ResourceKey<DamageType> expectedLightning;
    private int lightningHits, flightTicks;
    private boolean ownerAttackAcquired, defenseAcquired, blockedBreath, flightBite;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    void start(ServerLevel level) {
        this.level = level;
        for (int chunkX = 8; chunkX <= 10; chunkX++) {
            for (int chunkZ = 0; chunkZ <= 1; chunkZ++) level.setChunkForced(chunkX, chunkZ, true);
        }
        for (int x = 128; x < 166; x++) {
            for (int z = 1; z < 26; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                for (int y = 100; y < 108; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        owner = player("acf8290e-ac58-4d5e-8012-f4fb10668cef", "PetOwner", 146, 7);
        otherPlayer = player("06b1d0b1-3636-44a7-beb9-e80d69c0ae65", "OtherDragonPlayer", 155, 7);
        expectedLightning = ResourceKey.create(Registries.DAMAGE_TYPE, ModList.get().isLoaded("irons_spellbooks")
            ? new ResourceLocation("irons_spellbooks", "lightning_magic") : new ResourceLocation("minecraft", "lightning_bolt"));
        checkEggs();
        checkRecipes();
        checkNetherAttributes();
        followPet = pet(GuabaDragonVariant.NORMAL, 135, 100, 7);
        System.out.println("GUABA_DRAGON_PETS five eggs + consumption + collision + variant/owner/name NBT + all recipes + Nether stats PASS");
    }

    private FakePlayer player(String uuid, String name, double x, double z) {
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString(uuid), name));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(x, 100, z);
        level.addNewPlayer(player);
        return player;
    }

    private void checkEggs() {
        BlockPos floor = new BlockPos(131, 99, 4);
        List<VariantCase> variants = List.of(
            new VariantCase(ModItems.GUABA_DRAGON_EGG.get(), GuabaDragonVariant.NORMAL, 30, 0.23),
            new VariantCase(ModItems.PINK_GUABA_DRAGON_EGG.get(), GuabaDragonVariant.PINK, 120, 0.23),
            new VariantCase(ModItems.THUNDER_GUABA_DRAGON_EGG.get(), GuabaDragonVariant.THUNDER, 30, 0.69),
            new VariantCase(ModItems.CLOUD_GUABA_DRAGON_EGG.get(), GuabaDragonVariant.CLOUD, 30, 0.46),
            new VariantCase(ModItems.NETHER_GUABA_DRAGON_EGG.get(), GuabaDragonVariant.NETHER, 30, 0.23));
        for (VariantCase variant : variants) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(variant.egg(), 2));
            check(useEgg(floor), "valid egg rejected " + variant.variant());
            var dragons = level.getEntitiesOfClass(PetGuabaDragonEntity.class, new AABB(floor).inflate(3));
            check(dragons.size() == 1 && owner.getMainHandItem().getCount() == 1, "single egg spawn and cost");
            var dragon = dragons.get(0);
            check(dragon.getOwner() == owner && dragon.isTame() && dragon.getVariant() == variant.variant(), "incorrect pet identity");
            check(dragon.getMaxHealth() == variant.health() && dragon.getHealth() == variant.health()
                && dragon.getAttributeValue(Attributes.MOVEMENT_SPEED) == variant.speed(), "incorrect variant attributes");
            check(!useEgg(floor) && owner.getMainHandItem().getCount() == 1, "overlapping egg consumed");
            dragon.setCustomName(Component.literal("renamed pet"));
            dragon.setHealth(variant.variant() == GuabaDragonVariant.PINK ? 87 : 21);
            var restored = ModEntities.PET_GUABA_DRAGON.get().create(level);
            restored.load(dragon.saveWithoutId(new CompoundTag()));
            check(restored.getVariant() == variant.variant() && owner.getUUID().equals(restored.getOwnerUUID())
                && restored.getHealth() == dragon.getHealth() && restored.getName().getString().equals("renamed pet"), "pet NBT lost");
            dragon.discard();
        }
        level.setBlockAndUpdate(floor.above(2), Blocks.STONE.defaultBlockState());
        check(!useEgg(floor) && owner.getMainHandItem().getCount() == 1, "egg consumed under low ceiling");
        level.setBlockAndUpdate(floor.above(2), Blocks.AIR.defaultBlockState());
        owner.getAbilities().instabuild = true;
        check(useEgg(floor) && owner.getMainHandItem().getCount() == 1, "creative egg consumed");
        owner.getAbilities().instabuild = false;
        level.getEntitiesOfClass(PetGuabaDragonEntity.class, new AABB(floor).inflate(3)).forEach(PetGuabaDragonEntity::discard);
    }

    private boolean useEgg(BlockPos floor) {
        return owner.getMainHandItem().getItem().useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(floor.above()), Direction.UP, floor, false))).consumesAction();
    }

    private void checkRecipes() {
        CraftingContainer grid = new TransientCraftingContainer(owner.inventoryMenu, 3, 3);
        grid.setItem(0, new ItemStack(Items.EGG));
        grid.setItem(7, new ItemStack(ModItems.GUABA.get()));
        check(craft(grid).is(ModItems.GUABA_DRAGON_EGG.get()), "ordinary shapeless recipe");
        checkRing(grid, PinkOreContent.INGOT.get(), ModItems.PINK_GUABA_DRAGON_EGG.get());
        checkRing(grid, ModItems.CLOUD_DREAM.get(), ModItems.CLOUD_GUABA_DRAGON_EGG.get());
        grid.setItem(1, new ItemStack(ModItems.GUABA_CLOUD_DREAM.get()));
        check(craft(grid).isEmpty(), "mixed Cloud Dream accepted");
        checkRing(grid, ModItems.GUABA_CLOUD_DREAM.get(), ModItems.CLOUD_GUABA_DRAGON_EGG.get());
        checkRing(grid, ModItems.NETHER_DREAM.get(), ModItems.NETHER_GUABA_DRAGON_EGG.get());
        grid.setItem(8, new ItemStack(ModItems.GUABA_NETHER_DREAM.get()));
        check(craft(grid).isEmpty(), "mixed Nether Dream accepted");
        checkRing(grid, ModItems.GUABA_NETHER_DREAM.get(), ModItems.NETHER_GUABA_DRAGON_EGG.get());
        grid.setItem(4, new ItemStack(Items.EGG));
        check(craft(grid).isEmpty(), "variant accepted ordinary chicken egg");

        BlockPos benchPos = new BlockPos(132, 100, 12);
        level.setBlockAndUpdate(benchPos, WorkbenchContent.BLOCK.get().defaultBlockState());
        var bench = (GuabaWorkbenchEntity)level.getBlockEntity(benchPos);
        bench.inventory.setStackInSlot(0, new ItemStack(ModItems.GUABA.get(), 2));
        bench.inventory.setStackInSlot(1, new ItemStack(Items.EGG, 2));
        for (int i = 0; i < 19; i++) GuabaWorkbenchEntity.serverTick(level, benchPos, bench.getBlockState(), bench);
        check(bench.inventory.getStackInSlot(2).isEmpty(), "workbench output too early");
        GuabaWorkbenchEntity.serverTick(level, benchPos, bench.getBlockState(), bench);
        check(bench.inventory.getStackInSlot(2).is(ModItems.GUABA_DRAGON_EGG.get())
            && bench.inventory.getStackInSlot(2).getCount() == 1 && bench.inventory.getStackInSlot(1).getCount() == 1
            && bench.inventory.getStackInSlot(0).getCount() == 1, "workbench egg processing/cost");

        BlockPos chargerPos = benchPos.east(3);
        level.setBlockAndUpdate(chargerPos, ChargingContent.BLOCK.get().defaultBlockState());
        var charger = (GuabaChargingEntity)level.getBlockEntity(chargerPos);
        charger.inventory.setStackInSlot(0, new ItemStack(ModItems.GUABA_DRAGON_EGG.get(), 2));
        for (int i = 0; i < 19; i++) GuabaChargingEntity.serverTick(level, chargerPos, charger.getBlockState(), charger);
        check(charger.inventory.getStackInSlot(1).isEmpty(), "charger output too early");
        GuabaChargingEntity.serverTick(level, chargerPos, charger.getBlockState(), charger);
        check(charger.inventory.getStackInSlot(1).is(ModItems.THUNDER_GUABA_DRAGON_EGG.get())
            && charger.inventory.getStackInSlot(1).getCount() == 1 && charger.inventory.getStackInSlot(0).getCount() == 1,
            "charging egg processing/cost");
    }

    private void checkRing(CraftingContainer grid, Item material, Item output) {
        for (int i = 0; i < 9; i++) grid.setItem(i, new ItemStack(i == 4 ? ModItems.GUABA_DRAGON_EGG.get() : material));
        check(craft(grid).is(output), "shaped recipe for " + output + " with " + material);
    }

    private ItemStack craft(CraftingContainer grid) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
            .map(recipe -> recipe.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private void checkNetherAttributes() {
        var nether = level.getServer().getLevel(Level.NETHER);
        var netherDragon = ModEntities.PET_GUABA_DRAGON.get().create(nether);
        netherDragon.initializePet(owner, GuabaDragonVariant.NETHER);
        check(netherDragon.getHealth() == 90 && netherDragon.getAttributeValue(Attributes.ATTACK_DAMAGE) == 12,
            "Nether tripled attributes missing");
        netherDragon.setHealth(75);
        var saved = netherDragon.saveWithoutId(new CompoundTag());
        var restored = ModEntities.PET_GUABA_DRAGON.get().create(nether);
        restored.load(saved);
        check(restored.getHealth() == 75 && restored.getMaxHealth() == 90
            && restored.getAttributeValue(Attributes.ATTACK_DAMAGE) == 12, "Nether reload damaged or stacked bonus");
        var overworld = ModEntities.PET_GUABA_DRAGON.get().create(level);
        overworld.load(saved);
        check(overworld.getHealth() == 30 && overworld.getMaxHealth() == 30
            && overworld.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4, "Nether bonus leaked to Overworld");
        restored.load(overworld.saveWithoutId(new CompoundTag()));
        check(restored.getHealth() == 30 && restored.getMaxHealth() == 90, "returning to Nether healed dragon");
    }

    private PetGuabaDragonEntity pet(GuabaDragonVariant variant, double x, double y, double z) {
        var pet = ModEntities.PET_GUABA_DRAGON.get().create(level);
        pet.moveTo(x, y, z);
        pet.initializePet(owner, variant);
        level.addFreshEntity(pet);
        return pet;
    }

    private Cow cow(double x, double y, double z) {
        Cow cow = EntityType.COW.create(level);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        cow.setHealth(20);
        cow.setNoAi(true);
        cow.setPos(x, y, z);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        level.addFreshEntity(cow);
        return cow;
    }

    void tick(int ticks) {
        if (ticks == 55) {
            check(followPet.distanceTo(owner) < 7, "pet failed to follow player");
            followPet.discard();
            for (int x = 145; x <= 150; x++) {
                for (int z = 17; z <= 23; z++) level.setBlockAndUpdate(new BlockPos(x, 102, z), Blocks.STONE.defaultBlockState());
            }
            elevatedTarget = cow(147, 103, 20);
            flyingPet = pet(GuabaDragonVariant.CLOUD, 140, 100, 20);
            flyingPet.setTarget(elevatedTarget);
        }
        if (flyingPet != null && flyingPet.isFlying()) flightTicks++;
        if (ticks == 111) {
            check(flightTicks > 0 && flightTicks <= 40 && !flyingPet.isFlying() && !flyingPet.isNoGravity(),
                "Cloud flight invalid duration/gravity " + flightTicks);
            check(flyingPet.getX() > 141 && flightBite, "Cloud flight did not reach and bite the elevated target");
            flyingPet.discard();
            elevatedTarget.discard();
            setupCombat();
        }
        if (ticks > 111 && ticks < 252 && battlePet.getTarget() == target) ownerAttackAcquired = true;
        if (ticks == 239) {
            check(ownerAttackAcquired && lightningHits == 3 && target.getHealth() == 14,
                "owner attack or lightning pulses failed " + lightningHits + "/" + target.getHealth());
            check(bystander.getHealth() == 20 && ally.getHealth() == 20, "breath hit bystander or teammate");
            check(level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class,
                battlePet.getBoundingBox().inflate(12)).isEmpty(), "real lightning spawned");
            finishCombatAndTestDefense();
        }
        if (ticks > 239 && ticks < 315 && battlePet != null) {
            if (battlePet.getTarget() == target) defenseAcquired = true;
            if (!blockedBreath && battlePet.getBreathTicks() > 80) {
                blockedBreath = true;
                for (int y = 100; y < 105; y++) {
                    for (int z = 5; z <= 9; z++) {
                        level.setBlockAndUpdate(new BlockPos(142, y, z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
        if (ticks == 315) {
            check(defenseAcquired && blockedBreath && target.getHealth() == 20, "pet defense or wall occlusion failed");
            System.out.println("GUABA_DRAGON_PETS follow + Cloud flight + both owner combat triggers + player/owner/ally protection + wall + lightning bite/breath "
                + expectedLightning.location() + " PASS");
        }
    }

    private void setupCombat() {
        battlePet = pet(GuabaDragonVariant.THUNDER, 140, 100, 7);
        battlePet.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        check(!battlePet.doHurtTarget(owner) && !battlePet.doHurtTarget(otherPlayer), "pet bit a player");
        var wolf = EntityType.WOLF.create(level);
        wolf.setOwnerUUID(owner.getUUID());
        wolf.setTame(true);
        check(!battlePet.doHurtTarget(wolf), "pet bit same-owner animal");

        var biteTarget = cow(141.5, 100, 7);
        check(battlePet.doHurtTarget(biteTarget) && biteTarget.getHealth() == 16 && lightningHits == 1, "Thunder bite did not deal lightning");
        biteTarget.discard();
        lightningHits = 0;
        target = cow(145, 100, 7);
        bystander = cow(143, 100, 7);
        ally = cow(144, 100, 7);
        var board = level.getScoreboard();
        var team = board.addPlayerTeam("dragon_probe_team");
        board.addPlayerToTeam(owner.getScoreboardName(), team);
        board.addPlayerToTeam(ally.getScoreboardName(), team);
        check(!battlePet.doHurtTarget(ally), "pet bit same-team mob");
        owner.tickCount = 111;
        owner.setLastHurtMob(target);
    }

    private void finishCombatAndTestDefense() {
        battlePet.discard();
        target.discard();
        bystander.discard();
        ally.discard();
        owner.setLastHurtMob(null);
        battlePet = pet(GuabaDragonVariant.NORMAL, 140, 100, 7);
        battlePet.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        target = cow(145, 100, 7);
        owner.tickCount = 239;
        owner.setLastHurtByMob(target);
    }

    void onHurt(LivingHurtEvent event) {
        if (flyingPet != null && event.getSource().getEntity() == flyingPet && event.getEntity() == elevatedTarget) {
            flightBite |= flyingPet.isFlying();
        }
        if (battlePet != null && battlePet.getVariant() == GuabaDragonVariant.THUNDER
                && event.getSource().getEntity() == battlePet) {
            check(event.getSource().is(expectedLightning), "incorrect Thunder damage source " + event.getSource().getMsgId());
            lightningHits++;
        }
    }
}

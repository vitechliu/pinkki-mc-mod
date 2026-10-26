package pinkkicore.test;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.entity.PinkkiEntity;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

/** Runs only in an isolated dedicated server with the reobfuscated release JAR. */
@Mod("pinkkicore_pinkki_probe")
public final class PinkkiProbe {
    private ServerLevel level;
    private PinkkiEntity npc, wanderer;
    private Cow cow;
    private Vec3 start, roamStart;
    private ItemEntity blocked, delayed, trash;
    private boolean moved, retaliated, done;
    private int ticks;
    public PinkkiProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private ItemEntity drop(Item item, double offset) {
        var entity = new ItemEntity(level, npc.getX() + offset, npc.getY(), npc.getZ(), new ItemStack(item));
        entity.setNoPickUpDelay(); level.addFreshEntity(entity); return entity;
    }
    private void checkNames(ServerStartedEvent event) {
        for (var type : new EntityType<?>[]{ModEntities.PINKKI.get(), ModEntities.BINGWA.get()}) {
            var fresh = type.create(level);
            require(fresh.hasCustomName() && fresh.isCustomNameVisible()
                && fresh.getCustomName().equals(type.getDescription()), "default name missing " + type);
            var saved = fresh.saveWithoutId(new CompoundTag());
            var restored = type.create(level); restored.load(saved);
            require(restored.getCustomName().equals(type.getDescription()) && restored.isCustomNameVisible(), "name roundtrip failed");
            saved.remove("CustomName"); saved.putBoolean("CustomNameVisible", false);
            restored.load(saved);
            require(restored.hasCustomName() && restored.isCustomNameVisible(), "old unnamed mob migration failed");
            saved.putString("CustomName", "{\"text\":\"test-name\"}"); saved.remove("CustomNameVisible");
            restored.load(saved);
            require(restored.getCustomName().getString().equals("test-name") && restored.isCustomNameVisible(), "custom summon name overwritten");
            saved.putBoolean("CustomNameVisible", false); restored.load(saved);
            require(!restored.isCustomNameVisible(), "explicit name visibility overwritten");
            var tagger = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("868c947e-1c3b-49af-9fe3-d1b4b745319a"), "PinkkiProbe"));
            var nameTag = new ItemStack(Items.NAME_TAG);
            nameTag.setHoverName(net.minecraft.network.chat.Component.literal("renamed"));
            require(nameTag.interactLivingEntity(tagger, (net.minecraft.world.entity.LivingEntity)fresh, InteractionHand.MAIN_HAND).consumesAction(), "name tag rejected");
            require(fresh.getCustomName().getString().equals("renamed") && fresh.isCustomNameVisible(), "name tag rename failed");
            var renamed = type.create(level); renamed.load(fresh.saveWithoutId(new CompoundTag()));
            require(renamed.getCustomName().getString().equals("renamed") && renamed.isCustomNameVisible(), "renamed NBT lost");
        }
        System.out.println("PINKKI both mobs default names + old NBT migration + visibility + real name tag rename PASS");
    }
    private void started(ServerStartedEvent event) {
        try {
            level = event.getServer().overworld();
            checkNames(event);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
            level.setChunkForced(0, 0, true); level.setChunkForced(1, 0, true);
            for (int x = -4; x <= 35; x++) for (int z = -4; z <= 20; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                for (int y = 100; y <= 105; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
            event.getServer().getCommands().performPrefixedCommand(event.getServer().createCommandSourceStack(),
                "summon pinkkicore:pinkki 1 100 1 {NoAI:1b,CustomName:'\"probe\"'}");
            var list = level.getEntitiesOfClass(PinkkiEntity.class, new AABB(0, 99, 0, 3, 103, 3));
            require(list.size() == 1 && list.get(0).hasCustomName() && list.get(0).isPersistenceRequired()
                && list.get(0).canPickUpLoot() && list.get(0).isCustomNameVisible(), "summon defaults failed");
            list.get(0).discard();
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("868c947e-1c3b-49af-9fe3-d1b4b745319a"), "PinkkiProbe"));
            player.setPos(5, 100, 5);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PINKKI_SPAWN_EGG.get()));
            var result = ModItems.PINKKI_SPAWN_EGG.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(5.5, 100, 5.5), Direction.UP, new BlockPos(5, 99, 5), false)));
            var spawned = level.getEntitiesOfClass(PinkkiEntity.class, new AABB(4, 99, 4, 7, 103, 7));
            require(result.consumesAction() && spawned.size() == 1, "actual egg use failed");
            npc = spawned.get(0); npc.getRandom().setSeed(12345); start = npc.position();
            require(npc.canPickUpLoot() && npc.isPersistenceRequired() && npc.getMaxHealth() == 20
                && npc.hasCustomName() && npc.isCustomNameVisible(), "defaults failed");
            cow = EntityType.COW.create(level); cow.setNoAi(true); cow.setPos(6, 100, 5); level.addFreshEntity(cow);
            wanderer = ModEntities.PINKKI.get().create(level); wanderer.setPos(24,100,8);
            wanderer.getRandom().setSeed(12345); level.addFreshEntity(wanderer); roamStart = wanderer.position();
            System.out.println("PINKKI summon + egg + attributes PASS");
        } catch (Throwable e) { fail(e); }
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || level == null || done) return;
        try {
            ticks++;
            moved |= wanderer.position().distanceToSqr(roamStart) > 1;
            if (ticks > 280 && npc.getTarget() == cow) retaliated = true;
            if (ticks == 40) {
                require(npc.getTarget() == null && cow.getHealth() == cow.getMaxHealth(), "unprovoked attack");
                cow.setPos(12,100,12);
                npc.getNavigation().stop(); npc.setPos(5,100,5); start = npc.position();
                drop(Items.IRON_SWORD, 3);
            }
            if (ticks == 140) {
                require(npc.getMainHandItem().is(Items.IRON_SWORD), "did not seek/pick up sword: pos=" + npc.position() + " ticks=" + npc.tickCount + " target=" + npc.getTarget());
                require(npc.position().distanceToSqr(start) > 1, "did not move to equipment");
                for (Item armor : new Item[]{Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS,Items.SHIELD}) drop(armor,0);
                trash = drop(Items.DIRT,0);
            }
            if (ticks == 180) {
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND})
                    require(!npc.getItemBySlot(slot).isEmpty(), "missing equipment " + slot);
                require(trash.isAlive(), "picked up non-equipment");
                require(npc.getArmorValue() >= 15, "worn armor has no defense");
                require(npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > 2,
                    "weapon did not contribute attack damage");
                var tag = npc.saveWithoutId(new CompoundTag()); var restored = ModEntities.PINKKI.get().create(level); restored.load(tag);
                require(restored.canPickUpLoot() && restored.isPersistenceRequired(), "persistence flags lost");
                for (EquipmentSlot slot : EquipmentSlot.values()) require(ItemStack.matches(npc.getItemBySlot(slot),restored.getItemBySlot(slot)), "NBT equipment lost " + slot);
                drop(Items.DIAMOND_SWORD,0);
                System.out.println("PINKKI seek + pickup + six slots + ignore trash + NBT PASS");
            }
            if (ticks == 220) {
                require(npc.getMainHandItem().is(Items.DIAMOND_SWORD), "weapon upgrade failed");
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
                npc.setNoAi(true); blocked = drop(Items.NETHERITE_SWORD,0); npc.setNoAi(false);
            }
            if (ticks == 240) {
                require(blocked.isAlive() && npc.getMainHandItem().is(Items.DIAMOND_SWORD), "mobGriefing ignored");
                blocked.discard();
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
                npc.setNoAi(true); delayed = drop(Items.NETHERITE_SWORD,0); delayed.setPickUpDelay(60);
            }
            if (ticks == 260) {
                // NoAI prevents movement; turn it back on while the pickup delay still applies.
                npc.setNoAi(false); delayed.setPos(npc.position());
                require(delayed.isAlive(), "delayed item lost");
            }
            if (ticks == 270) require(npc.getMainHandItem().is(Items.DIAMOND_SWORD), "pickup delay ignored");
            if (ticks == 280) {
                delayed.setNoPickUpDelay(); delayed.setPos(npc.position());
            }
            if (ticks == 300) {
                require(npc.getMainHandItem().is(Items.NETHERITE_SWORD), "pickup after delay failed");
                cow.setPos(npc.getX()+1,100,npc.getZ());
                require(npc.hurt(level.damageSources().mobAttack(cow),1), "provocation failed");
            }
            if (ticks == 380) {
                require(retaliated && cow.getHealth() < cow.getMaxHealth(), "retaliation failed");
                cow.discard(); npc.setTarget(null);
                require(npc.hurt(level.damageSources().genericKill(),1000), "kill failed");
                require(level.getEntitiesOfClass(ItemEntity.class,npc.getBoundingBox().inflate(3)).stream()
                    .anyMatch(item -> item.getItem().is(Items.NETHERITE_SWORD)), "picked weapon not dropped on death");
                System.out.println("PINKKI upgrade + griefing + delay + retaliation + equipment death drop PASS");
            }
            if (ticks == 600) {
                require(moved, "random stroll never moved");
                System.out.println("PINKKI autonomous random stroll PASS");
                System.out.println("PINKKI_PROBE PASS"); done=true;
            }
        } catch (Throwable e) { fail(e); }
    }
    private void fail(Throwable e) { done=true; System.out.println("PINKKI_PROBE FAIL " + e); e.printStackTrace(); }
}

package pinkkicore.test;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.entity.BingwaEntity;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

/** Exercises the release JAR in a disposable dedicated server, without a client. */
@Mod("pinkkicore_bingwa_probe")
public final class BingwaProbe {
    private ServerLevel level;
    private BingwaEntity frog;
    private Cow cow;
    private int ticks;
    private boolean done;
    private boolean retaliationSeen;

    public BingwaProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private void started(ServerStartedEvent event) {
        try {
            level = event.getServer().overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
            level.setChunkForced(0, 0, true);
            for (int x = -4; x <= 15; x++) for (int z = -4; z <= 15; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                for (int y = 100; y <= 104; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
            var source = event.getServer().createCommandSourceStack();
            event.getServer().getCommands().performPrefixedCommand(source, "summon pinkkicore:bingwa 1 100 1 {NoAI:1b,CustomName:'\"probe-summoned\"'}");
            var summoned = level.getEntitiesOfClass(BingwaEntity.class, new AABB(0, 100, 0, 3, 103, 3));
            require(summoned.size() == 1, "summon failed");
            var saved = summoned.get(0).saveWithoutId(new CompoundTag());
            var restored = ModEntities.BINGWA.get().create(level);
            restored.load(saved);
            require(restored.hasCustomName() && restored.isNoAi() && restored.getMaxHealth() == 20, "NBT/attributes roundtrip failed");
            summoned.get(0).discard();
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("868c947e-1c3b-49af-9fe3-d1b4b745319a"), "BingwaProbe"));
            player.setPos(5, 100, 5);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BINGWA_SPAWN_EGG.get()));
            var result = ModItems.BINGWA_SPAWN_EGG.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(5.5, 100, 5.5), Direction.UP, new BlockPos(5, 99, 5), false)));
            require(result.consumesAction() && level.getEntitiesOfClass(BingwaEntity.class, new AABB(4, 99, 4, 7, 103, 7)).size() == 1, "egg use did not spawn frog");
            level.getEntitiesOfClass(BingwaEntity.class, new AABB(4, 99, 4, 7, 103, 7)).forEach(entity -> entity.discard());
            frog = ModEntities.BINGWA.get().create(level);
            frog.setPos(8, 100, 8);
            frog.setPersistenceRequired();
            level.addFreshEntity(frog);
            cow = EntityType.COW.create(level);
            cow.setNoAi(true);
            cow.setPos(9.6, 100, 8);
            level.addFreshEntity(cow);
            System.out.println("BINGWA summon + egg use + attributes + NBT PASS");
        } catch (Throwable error) { fail(error); }
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || level == null || done) return;
        try {
            ticks++;
            if (ticks == 40) {
                require(frog.getTarget() == null && cow.getHealth() == cow.getMaxHealth(), "frog attacked unprovoked");
                cow.setPos(frog.getX() + 1.6, frog.getY(), frog.getZ());
                require(frog.hurt(net.minecraft.world.damagesource.DamageSource.mobAttack(cow), 1), "provocation was rejected");
                System.out.println("BINGWA neutral before damage PASS");
            }
            if (ticks > 40 && frog.getTarget() == cow) retaliationSeen = true;
            if (ticks == 240) {
                require(retaliationSeen && cow.getHealth() < cow.getMaxHealth(), "retaliation AI or delayed damage failed: targetSeen=" + retaliationSeen + " health=" + cow.getHealth() + " distance=" + frog.distanceTo(cow) + " frog=" + frog.position() + " cow=" + cow.position());
                frog.setTarget(null);
                cow.discard();
                // Isolate the attack timeline from autonomous acquisition.
                var victim = EntityType.COW.create(level);
                victim.setNoAi(true);
                victim.setPos(frog.getX() + 1.5, 100, frog.getZ());
                level.addFreshEntity(victim);
                frog.setTarget(victim);
                float health = victim.getHealth();
                // A fresh entity has no pending attack or cooldown from the AI scenario.
                var attacker = ModEntities.BINGWA.get().create(level);
                attacker.setPos(frog.position());
                level.addFreshEntity(attacker);
                attacker.setTarget(victim);
                attacker.doHurtTarget(victim);
                require(victim.getHealth() == health, "damage was immediate");
                for (int i = 0; i < 6; i++) attacker.aiStep();
                require(victim.getHealth() < health, "tongue hit not applied at tick 6");
                victim.discard(); attacker.discard();
                // A target moving out of range during the wind-up must not be hit.
                var far = EntityType.COW.create(level);
                far.setNoAi(true); far.setPos(8, 100, 8); level.addFreshEntity(far);
                var ranged = ModEntities.BINGWA.get().create(level);
                ranged.setPos(9.5, 100, 8); ranged.setTarget(far); level.addFreshEntity(ranged);
                ranged.doHurtTarget(far); far.setPos(14, 100, 14);
                for (int i = 0; i < 6; i++) ranged.aiStep();
                require(far.getHealth() == far.getMaxHealth(), "out-of-range target was hit");
                far.discard(); ranged.discard();
                var blockedVictim = EntityType.COW.create(level);
                blockedVictim.setNoAi(true); blockedVictim.setPos(12, 100, 12); level.addFreshEntity(blockedVictim);
                var blockedAttacker = ModEntities.BINGWA.get().create(level);
                blockedAttacker.setPos(10, 100, 12); blockedAttacker.setTarget(blockedVictim); level.addFreshEntity(blockedAttacker);
                level.setBlockAndUpdate(new BlockPos(11, 100, 12), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(11, 101, 12), Blocks.STONE.defaultBlockState());
                blockedAttacker.doHurtTarget(blockedVictim);
                for (int i = 0; i < 6; i++) blockedAttacker.aiStep();
                require(blockedVictim.getHealth() == blockedVictim.getMaxHealth(), "tongue hit through wall");
                require(blockedAttacker.canBreatheUnderwater(), "amphibian cannot breathe underwater");
                System.out.println("BINGWA retaliation + delayed tongue + range/line-of-sight rejection + underwater breathing PASS");
                System.out.println("BINGWA_PROBE PASS");
                done = true;
            }
        } catch (Throwable error) { fail(error); }
    }
    private void fail(Throwable error) {
        done = true;
        System.out.println("BINGWA_PROBE FAIL " + error);
        error.printStackTrace();
    }
}

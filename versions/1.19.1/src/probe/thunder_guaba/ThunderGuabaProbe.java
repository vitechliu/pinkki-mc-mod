package pinkkicore.test;

import io.github.vitech.pinkkicore.forge.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod("pinkkicore_thunder_guaba_probe")
public final class ThunderGuabaProbe {
    private ServerLevel level;
    private final List<ItemEntity> batch = new ArrayList<>();
    private ItemEntity unrelated, distant, visualOnly, canceled;
    private int ticks;
    private boolean finished;

    public ThunderGuabaProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::cancelSelected);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private ItemEntity drop(double x, double z, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, x, 200.01, z, stack, 0, 0, 0);
        entity.setNoGravity(true);
        entity.setPickUpDelay(200);
        level.addFreshEntity(entity);
        return entity;
    }
    private void strike(double x, double z, boolean visual) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        bolt.moveTo(x, 200, z);
        bolt.setVisualOnly(visual);
        level.addFreshEntity(bolt);
    }
    private void cancelSelected(EntityStruckByLightningEvent event) {
        if (event.getEntity() == canceled) event.setCanceled(true);
    }
    private void started(ServerStartedEvent event) {
        try {
            level = event.getServer().overworld();
            level.setChunkForced(0, 0, true);
            level.setChunkForced(1, 0, true);
            level.setChunkForced(2, 0, true);
            level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true, event.getServer());
            for (int x = -2; x <= 5; x++) for (int z = -2; z <= 5; z++)
                level.setBlockAndUpdate(new BlockPos(x, 199, z), Blocks.STONE.defaultBlockState());
            int[] counts = {1, 64, 17, 32};
            for (int i = 0; i < counts.length; i++) {
                ItemStack stack = new ItemStack(ModItems.GUABA.get(), counts[i]);
                stack.setHoverName(Component.literal("Batch " + i));
                stack.getOrCreateTag().putString("probe_stamp", "preserve-" + i);
                batch.add(drop(1 + i % 2, 1 + i / 2, stack));
            }
            unrelated = drop(1.5, 2.5, new ItemStack(Items.DIAMOND, 20));
            distant = drop(40, 1, new ItemStack(ModItems.GUABA.get(), 11));
            visualOnly = drop(12, 1, new ItemStack(ModItems.GUABA.get(), 13));
            canceled = drop(24, 1, new ItemStack(ModItems.GUABA.get(), 19));
            canceled.setInvulnerable(true); // Only isolate the event-cancellation test from fire.
            check(ModItems.THUNDER_GUABA.get().isFireResistant(), "material must be fire resistant");
            check(!ModItems.THUNDER_GUABA.get().isEdible(), "material must not be food");
            check(new ItemStack(ModItems.THUNDER_GUABA.get()).getMaxStackSize() == 64, "stack size");
            strike(1.5, 1.5, false);
            strike(12, 1, true);
            strike(24, 1, false);
        } catch (Throwable failure) { fail(failure); }
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if (finished || level == null || event.phase != TickEvent.Phase.END) return;
        try {
            ticks++;
            if (ticks == 8) {
                int[] counts = {1, 64, 17, 32};
                for (int i = 0; i < batch.size(); i++) {
                    ItemEntity item = batch.get(i);
                    check(item.isAlive() && item.getItem().is(ModItems.THUNDER_GUABA.get()), "conversion " + i);
                    check(item.getItem().getCount() == counts[i], "lost/duplicated items " + i);
                    check(item.getItem().getHoverName().getString().equals("Batch " + i), "custom name lost");
                    check(item.getItem().getTag().getString("probe_stamp").equals("preserve-" + i), "NBT lost");
                    check(item.hasPickUpDelay() && item.getAge() >= 7, "entity pickup delay/age lost");
                    check(!item.hurt(net.minecraft.world.damagesource.DamageSource.IN_FIRE, 99), "fire damaged output");
                    check(!item.hurt(net.minecraft.world.damagesource.DamageSource.LAVA, 99), "lava damaged output");
                }
                check(!unrelated.isAlive(), "unrelated vanilla item lightning behavior changed");
                check(distant.getItem().is(ModItems.GUABA.get()), "out-of-range input converted");
                check(visualOnly.getItem().is(ModItems.GUABA.get()), "visual-only lightning converted input");
                check(canceled.getItem().is(ModItems.GUABA.get()), "canceled lightning converted input");
                System.out.println("THUNDER_GUABA batch 1+64+17+32=114 + NBT/name + entity state + fire/lava + unrelated/distant/visual/canceled controls PASS");
                strike(1.5, 1.5, false);
            }
            if (ticks == 100) {
                int total = batch.stream().mapToInt(item -> {
                    check(item.isAlive() && item.getItem().is(ModItems.THUNDER_GUABA.get()), "product destroyed by later flash");
                    return item.getItem().getCount();
                }).sum();
                check(total == 114, "repeat strike changed output quantity");
                int allProducts = level.getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(-2, 195, -2, 5, 205, 5)).stream()
                    .filter(item -> item.getItem().is(ModItems.THUNDER_GUABA.get()))
                    .mapToInt(item -> item.getItem().getCount()).sum();
                check(allProducts == 114, "duplicate product entities spawned");
                System.out.println("THUNDER_GUABA real lightning repeated strikes + 100 ticks + exact total/no duplicates PASS");
                finished = true;
                System.out.println("THUNDER_GUABA_PROBE PASS");
            }
        } catch (Throwable failure) { fail(failure); }
    }
    private void fail(Throwable failure) {
        finished = true;
        System.out.println("THUNDER_GUABA_PROBE FAIL " + failure);
        failure.printStackTrace();
    }
}

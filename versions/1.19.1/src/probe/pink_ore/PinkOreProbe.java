package pinkkicore.test;

import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.ore.PinkOreContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

/** Checks live registries, tags, loot, recipes and genuinely generated new chunks. */
@Mod("pinkkicore_pink_ore_probe")
public final class PinkOreProbe {
    public PinkOreProbe() { MinecraftForge.EVENT_BUS.addListener(this::started); }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static ResourceLocation id(String path) { return new ResourceLocation("pinkkicore", path); }

    private void started(ServerStartedEvent event) {
        try {
            var server = event.getServer();
            var level = server.overworld();
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PinkOreProbe"));
            var position = new BlockPos(0, 200, 0);
            for (var block : new Block[]{PinkOreContent.ORE.get(), PinkOreContent.DEEPSLATE_ORE.get()}) {
                var state = block.defaultBlockState();
                check(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state), "iron pickaxe rejected");
                check(!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state), "stone pickaxe accepted");
                var normal = Block.getDrops(state, level, position, null, player, new ItemStack(Items.IRON_PICKAXE));
                check(normal.size() == 1 && normal.get(0).is(PinkOreContent.RAW_PINK.get()) && normal.get(0).getCount() == 1, "normal raw drop");
                var silk = new ItemStack(Items.DIAMOND_PICKAXE);
                silk.enchant(Enchantments.SILK_TOUCH, 1);
                var silkDrops = Block.getDrops(state, level, position, null, player, silk);
                check(silkDrops.size() == 1 && silkDrops.get(0).is(block.asItem()) && silkDrops.get(0).getCount() == 1, "silk drop");
                var fortune = new ItemStack(Items.DIAMOND_PICKAXE);
                fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
                int total = 0;
                for (int i = 0; i < 128; i++) {
                    var drops = Block.getDrops(state, level, position, null, player, fortune);
                    check(drops.size() == 1 && drops.get(0).is(PinkOreContent.RAW_PINK.get()), "fortune drop type");
                    int count = drops.get(0).getCount();
                    check(count >= 1 && count <= 4, "fortune III bounds"); total += count;
                }
                check(total > 128, "fortune did not increase yield");
            }
            for (String input : new String[]{"raw_pink", "pink_ore", "deepslate_pink_ore"}) {
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id(input));
                for (String method : new String[]{"smelting", "blasting"}) {
                    var recipe = (AbstractCookingRecipe)level.getRecipeManager().byKey(id("pink_ingot_from_" + method + "_" + input)).orElseThrow();
                    check(recipe.matches(new SimpleContainer(new ItemStack(item)), level), "cooking input " + input);
                    check(!recipe.matches(new SimpleContainer(new ItemStack(Items.IRON_INGOT)), level), "wrong cooking input accepted");
                    check(recipe.getResultItem().is(PinkOreContent.INGOT.get()), "cooking output");
                    check(recipe.getCookingTime() == (method.equals("smelting") ? 200 : 100) && recipe.getExperience() == 1, "cooking time/XP");
                }
            }
            var menu = new AbstractContainerMenu(null, 0) {
                @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                @Override public boolean stillValid(Player p) { return true; }
            };
            for (String[] pair : new String[][]{{"pink_block", "pink_ingot"}, {"raw_pink_block", "raw_pink"}, {"pink_ingot", "pink_nugget"}}) {
                var large = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id(pair[0]));
                var small = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id(pair[1]));
                var recipe = (CraftingRecipe)level.getRecipeManager().byKey(id(pair[0] + "_from_" + pair[1])).orElseThrow();
                var grid = new CraftingContainer(menu, 3, 3);
                for (int i=0; i<9; i++) grid.setItem(i, new ItemStack(small));
                check(recipe.matches(grid, level), "compression match");
                var result = recipe.assemble(grid);
                check(result.is(large) && result.getCount() == 1, "compression quantity");
                grid.setItem(0, ItemStack.EMPTY);
                check(!recipe.matches(grid, level), "incomplete compression accepted");
                var reverse = (CraftingRecipe)level.getRecipeManager().byKey(id(pair[1] + "_from_" + pair[0])).orElseThrow();
                var smallGrid = new CraftingContainer(menu, 2, 2);
                smallGrid.setItem(3, new ItemStack(large));
                check(reverse.matches(smallGrid, level), "2x2 decompression match");
                result = reverse.assemble(smallGrid);
                check(result.is(small) && result.getCount() == 9, "decompression quantity");
            }
            var configured = server.registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY);
            for (String suffix : new String[]{"", "_large", "_buried"}) {
                var pink = (OreConfiguration)configured.get(id("ore_pink" + suffix)).config();
                var vanilla = (OreConfiguration)configured.get(new ResourceLocation("ore_diamond" + (suffix.isEmpty() ? "_small" : suffix))).config();
                check(pink.size == vanilla.size && pink.discardChanceOnAirExposure == vanilla.discardChanceOnAirExposure, "diamond configuration drift");
                check(pink.targetStates.size() == 2 && pink.targetStates.get(0).state.is(PinkOreContent.ORE.get())
                        && pink.targetStates.get(1).state.is(PinkOreContent.DEEPSLATE_ORE.get()), "replacement states");
                var key = ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, id("ore_pink" + suffix));
                check(level.getBiome(new BlockPos(0, -32, 0)).value().getGenerationSettings().features().stream()
                        .flatMap(set -> set.stream()).anyMatch(holder -> holder.is(key)), "overworld biome missing feature " + suffix);
                check(server.getLevel(net.minecraft.world.level.Level.NETHER).getBiome(new BlockPos(0, 32, 0)).value()
                        .getGenerationSettings().features().stream().flatMap(set -> set.stream()).noneMatch(holder -> holder.is(key)), "feature leaked into Nether");
            }
            int ores = 0, deep = 0, lower = 0, upper = 0;
            var cursor = new BlockPos.MutableBlockPos();
            for (int cx=8; cx<12; cx++) for (int cz=8; cz<12; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (int x=0; x<16; x++) for (int z=0; z<16; z++) for (int y=-64; y<32; y++) {
                    cursor.set(cx*16+x, y, cz*16+z);
                    var state = chunk.getBlockState(cursor);
                    if (state.is(PinkOreContent.ORE.get()) || state.is(PinkOreContent.DEEPSLATE_ORE.get())) {
                        ores++;
                        if (state.is(PinkOreContent.DEEPSLATE_ORE.get())) deep++;
                        if (y < -24) lower++; else upper++;
                    }
                }
            }
            check(ores > 0 && deep > 0 && lower > upper, "new chunk ore generation / bottom weighting");
            System.out.println("PINK_ORE_PROBE generated 16 chunks: total="+ores+", deepslate="+deep+", lower="+lower+", upper="+upper);
            System.out.println("PINK_ORE_PROBE PASS iron tool + silk/fortune + six cooking/six crafting recipes + vanilla configuration + biome scope + real chunk generation");
        } catch (Throwable error) {
            error.printStackTrace();
            System.out.println("PINK_ORE_PROBE FAIL " + error);
        }
    }
}

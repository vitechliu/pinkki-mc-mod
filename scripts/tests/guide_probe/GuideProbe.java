package pinkkicore.test;

import io.github.vitech.pinkkicore.forge.guide.GuideBook;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import com.mojang.authlib.GameProfile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

/** Dedicated-server proof for registration, recipe, prose, and client-class isolation. */
@Mod("pinkkicore_guide_probe")
public final class GuideProbe {
    public GuideProbe() { MinecraftForge.EVENT_BUS.addListener(this::started); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void started(ServerStartedEvent event) {
        try {
            var level = event.getServer().overworld();
            check(ModItems.GUIDE_BOOK.get().getMaxStackSize() == 1, "guide must not stack");
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GuideProbe"));
            var stack = new ItemStack(ModItems.GUIDE_BOOK.get()); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var result = stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
            check(result.getResult().consumesAction() && result.getObject() == stack && stack.getCount() == 1, "server-side use changed book");
            var recipe = (CraftingRecipe)level.getRecipeManager().byKey(new ResourceLocation("pinkkicore:guide_book")).orElseThrow();
            var menu = new AbstractContainerMenu(null, 0) {
                @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                @Override public boolean stillValid(Player p) { return true; }
            };
            var grid = new TransientCraftingContainer(menu, 2, 2);
            grid.setItem(0, new ItemStack(Items.BOOK)); grid.setItem(3, new ItemStack(ModItems.GUABA.get()));
            check(recipe.matches(grid, level), "2x2 recipe mismatch");
            check(recipe.assemble(grid, level.registryAccess()).is(ModItems.GUIDE_BOOK.get()), "wrong crafting result");
            grid.setItem(0, new ItemStack(Items.DIRT)); check(!recipe.matches(grid, level), "incorrect book ingredient accepted");
            try (var reader = new InputStreamReader(GuideBook.class.getResourceAsStream("/assets/pinkkicore/guide/zh_cn.json"), StandardCharsets.UTF_8)) {
                var book = GuideBook.read(reader);
                check(book.entries().stream().map(GuideBook.Entry::title).toList().equals(java.util.List.of(
                        "序章", "材料", "工作方块", "基础武器", "药水效果", "生物")), "Wiki chapter order");
                check(book.entries().stream().anyMatch(e -> e.matches("六面") && e.id().equals("work-blocks")), "Chinese body search");
                check(book.entries().stream().anyMatch(e -> e.matches("瓜巴手册") && e.id().equals("handbook")), "Chinese title search");
                check(book.entries().stream().anyMatch(e -> e.matches("瓜巴充能台") && e.id().equals("work-blocks")), "subsection title search");
                var noMods = book.withInstalledMods(mod -> false);
                check(noMods.entries().stream().noneMatch(e -> e.matches("寂静装备") || e.matches("铁魔法") || e.matches("梦系材料联动")),
                        "optional entries searchable with no host mods");
                var tetra = book.withInstalledMods("tetra"::equals);
                check(tetra.entries().stream().anyMatch(e -> e.matches("Tetra") && e.matches("梦系材料联动")), "Tetra material content missing");
                check(tetra.entries().stream().noneMatch(e -> e.matches("匠魂") || e.matches("寂静装备")), "uninstalled hosts shown");
                var twoMods = book.withInstalledMods(mod -> mod.equals("silentgear") || mod.equals("irons_spellbooks"));
                check(twoMods.entries().stream().anyMatch(e -> e.matches("寂静装备")), "Silent Gear material content missing");
                check(twoMods.entries().stream().anyMatch(e -> e.matches("铁魔法充能")), "Iron's Spells content missing");
                for (var entry : book.entries()) for (var block : entry.blocks()) if (block.type().equals("recipe")) {
                    var id = new ResourceLocation(block.id());
                    if (level.getRecipeManager().byKey(id).isPresent()) continue;
                    // Optional host-mod recipes are absent when their actual Forge condition is false.
                    try (var recipeReader = new InputStreamReader(GuideBook.class.getResourceAsStream(
                            "/data/" + id.getNamespace() + "/recipes/" + id.getPath() + ".json"), StandardCharsets.UTF_8)) {
                        var json = com.google.gson.JsonParser.parseReader(recipeReader).getAsJsonObject();
                        check(!net.minecraftforge.common.crafting.CraftingHelper.processConditions(json, "conditions",
                                net.minecraftforge.common.crafting.conditions.ICondition.IContext.TAGS_INVALID), "missing live recipe " + block.id());
                    }
                }
            }
            System.out.println("GUIDE_PROBE PASS registration + 2x2 recipe + server use + bundled Wiki + Chinese search + live recipe IDs");
        } catch (Throwable error) { error.printStackTrace(); System.out.println("GUIDE_PROBE FAIL " + error); }
    }
}

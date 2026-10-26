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
                check(book.entries().size() == 7, "missing Wiki chapters");
                check(book.entries().stream().anyMatch(e -> e.matches("六面") && e.id().equals("guaba-workbench")), "Chinese body search");
                check(book.entries().stream().anyMatch(e -> e.matches("引导手册")), "Chinese title search");
                for (var entry : book.entries()) for (var block : entry.blocks()) if (block.type().equals("recipe"))
                    check(level.getRecipeManager().byKey(new ResourceLocation(block.id())).isPresent(), "missing live recipe " + block.id());
            }
            System.out.println("GUIDE_PROBE PASS registration + 2x2 recipe + server use + bundled Wiki + Chinese search + live recipe IDs");
        } catch (Throwable error) { error.printStackTrace(); System.out.println("GUIDE_PROBE FAIL " + error); }
    }
}

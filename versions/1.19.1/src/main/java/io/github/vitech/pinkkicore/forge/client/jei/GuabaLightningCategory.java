package io.github.vitech.pinkkicore.forge.client.jei;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import io.github.vitech.pinkkicore.forge.client.LegacyGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A JEI view of the world interaction, not a crafting or workbench recipe. */
public final class GuabaLightningCategory implements IRecipeCategory<GuabaLightningCategory.Conversion> {
    public enum Conversion { INSTANCE }
    public static final RecipeType<Conversion> TYPE = RecipeType.create(ModInfo.ID, "guaba_lightning", Conversion.class);
    // One-pixel scanlines form a sharp, pointed lightning silhouette at native GUI resolution.
    private static final int[][] LIGHTNING_ROWS = {
        {10, 16}, {9, 15}, {8, 14}, {7, 14}, {7, 13}, {6, 12}, {5, 12}, {5, 11}, {4, 10}, {3, 15}, {2, 14}, {2, 13},
        {1, 12}, {0, 11}, {6, 10}, {5, 9}, {5, 8}, {5, 8}, {4, 7}, {4, 6}, {3, 5}, {3, 4}, {3, 3}, {2, 2}
    };
    private final IDrawable background, icon, slot;

    public GuabaLightningCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(180, 120);
        icon = helper.createDrawableItemStack(new ItemStack(ModItems.THUNDER_GUABA.get()));
        slot = helper.getSlotDrawable();
    }
    @Override public RecipeType<Conversion> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.pinkkicore.lightning.title"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, Conversion recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 26, 32).setSlotName("guaba")
            .setBackground(slot, -1, -1).addItemStack(new ItemStack(ModItems.GUABA.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 32).setSlotName("thunder_guaba")
            .setBackground(slot, -1, -1).addItemStack(new ItemStack(ModItems.THUNDER_GUABA.get()));
    }
    @Override public void draw(Conversion recipe, IRecipeSlotsView slots, PoseStack pose, double mouseX, double mouseY) {
        LegacyGraphics graphics = new LegacyGraphics(pose);
        var font = Minecraft.getInstance().font;
        // Gold outline, then a flat yellow core: no texture filtering or glow.
        for (int y = 0; y < LIGHTNING_ROWS.length; y++) {
            int left = 82 + LIGHTNING_ROWS[y][0], right = 82 + LIGHTNING_ROWS[y][1];
            graphics.fill(left - 1, y + 2, right + 1, y + 5, 0xFFB88418);
        }
        for (int y = 0; y < LIGHTNING_ROWS.length; y++) {
            graphics.fill(82 + LIGHTNING_ROWS[y][0], y + 3, 82 + LIGHTNING_ROWS[y][1], y + 4, 0xFFFFD447);
        }
        graphics.fill(68, 38, 108, 42, 0xFF657FA8);
        graphics.fill(104, 34, 110, 46, 0xFF657FA8);
        graphics.fill(110, 37, 114, 43, 0xFF657FA8);
        graphics.drawWordWrap(font, Component.translatable("jei.pinkkicore.lightning.condition"), 4, 64, 172, 0xFF444444);
        graphics.drawWordWrap(font, Component.translatable("jei.pinkkicore.lightning.batch"), 4, 94, 172, 0xFF444444);
    }
}

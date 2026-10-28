package io.github.vitech.pinkkicore.forge.client.jei;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.workbench.GuabaConversionRecipe;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import java.util.Arrays;
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

public final class GuabaConversionCategory implements IRecipeCategory<GuabaConversionRecipe> {
    public static final RecipeType<GuabaConversionRecipe> TYPE = RecipeType.create(ModInfo.ID, "guaba_conversion", GuabaConversionRecipe.class);
    private final IDrawable background, icon, slot;
    public GuabaConversionCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(150, 76);
        icon = helper.createDrawableItemStack(new ItemStack(WorkbenchContent.ITEM.get()));
        slot = helper.getSlotDrawable();
    }
    @Override public RecipeType<GuabaConversionRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.pinkkicore.guaba_workbench"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, GuabaConversionRecipe recipe, IFocusGroup focuses) {
        // Keep every tag/custom Ingredient alternative and its actual consumed count.
        var inputs = Arrays.stream(recipe.getIngredients().get(0).getItems())
            .map(stack -> { ItemStack copy = stack.copy(); copy.setCount(recipe.inputCount()); return copy; }).toList();
        builder.addSlot(recipe.guabaCost() == 0 ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT, 8, 7)
            .setSlotName("guaba").setBackground(slot, -1, -1)
            .addItemStack(new ItemStack(ModItems.GUABA.get(), Math.max(1, recipe.guabaCost())))
            .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                recipe.guabaCost() == 0 ? "jei.pinkkicore.guaba.reusable" : "jei.pinkkicore.guaba.consumed", recipe.guabaCost())));
        builder.addSlot(RecipeIngredientRole.INPUT, 34, 39).setSlotName("input").setBackground(slot, -1, -1).addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 39).setSlotName("output").setBackground(slot, -1, -1)
            .addItemStack(recipe.getResultItem());
    }
    @Override public void draw(GuabaConversionRecipe recipe, IRecipeSlotsView slots, PoseStack pose, double mouseX, double mouseY) {
        LegacyGraphics graphics = new LegacyGraphics(pose);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("gui.pinkkicore.catalyst"), 30, 10, 0xFF555555, false);
        graphics.drawString(font, Component.translatable("gui.pinkkicore.input"), 34, 27, 0xFF555555, false);
        graphics.drawString(font, Component.translatable("gui.pinkkicore.output"), 110, 27, 0xFF555555, false);
        graphics.fill(66, 45, 91, 50, 0xFFB84332);
        graphics.fill(86, 41, 91, 54, 0xFFB84332);
        graphics.fill(91, 44, 94, 51, 0xFFB84332);
        graphics.drawCenteredString(font, Component.translatable("jei.pinkkicore.duration", GuabaConversionRecipe.DURATION/20), 75, 64, 0xFF555555);
    }
}

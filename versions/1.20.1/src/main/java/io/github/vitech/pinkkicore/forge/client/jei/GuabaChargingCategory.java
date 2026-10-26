package io.github.vitech.pinkkicore.forge.client.jei;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.charging.GuabaChargingRecipe;
import io.github.vitech.pinkkicore.forge.charging.ChargingContent;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class GuabaChargingCategory implements IRecipeCategory<GuabaChargingRecipe> {
    public static final RecipeType<GuabaChargingRecipe> TYPE = RecipeType.create(ModInfo.ID, "guaba_charging", GuabaChargingRecipe.class);
    private final IDrawable background, icon, slot;
    public GuabaChargingCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(150, 76);
        icon = helper.createDrawableItemStack(new ItemStack(ChargingContent.ITEM.get()));
        slot = helper.getSlotDrawable();
    }
    @Override public RecipeType<GuabaChargingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.pinkkicore.guaba_charging_station"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, GuabaChargingRecipe recipe, IFocusGroup focuses) {
        // Keep every tag/custom Ingredient alternative and its actual consumed count.
        var inputs = Arrays.stream(recipe.getIngredients().get(0).getItems())
            .map(stack -> { ItemStack copy = stack.copy(); copy.setCount(recipe.inputCount()); return copy; }).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 34, 39).setSlotName("input").setBackground(slot, -1, -1).addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 39).setSlotName("output").setBackground(slot, -1, -1)
            .addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }
    @Override public void draw(GuabaChargingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        graphics.drawString(font, Component.translatable("gui.pinkkicore.input"), 34, 27, 0xFF555555, false);
        graphics.drawString(font, Component.translatable("gui.pinkkicore.output"), 110, 27, 0xFF555555, false);
        graphics.fill(66, 45, 91, 50, 0xFF52C8F2);
        graphics.fill(86, 41, 91, 54, 0xFF52C8F2);
        graphics.fill(91, 44, 94, 51, 0xFF52C8F2);
        graphics.drawCenteredString(font, Component.translatable("jei.pinkkicore.duration", GuabaChargingRecipe.DURATION/20), 75, 64, 0xFF555555);
    }
}

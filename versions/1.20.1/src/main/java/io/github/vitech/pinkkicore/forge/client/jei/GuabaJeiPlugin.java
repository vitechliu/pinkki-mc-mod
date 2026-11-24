package io.github.vitech.pinkkicore.forge.client.jei;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.client.GuabaWorkbenchScreen;
import io.github.vitech.pinkkicore.forge.client.GuabaChargingScreen;
import io.github.vitech.pinkkicore.forge.client.NiumoTableScreen;
import io.github.vitech.pinkkicore.forge.client.guide.GuideScreen;
import io.github.vitech.pinkkicore.forge.charging.ChargingContent;
import io.github.vitech.pinkkicore.forge.workbench.WorkbenchContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Only JEI discovers this class. No common registration path loads optional client APIs. */
@JeiPlugin
public final class GuabaJeiPlugin implements IModPlugin {
    private IJeiRuntime runtime;

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) { this.runtime = runtime; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    @Override public ResourceLocation getPluginUid() { return new ResourceLocation(ModInfo.ID, "guaba_workbench"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
            new GuabaChargingCategory(registration.getJeiHelpers().getGuiHelper()),
            new GuabaConversionCategory(registration.getJeiHelpers().getGuiHelper()),
            new GuabaLightningCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(GuabaLightningCategory.TYPE, List.of(GuabaLightningCategory.Conversion.INSTANCE));
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var recipes = level.getRecipeManager().getAllRecipesFor(WorkbenchContent.RECIPE.get()).stream()
            .sorted(Comparator.comparing(recipe -> recipe.getId().toString())).toList();
        registration.addRecipes(GuabaConversionCategory.TYPE, recipes);
        registration.addRecipes(GuabaChargingCategory.TYPE, level.getRecipeManager().getAllRecipesFor(ChargingContent.RECIPE.get()));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(WorkbenchContent.ITEM.get()), GuabaConversionCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ChargingContent.ITEM.get()), GuabaChargingCategory.TYPE);
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(GuideScreen.class, GuideGuiProperties::new);
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(double mouseX, double mouseY) {
                if (runtime == null || !(Minecraft.getInstance().screen instanceof GuideScreen guide)) return Optional.empty();
                return guide.itemAt(mouseX, mouseY).flatMap(item -> runtime.getIngredientManager()
                        .createTypedIngredient(VanillaTypes.ITEM_STACK, item.stack())
                        .map(ingredient -> new GuideIngredient(ingredient, item.area())));
            }
        });
        registration.addRecipeClickArea(GuabaChargingScreen.class, 76, 52, 28, 20, GuabaChargingCategory.TYPE);
        registration.addGuiContainerHandler(GuabaChargingScreen.class, new IGuiContainerHandler<GuabaChargingScreen>() {
            @Override public List<Rect2i> getGuiExtraAreas(GuabaChargingScreen screen) { return screen.getExtraGuiAreas(); }
        });
        registration.addRecipeClickArea(GuabaWorkbenchScreen.class, 76, 52, 28, 20, GuabaConversionCategory.TYPE);
        registration.addGuiContainerHandler(GuabaWorkbenchScreen.class, new IGuiContainerHandler<GuabaWorkbenchScreen>() {
            @Override public List<Rect2i> getGuiExtraAreas(GuabaWorkbenchScreen screen) { return screen.getExtraGuiAreas(); }
        });
        registration.addGuiContainerHandler(NiumoTableScreen.class, new IGuiContainerHandler<NiumoTableScreen>() {
            @Override public List<Rect2i> getGuiExtraAreas(NiumoTableScreen screen) { return screen.getExtraGuiAreas(); }
        });
    }

    // The guide uses the whole screen, including its backdrop. Let JEI handle input without overlaying the reader.
    private record GuideGuiProperties(GuideScreen screen) implements IGuiProperties {
        @Override public Class<GuideScreen> getScreenClass() { return GuideScreen.class; }
        @Override public int getGuiLeft() { return 0; }
        @Override public int getGuiTop() { return 0; }
        @Override public int getGuiXSize() { return getScreenWidth(); }
        @Override public int getGuiYSize() { return getScreenHeight(); }
        @Override public int getScreenWidth() { return screen.width; }
        @Override public int getScreenHeight() { return screen.height; }
    }

    private record GuideIngredient(ITypedIngredient<ItemStack> getTypedIngredient, Rect2i getArea)
            implements IClickableIngredient<ItemStack> {}
}

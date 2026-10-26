package io.github.vitech.pinkkicore.forge.charging;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Data-pack recipe; elapsed time is a machine contract (20 ticks), not recipe-specific. */
public final class GuabaChargingRecipe implements Recipe<SimpleContainer> {
    public static final int DURATION = 20;
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack result;
    private final int inputCount;
    public GuabaChargingRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack result) {
        if (input.isEmpty() || inputCount != 1 || result.isEmpty() || result.getCount() != 1)
            throw new IllegalArgumentException("Charging requires one input and output: " + id);
        this.id=id; this.input=input; this.inputCount=inputCount; this.result=result.copy();
    }
    @Override public boolean matches(SimpleContainer container, Level level) {
        return input.test(container.getItem(0)) && container.getItem(0).getCount() >= inputCount;
    }
    public int inputCount() { return inputCount; }
    @Override public ItemStack assemble(SimpleContainer container, RegistryAccess registries) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return result.copy(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(ChargingContent.ITEM.get()); }
    @Override public RecipeSerializer<?> getSerializer() { return ChargingContent.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ChargingContent.RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<GuabaChargingRecipe> {
        @Override public GuabaChargingRecipe fromJson(ResourceLocation id, JsonObject json) {
            try {
                return new GuabaChargingRecipe(id, Ingredient.fromJson(json.get("ingredient")),
                    GsonHelper.getAsInt(json, "input_count", 1),
                    ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")));
            } catch (IllegalArgumentException error) { throw new JsonSyntaxException("Invalid recipe " + id, error); }
        }
        @Override public GuabaChargingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new GuabaChargingRecipe(id, Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readItem());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, GuabaChargingRecipe recipe) {
            recipe.input.toNetwork(buffer); buffer.writeVarInt(recipe.inputCount); buffer.writeItem(recipe.result);
        }
    }
}

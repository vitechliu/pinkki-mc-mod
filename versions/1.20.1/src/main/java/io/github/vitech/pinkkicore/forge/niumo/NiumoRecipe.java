package io.github.vitech.pinkkicore.forge.niumo;

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
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraft.world.level.Level;

public final class NiumoRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack result;
    private final int inputCount;
    public NiumoRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack result) {
        if (input.isEmpty() || inputCount < 1 || inputCount > 64 || result.isEmpty() || result.getCount() > result.getMaxStackSize())
            throw new IllegalArgumentException("Invalid ingredient or stack count: " + id);
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
    @Override public ItemStack getToastSymbol() { return new ItemStack(NiumoTable.ITEM.get()); }
    @Override public RecipeSerializer<?> getSerializer() { return NiumoTable.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return NiumoTable.RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<NiumoRecipe> {
        @Override public NiumoRecipe fromJson(ResourceLocation id, JsonObject json) {
            try {
                return new NiumoRecipe(id, Ingredient.fromJson(json.get("ingredient")),
                    GsonHelper.getAsInt(json, "input_count", 1),
                    CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true));
            } catch (IllegalArgumentException error) { throw new JsonSyntaxException("Invalid recipe " + id, error); }
        }
        @Override public NiumoRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new NiumoRecipe(id, Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readItem());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, NiumoRecipe recipe) {
            recipe.input.toNetwork(buffer); buffer.writeVarInt(recipe.inputCount); buffer.writeItem(recipe.result);
        }
    }
}

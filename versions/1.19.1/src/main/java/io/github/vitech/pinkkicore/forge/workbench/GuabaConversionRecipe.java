package io.github.vitech.pinkkicore.forge.workbench;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;

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
public final class GuabaConversionRecipe implements Recipe<SimpleContainer> {
    public static final int DURATION = 20;
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack result;
    private final int inputCount;
    private final int guabaCost;
    private final boolean copyInputTag;
    public GuabaConversionRecipe(ResourceLocation id, Ingredient input, int inputCount, int guabaCost, ItemStack result) {
        this(id, input, inputCount, guabaCost, result, false);
    }
    public GuabaConversionRecipe(ResourceLocation id, Ingredient input, int inputCount, int guabaCost, ItemStack result, boolean copyInputTag) {
        if (copyInputTag && (inputCount != 1 || result.getCount() != 1))
            throw new IllegalArgumentException("Tag-preserving conversions require one input and output");
        if (input.isEmpty() || inputCount < 1 || inputCount > 64 || guabaCost < 0 || guabaCost > 64
            || result.isEmpty() || result.getCount() < 1 || result.getCount() > result.getMaxStackSize())
            throw new IllegalArgumentException("Invalid Guaba conversion recipe: " + id);
        this.id = id; this.input = input; this.inputCount = inputCount; this.guabaCost = guabaCost; this.result = result.copy();
        this.copyInputTag = copyInputTag;
    }
    @Override public boolean matches(SimpleContainer container, Level level) {
        return input.test(container.getItem(1)) && container.getItem(1).getCount() >= inputCount;
    }
    public int inputCount() { return inputCount; }
    public int guabaCost() { return guabaCost; }
    @Override public ItemStack assemble(SimpleContainer container) {
        ItemStack output = result.copy();
        // Like vanilla smithing: retain names, enchantments and spent durability only when requested.
        if (copyInputTag && container.getItem(1).hasTag()) output.setTag(container.getItem(1).getTag().copy());
        return output;
    }
    @Override public ItemStack getResultItem() { return result.copy(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(WorkbenchContent.ITEM.get()); }
    @Override public RecipeSerializer<?> getSerializer() { return WorkbenchContent.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return WorkbenchContent.RECIPE.get(); }

    public static final class Serializer implements RecipeSerializer<GuabaConversionRecipe> {
        @Override public GuabaConversionRecipe fromJson(ResourceLocation id, JsonObject json) {
            try {
                return new GuabaConversionRecipe(id, Ingredient.fromJson(json.get("ingredient")),
                    GsonHelper.getAsInt(json, "input_count", 1), GsonHelper.getAsInt(json, "guaba_cost", 1),
                    ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")),
                    GsonHelper.getAsBoolean(json, "copy_input_tag", false));
            } catch (IllegalArgumentException error) { throw new JsonSyntaxException("Invalid recipe " + id, error); }
        }
        @Override public GuabaConversionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new GuabaConversionRecipe(id, Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readVarInt(), buffer.readItem(), buffer.readBoolean());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, GuabaConversionRecipe recipe) {
            recipe.input.toNetwork(buffer); buffer.writeVarInt(recipe.inputCount); buffer.writeVarInt(recipe.guabaCost); buffer.writeItem(recipe.result); buffer.writeBoolean(recipe.copyInputTag);
        }
    }
}

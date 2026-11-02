package io.github.vitech.pinkkicore.neoforge.workbench;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import io.github.vitech.pinkkicore.neoforge.workbench.MachineInput;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Data-pack recipe; elapsed time is a machine contract (20 ticks), not recipe-specific. */
public final class GuabaConversionRecipe implements Recipe<MachineInput> {
    public static final int DURATION = 20;
    private final Ingredient input;
    private final ItemStack result;
    private final int inputCount;
    private final int guabaCost;
    private final boolean copyInputTag;
    public GuabaConversionRecipe(Ingredient input, int inputCount, int guabaCost, ItemStack result) {
        this(input, inputCount, guabaCost, result, false);
    }
    public GuabaConversionRecipe(Ingredient input, int inputCount, int guabaCost, ItemStack result, boolean copyInputTag) {
        if (copyInputTag && (inputCount != 1 || result.getCount() != 1))
            throw new IllegalArgumentException("Tag-preserving conversions require one input and output");
        if (input.isEmpty() || inputCount < 1 || inputCount > 64 || guabaCost < 0 || guabaCost > 64
            || result.isEmpty() || result.getCount() < 1 || result.getCount() > result.getMaxStackSize())
            throw new IllegalArgumentException("Invalid Guaba conversion recipe: ");
        this.input = input; this.inputCount = inputCount; this.guabaCost = guabaCost; this.result = result.copy();
        this.copyInputTag = copyInputTag;
    }
    @Override public boolean matches(MachineInput container, Level level) {
        return input.test(container.getItem(1)) && container.getItem(1).getCount() >= inputCount;
    }
    public int inputCount() { return inputCount; }
    public int guabaCost() { return guabaCost; }
    @Override public ItemStack assemble(MachineInput container, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack output = result.copy();
        // Like vanilla smithing: retain names, enchantments and spent durability only when requested.
        if (copyInputTag) output.applyComponents(container.getItem(1).getComponentsPatch());
        return output;
    }
    @Override public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) { return result.copy(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(WorkbenchContent.ITEM.get()); }
    @Override public RecipeSerializer<?> getSerializer() { return WorkbenchContent.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return WorkbenchContent.RECIPE.get(); }


    public static final class Serializer implements RecipeSerializer<GuabaConversionRecipe> {
        private static final MapCodec<GuabaConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
            Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(r -> r.inputCount),
            Codec.intRange(0, 64).optionalFieldOf("guaba_cost", 1).forGetter(r -> r.guabaCost),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
            Codec.BOOL.optionalFieldOf("copy_input_tag", false).forGetter(r -> r.copyInputTag)
        ).apply(instance, GuabaConversionRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, GuabaConversionRecipe> STREAM = new StreamCodec<>() {
            @Override public GuabaConversionRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new GuabaConversionRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt(), ItemStack.STREAM_CODEC.decode(buffer), buffer.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, GuabaConversionRecipe recipe) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
                buffer.writeVarInt(recipe.inputCount);
                buffer.writeVarInt(recipe.guabaCost);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                buffer.writeBoolean(recipe.copyInputTag);
            }
        };
        @Override public MapCodec<GuabaConversionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GuabaConversionRecipe> streamCodec() { return STREAM; }
    }
}

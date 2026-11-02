package io.github.vitech.pinkkicore.neoforge.charging;

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
public final class GuabaChargingRecipe implements Recipe<MachineInput> {
    public static final int DURATION = 20;
    private final Ingredient input;
    private final ItemStack result;
    private final int inputCount;
    public GuabaChargingRecipe(Ingredient input, int inputCount, ItemStack result) {
        if (input.isEmpty() || inputCount != 1 || result.isEmpty() || result.getCount() != 1)
            throw new IllegalArgumentException("Charging requires one input and output: ");
        this.input=input; this.inputCount=inputCount; this.result=result.copy();
    }
    @Override public boolean matches(MachineInput container, Level level) {
        return input.test(container.getItem(0)) && container.getItem(0).getCount() >= inputCount;
    }
    public int inputCount() { return inputCount; }
    @Override public ItemStack assemble(MachineInput container, net.minecraft.core.HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) { return result.copy(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(ChargingContent.ITEM.get()); }
    @Override public RecipeSerializer<?> getSerializer() { return ChargingContent.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ChargingContent.RECIPE.get(); }


    public static final class Serializer implements RecipeSerializer<GuabaChargingRecipe> {
        private static final MapCodec<GuabaChargingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
            Codec.intRange(1, 1).optionalFieldOf("input_count", 1).forGetter(r -> r.inputCount),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(instance, GuabaChargingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, GuabaChargingRecipe> STREAM = new StreamCodec<>() {
            @Override public GuabaChargingRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new GuabaChargingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt(),  ItemStack.STREAM_CODEC.decode(buffer));
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, GuabaChargingRecipe recipe) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
                buffer.writeVarInt(recipe.inputCount);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            }
        };
        @Override public MapCodec<GuabaChargingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GuabaChargingRecipe> streamCodec() { return STREAM; }
    }
}

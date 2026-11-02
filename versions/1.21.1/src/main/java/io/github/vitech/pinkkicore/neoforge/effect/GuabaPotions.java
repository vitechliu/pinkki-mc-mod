package io.github.vitech.pinkkicore.neoforge.effect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.vitech.pinkkicore.core.ModInfo;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class GuabaPotions {
    // Bundled definition also imported by the Wiki; not a reloadable datapack recipe type.
    static final JsonObject SPEC = loadSpec();
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ModInfo.ID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, ModInfo.ID);
    public static final DeferredHolder<MobEffect, MobEffect> GUABA = EFFECTS.register("guaba", GuabaEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> THUNDER_GUABA = EFFECTS.register("thunder_guaba", ThunderGuabaEffect::new);

    private GuabaPotions() {}

    private static JsonObject loadSpec() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(GuabaPotions.class.getResourceAsStream(
                "/data/pinkkicore/guaba_potions.json")), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load bundled Guaba potion definitions", exception);
        }
    }

    public static void register(IEventBus bus) {
        registerPotionDefinitions(SPEC, GUABA);
        registerPotionDefinitions(SPEC.getAsJsonObject("thunder"), THUNDER_GUABA);
        EFFECTS.register(bus);
        POTIONS.register(bus);
    }

    private static void registerPotionDefinitions(JsonObject spec, DeferredHolder<MobEffect, MobEffect> effect) {
        for (var entry : spec.getAsJsonArray("potions")) {
            var definition = entry.getAsJsonObject();
            String id = definition.get("id").getAsString();
            POTIONS.register(id, () -> new Potion(id, new MobEffectInstance(effect,
                    definition.get("duration").getAsInt(), definition.get("amplifier").getAsInt())));
        }
    }

    public static void registerBrewing(RegisterBrewingRecipesEvent event) {
        registerBrewingDefinitions(SPEC, event.getBuilder());
        registerBrewingDefinitions(SPEC.getAsJsonObject("thunder"), event.getBuilder());
    }

    private static void registerBrewingDefinitions(JsonObject spec, net.minecraft.world.item.alchemy.PotionBrewing.Builder builder) {
        for (var entry : spec.getAsJsonArray("brewing")) {
            var rule = entry.getAsJsonObject();
            var input = BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(rule.get("input").getAsString())).orElseThrow();
            var output = BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(rule.get("output").getAsString())).orElseThrow();
            Item reagent = Objects.requireNonNull(BuiltInRegistries.ITEM.get(ResourceLocation.parse(rule.get("ingredient").getAsString())));
            // JEI can enumerate Forge BrewingRecipe inputs/outputs, but not an opaque IBrewingRecipe.
            // Separate bottle types ensure the displayed output always retains the input bottle type.
            for (Item bottle : new Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
                builder.addRecipe(new BrewingRecipe(
                        DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS, new PotionContents(input), bottle),
                        Ingredient.of(reagent), PotionContents.createItemStack(bottle, output)) {
                    @Override public boolean isInput(ItemStack stack) {
                        // Match the potion while retaining support for renamed/custom-tagged bottles.
                        return stack.is(bottle) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().filter(input::equals).isPresent();
                    }
                    @Override public ItemStack getOutput(ItemStack stack, ItemStack ingredient) {
                        if (!isInput(stack) || !isIngredient(ingredient)) return ItemStack.EMPTY;
                        ItemStack result = stack.copy();
                        result.setCount(1);
                        var previous = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                        result.set(DataComponents.POTION_CONTENTS, new PotionContents(java.util.Optional.of(output), previous.customColor(), previous.customEffects()));
                        return result;
                    }
                });
            }
        }
    }
}

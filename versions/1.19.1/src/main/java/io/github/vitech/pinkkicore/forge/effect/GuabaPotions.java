package io.github.vitech.pinkkicore.forge.effect;

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
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GuabaPotions {
    // Bundled definition also imported by the Wiki; not a reloadable datapack recipe type.
    static final JsonObject SPEC = loadSpec();
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ModInfo.ID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, ModInfo.ID);
    public static final RegistryObject<MobEffect> GUABA = EFFECTS.register("guaba", GuabaEffect::new);

    public static final RegistryObject<MobEffect> THUNDER_GUABA = EFFECTS.register("thunder_guaba", ThunderGuabaEffect::new);

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

    private static void registerPotionDefinitions(JsonObject spec, RegistryObject<MobEffect> effect) {
        for (var entry : spec.getAsJsonArray("potions")) {
            var definition = entry.getAsJsonObject();
            String id = definition.get("id").getAsString();
            POTIONS.register(id, () -> new Potion(id, new MobEffectInstance(effect.get(),
                    definition.get("duration").getAsInt(), definition.get("amplifier").getAsInt())));
        }
    }

    public static void registerBrewing() {
        registerBrewingDefinitions(SPEC);
        registerBrewingDefinitions(SPEC.getAsJsonObject("thunder"));
    }

    private static void registerBrewingDefinitions(JsonObject spec) {
        for (var entry : spec.getAsJsonArray("brewing")) {
            var rule = entry.getAsJsonObject();
            Potion input = Objects.requireNonNull(ForgeRegistries.POTIONS.getValue(new ResourceLocation(rule.get("input").getAsString())));
            Potion output = Objects.requireNonNull(ForgeRegistries.POTIONS.getValue(new ResourceLocation(rule.get("output").getAsString())));
            Item reagent = Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(rule.get("ingredient").getAsString())));
            // JEI can enumerate Forge BrewingRecipe inputs/outputs, but not an opaque IBrewingRecipe.
            // Separate bottle types ensure the displayed output always retains the input bottle type.
            for (Item bottle : new Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
                BrewingRecipeRegistry.addRecipe(new BrewingRecipe(
                        StrictNBTIngredient.of(PotionUtils.setPotion(new ItemStack(bottle), input)),
                        Ingredient.of(reagent), PotionUtils.setPotion(new ItemStack(bottle), output)) {
                    @Override public boolean isInput(ItemStack stack) {
                        // Match the potion while retaining support for renamed/custom-tagged bottles.
                        return stack.is(bottle) && PotionUtils.getPotion(stack) == input;
                    }
                    @Override public ItemStack getOutput(ItemStack stack, ItemStack ingredient) {
                        if (!isInput(stack) || !isIngredient(ingredient)) return ItemStack.EMPTY;
                        ItemStack result = stack.copy();
                        result.setCount(1);
                        return PotionUtils.setPotion(result, output);
                    }
                });
            }
        }
    }
}

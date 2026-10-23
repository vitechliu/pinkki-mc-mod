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
import net.minecraftforge.common.brewing.IBrewingRecipe;
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
        for (var entry : SPEC.getAsJsonArray("potions")) {
            var definition = entry.getAsJsonObject();
            String id = definition.get("id").getAsString();
            POTIONS.register(id, () -> new Potion(id, new MobEffectInstance(GUABA.get(),
                    definition.get("duration").getAsInt(), definition.get("amplifier").getAsInt())));
        }
        EFFECTS.register(bus);
        POTIONS.register(bus);
    }

    public static void registerBrewing() {
        for (var entry : SPEC.getAsJsonArray("brewing")) {
            var rule = entry.getAsJsonObject();
            Potion input = Objects.requireNonNull(ForgeRegistries.POTIONS.getValue(new ResourceLocation(rule.get("input").getAsString())));
            Potion output = Objects.requireNonNull(ForgeRegistries.POTIONS.getValue(new ResourceLocation(rule.get("output").getAsString())));
            Item reagent = Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(rule.get("ingredient").getAsString())));
            BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
                @Override public boolean isInput(ItemStack stack) {
                    return (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
                            && PotionUtils.getPotion(stack) == input;
                }
                @Override public boolean isIngredient(ItemStack stack) { return stack.is(reagent); }
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

package pinkkicore.test;

import java.util.Arrays;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Test-only mod, compiled against released SRG jars. Never shipped in PinkkiCore. */
@Mod("pinkkicore_compat_probe")
public final class GuabaCompatProbe {
    public GuabaCompatProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private void started(ServerStartedEvent event) {
        try {
            ItemStack guaba = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("pinkkicore:guaba")));
            if (ModList.get().isLoaded("silentgear")) Silent.check(guaba);
            if (ModList.get().isLoaded("tconstruct")) Tinkers.check(guaba);
            if (ModList.get().isLoaded("tetra")) Tetra.check();
            System.out.println("GUABA_COMPAT_PROBE PASS");
        } catch (Throwable failure) {
            System.out.println("GUABA_COMPAT_PROBE FAIL " + failure);
            failure.printStackTrace();
        }
    }

    private static final class Silent {
        static void check(ItemStack guaba) {
            var material = net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation("pinkkicore:guaba"));
            require(material != null, "Silent Gear material missing");
            require(material.getIngredient().test(guaba), "Silent Gear ingredient mismatch");
            System.out.println("GUABA silentgear registered + ingredient matched");
        }
    }

    private static final class Tinkers {
        static void check(ItemStack guaba) {
            var id = new slimeknights.tconstruct.library.materials.definition.MaterialId("pinkkicore:guaba");
            var registry = slimeknights.tconstruct.library.materials.MaterialRegistry.getInstance();
            require(registry.getMaterial(id).getIdentifier().equals(id), "Tinkers material missing");
            require(registry.getMaterial(id).isCraftable(), "Tinkers material not craftable");
            for (String part : new String[]{"head", "handle", "binding"}) {
                var stat = new slimeknights.tconstruct.library.materials.stats.MaterialStatsId("tconstruct:" + part);
                require(registry.getMaterialStats(id, stat).isPresent(), "Tinkers missing stat " + part);
                require(registry.getTraits(id, stat).isEmpty(), "Tinkers unexpected trait " + part);
            }
            var recipe = slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache.findRecipe(guaba);
            require(recipe != null && recipe.getMaterial().getId().equals(id), "Tinkers item mapping missing");
            require(recipe.getNeeded() == 1 && recipe.getValue() == 1, "Tinkers item conversion differs");
            System.out.println("GUABA tconstruct registered + 3 stats + empty traits + 1:1 item mapping");
        }
    }

    private static final class Tetra {
        static void check() {
            var data = se.mickelus.tetra.data.DataManager.instance;
            var material = data.materialData.getData(new ResourceLocation("tetra:wood/pinkkicore_guaba"));
            require(material != null && "pinkkicore_guaba".equals(material.key), "Tetra material missing");
            require(material.durability == 60 && material.magicCapacity == 0, "Tetra placeholder stats differ");
            require(material.material.getApplicableItemStacks().length == 1, "Tetra ingredient missing");
            for (String module : new String[]{"basic_blade", "basic_hilt"}) {
                var itemModule = se.mickelus.tetra.module.ModuleRegistry.instance.getModule(new ResourceLocation("tetra:sword/" + module));
                require(itemModule != null, "Tetra module missing " + module);
                require(Arrays.stream(itemModule.getVariantData()).anyMatch(v -> (module + "/pinkkicore_guaba").equals(v.key)), "Tetra generated variant missing " + module);
                require(Arrays.stream(itemModule.getVariantData()).anyMatch(v -> (module + "/oak").equals(v.key)), "Tetra original oak variant lost " + module);
            }
            System.out.println("GUABA tetra registered + ingredient + generated blade/hilt + original oak retained");
        }
    }
}

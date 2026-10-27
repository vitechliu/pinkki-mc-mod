package pinkkicore.test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.util.FakePlayer;
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
            Combat.level = event.getServer().m_129783_(); // MinecraftServer.overworld (SRG)
            if (ModList.get().isLoaded("silentgear")) Silent.check(guaba);
            if (ModList.get().isLoaded("tconstruct")) Tinkers.check(guaba);
            if (ModList.get().isLoaded("tetra")) Tetra.check();
            if (ModList.get().isLoaded("silentgear")) Silent.thunder();
            if (ModList.get().isLoaded("tconstruct")) Tinkers.thunder();
            if (ModList.get().isLoaded("tetra")) Tetra.thunder();
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
            Combat.verify("silentgear/main", weapon(true, false), weapon(false, false));
            Combat.verify("silentgear/rod", weapon(false, true), weapon(false, false));
            Combat.verify("silentgear/both", weapon(true, true), weapon(false, false));
        }
        static void thunder() {
            var material = net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation("pinkkicore:thunder_guaba"));
            require(material != null && material.getIngredient().test(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("pinkkicore:thunder_guaba")))), "Silent thunder ingredient");
            var fast = weapon("pinkkicore:thunder_guaba", true, false);
            var diamond = weapon("silentgear:diamond", true, false);
            require(net.silentchaos512.gear.util.GearData.getStat(fast, net.silentchaos512.gear.api.stats.ItemStats.ATTACK_SPEED) > net.silentchaos512.gear.util.GearData.getStat(diamond, net.silentchaos512.gear.api.stats.ItemStats.ATTACK_SPEED), "Silent thunder attack speed not above diamond");
            require(net.silentchaos512.gear.util.GearData.getStat(fast, net.silentchaos512.gear.api.stats.ItemStats.HARVEST_SPEED) > net.silentchaos512.gear.util.GearData.getStat(diamond, net.silentchaos512.gear.api.stats.ItemStats.HARVEST_SPEED), "Silent thunder harvest speed not above diamond");
            require(net.silentchaos512.gear.util.GearData.getStat(fast, net.silentchaos512.gear.api.stats.ItemStats.DURABILITY) < net.silentchaos512.gear.util.GearData.getStat(diamond, net.silentchaos512.gear.api.stats.ItemStats.DURABILITY), "Silent thunder durability not below diamond");
            for (boolean[] parts : new boolean[][]{{true,false},{false,true},{true,true}}) {
                var sword = weapon("pinkkicore:thunder_guaba", parts[0],parts[1]);
                Combat.verifyThunder("silentgear/thunder/" + Arrays.toString(parts), sword, weapon(false,false));
                Combat.lightweight("silentgear", sword);
            }
            System.out.println("GUABA silentgear thunder faster attack/mining + lower durability than diamond PASS");
        }
        private static ItemStack weapon(boolean headGuaba, boolean rodGuaba) { return weapon("pinkkicore:guaba", headGuaba, rodGuaba); }
        private static ItemStack weapon(String material, boolean headGuaba, boolean rodGuaba) {
            var type = net.silentchaos512.gear.api.item.GearType.SWORD;
            var head = net.silentchaos512.gear.gear.material.MaterialInstance.of(
                net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation(headGuaba ? material : "silentgear:wood")));
            var rod = net.silentchaos512.gear.gear.material.MaterialInstance.of(
                net.silentchaos512.gear.gear.material.MaterialManager.get(new ResourceLocation(rodGuaba ? material : "silentgear:wood")));
            var main = net.silentchaos512.gear.api.part.PartType.MAIN.makeCompoundPart(type, List.of(head)).orElseThrow();
            var handle = net.silentchaos512.gear.api.part.PartType.ROD.makeCompoundPart(type, List.of(rod)).orElseThrow();
            var sword = (net.silentchaos512.gear.api.item.ICoreItem) ForgeRegistries.ITEMS.getValue(new ResourceLocation("silentgear:sword"));
            return sword.construct(List.of(main, handle));
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
                require(registry.getTraits(id, stat).stream().anyMatch(t -> t.getId().toString().equals("pinkkicore:guaba_touch")), "Tinkers Guaba Touch missing " + part);
            }
            var recipe = slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache.findRecipe(guaba);
            require(recipe != null && recipe.getMaterial().getId().equals(id), "Tinkers item mapping missing");
            require(recipe.getNeeded() == 1 && recipe.getValue() == 1, "Tinkers item conversion differs");
            System.out.println("GUABA tconstruct registered + 3 stats + Guaba Touch + 1:1 item mapping");
            Combat.verify("tconstruct/head", weapon(true, false, false), weapon(false, false, false));
            Combat.verify("tconstruct/handle", weapon(false, true, false), weapon(false, false, false));
            Combat.verify("tconstruct/all", weapon(true, true, true), weapon(false, false, false));
            Combat.verify("tconstruct/binding", weapon("pickaxe", false, false, true), weapon("pickaxe", false, false, false));
        }
        private static ItemStack weapon(boolean... guaba) { return weapon("sword", guaba); }
        static void thunder() {
            var id = new slimeknights.tconstruct.library.materials.definition.MaterialId("pinkkicore:thunder_guaba");
            var registry = slimeknights.tconstruct.library.materials.MaterialRegistry.getInstance();
            require(registry.getMaterial(id).getIdentifier().equals(id), "Tinkers thunder missing");
            var ingredient = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("pinkkicore:thunder_guaba")));
            var mapping = slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache.findRecipe(ingredient);
            require(mapping != null && mapping.getMaterial().getId().equals(id) && mapping.getNeeded()==1 && mapping.getValue()==1, "Tinkers thunder 1:1 mapping");
            var fast = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(weaponMaterial("pinkkicore:thunder_guaba", "pickaxe", true,false,false)).getStats();
            var baseline = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(weaponMaterial("tconstruct:manyullyn", "pickaxe", true,false,false)).getStats();
            require(fast.get(slimeknights.tconstruct.library.tools.stat.ToolStats.ATTACK_SPEED)>baseline.get(slimeknights.tconstruct.library.tools.stat.ToolStats.ATTACK_SPEED), "Tinkers thunder attack speed");
            require(fast.get(slimeknights.tconstruct.library.tools.stat.ToolStats.MINING_SPEED)>baseline.get(slimeknights.tconstruct.library.tools.stat.ToolStats.MINING_SPEED), "Tinkers thunder mining speed");
            require(fast.get(slimeknights.tconstruct.library.tools.stat.ToolStats.DURABILITY)<baseline.get(slimeknights.tconstruct.library.tools.stat.ToolStats.DURABILITY), "Tinkers thunder durability");
            for (boolean[] parts : new boolean[][]{{true,false,false},{false,true,false},{false,false,true},{true,true,true}}) {
                var sword = weaponMaterial("pinkkicore:thunder_guaba", "pickaxe", parts);
                Combat.verifyThunder("tconstruct/thunder/" + Arrays.toString(parts), sword, weapon("pickaxe",false,false,false));
                Combat.lightweight("tconstruct", sword);
            }
            System.out.println("GUABA tconstruct thunder faster attack/mining + lower durability than manyullyn + mapping PASS");
        }
        private static ItemStack weapon(String item, boolean... guaba) { return weaponMaterial("pinkkicore:guaba", item, guaba); }
        private static ItemStack weaponMaterial(String material, String item, boolean... guaba) {
            var materials = new slimeknights.tconstruct.library.materials.definition.IMaterial[guaba.length];
            for (int i = 0; i < guaba.length; i++) {
                materials[i] = slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(
                    new slimeknights.tconstruct.library.materials.definition.MaterialId(guaba[i] ? material : "tconstruct:wood"));
            }
            return slimeknights.tconstruct.library.tools.helper.ToolBuildHandler.buildItemFromMaterials(
                (slimeknights.tconstruct.library.tools.item.IModifiable) ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct:" + item)),
                slimeknights.tconstruct.library.tools.nbt.MaterialNBT.of(materials));
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
            Combat.verify("tetra/blade", weapon(true, false), weapon(false, false));
            Combat.verify("tetra/hilt", weapon(false, true), weapon(false, false));
            Combat.verify("tetra/both", weapon(true, true), weapon(false, false));
        }
        static void thunder() {
            var data = se.mickelus.tetra.data.DataManager.instance;
            var material = data.materialData.getData(new ResourceLocation("tetra:wood/pinkkicore_thunder_guaba"));
            var diamond = data.materialData.getData(new ResourceLocation("tetra:gem/diamond"));
            var netherite = data.materialData.getData(new ResourceLocation("tetra:metal/netherite"));
            require(material != null && material.primary >= diamond.primary && material.primary <= netherite.primary, "Tetra thunder damage tier");
            require(material.durability < diamond.durability && material.toolEfficiency > netherite.toolEfficiency, "Tetra thunder durability/mining");
            var fast = weapon("pinkkicore_thunder_guaba",true,true);
            var iron = weapon("iron",true,true);
            var item = (se.mickelus.tetra.items.modular.IModularItem)fast.m_41720_();
            var attack = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.attack_speed"));
            require(item.getAttributeValue(fast,attack,4)>item.getAttributeValue(iron,attack,4), "Tetra thunder assembled attack speed");
            for (boolean[] parts : new boolean[][]{{true,false},{false,true},{true,true}}) {
                var sword = weapon("pinkkicore_thunder_guaba",parts[0],parts[1]);
                Combat.verifyThunder("tetra/thunder/" + Arrays.toString(parts),sword,weapon(false,false));
                Combat.lightweight("tetra",sword);
            }
            System.out.println("GUABA tetra thunder diamond-netherite damage + lower durability + faster mining/attack PASS");
        }
        private static ItemStack weapon(boolean bladeGuaba, boolean hiltGuaba) { return weapon("pinkkicore_guaba",bladeGuaba,hiltGuaba); }
        private static ItemStack weapon(String material, boolean bladeGuaba, boolean hiltGuaba) {
            var sword = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("tetra:modular_sword")));
            for (String part : new String[]{"basic_blade", "basic_hilt"}) {
                boolean guaba = part.equals("basic_blade") ? bladeGuaba : hiltGuaba;
                se.mickelus.tetra.module.ModuleRegistry.instance.getModule(new ResourceLocation("tetra:sword/" + part))
                    .addModule(sword, part + "/" + (guaba ? material : "oak"), null);
            }
            ((se.mickelus.tetra.items.modular.IModularItem) sword.m_41720_()).assemble(sword, Combat.level, 0);
            return sword;
        }
    }

    /** SRG calls keep this release-JAR fixture independent of the development mappings. */
    private static final class Combat {
        static ServerLevel level;
        static final class Attacker extends FakePlayer {
            float charge = 1.0f;
            Attacker() { super(level, new GameProfile(UUID.randomUUID(), "GuabaHitProbe")); }
            @Override public float m_36403_(float partialTick) { return charge; } // attack strength
        }
        @SuppressWarnings("unchecked")
        static Cow target() {
            var cow = new Cow((EntityType<? extends Cow>) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft:cow")), level);
            cow.m_21051_(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.max_health"))).m_22100_(1000);
            cow.m_21153_(1000);
            return cow;
        }
        static MobEffect effect() { return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("pinkkicore:guaba")); }
        static void hit(Attacker attacker, Cow target, ItemStack weapon) {
            target.f_19802_ = 0; // invulnerableTime
            attacker.m_8061_(EquipmentSlot.MAINHAND, weapon);
            attacker.m_5706_(target); // actual Player.attack, including each mod's hooks
        }
        static void verifyThunder(String name, ItemStack weapon, ItemStack control) {
            verify(name, weapon, control, ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("pinkkicore:thunder_guaba")));
        }
        static void lightweight(String name, ItemStack weapon) {
            var movement = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.movement_speed"));
            var map = weapon.m_41638_(EquipmentSlot.MAINHAND);
            require(!map.get(movement).isEmpty(), name + " thunder movement modifier missing in mainhand");
            double amount = map.get(movement).stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::m_22218_).sum();
            if (name.equals("tetra")) {
                require(!weapon.m_41638_(EquipmentSlot.OFFHAND).get(movement).isEmpty(), name + " native offhand movement missing");
                require(amount >= 0.05 && amount <= 0.103, name + " movement outside per-module native bounds: " + amount);
            } else {
                require(Math.abs(amount-0.05)<0.00001, name + " lightweight unexpectedly stacked: " + amount);
                require(weapon.m_41638_(EquipmentSlot.OFFHAND).get(movement).isEmpty(),name + " movement leaked to offhand");
            }
        }
        static void verify(String name, ItemStack weapon, ItemStack control) { verify(name,weapon,control,effect()); }
        static void verify(String name, ItemStack weapon, ItemStack control, MobEffect effect) {
            var attacker = new Attacker();
            var plain = target();
            hit(attacker, plain, control);
            require(!plain.m_21023_(effect), name + " non-Guaba control applied effect");
            if (name.startsWith("tconstruct/")) {
                attacker.charge = 0.5f;
                var uncharged = target();
                hit(attacker, uncharged, weapon);
                require(uncharged.m_21023_(effect), name + " native 3.11.2 melee hook did not apply effect");
                attacker.charge = 1.0f;
            }
            var victim = target();
            if (effect != effect()) victim.m_7292_(new MobEffectInstance(effect(), 80, 0));
            hit(attacker, victim, weapon);
            if (effect != effect()) {
                require(victim.m_21023_(effect()), name + " ordinary Guaba effect removed");
                long next = ((net.minecraftforge.common.extensions.IForgeEntity)victim).getPersistentData().m_128454_("pinkkicore:thunder_guaba_next_lightning");
                require(next > level.m_46467_(), name + " lightning clock missing");
                hit(attacker, victim, weapon);
                require(((net.minecraftforge.common.extensions.IForgeEntity)victim).getPersistentData().m_128454_("pinkkicore:thunder_guaba_next_lightning")==next, name + " repeated hit postponed lightning");
            }
            require(victim.m_6084_(), name + " victim died before refresh verification");
            var applied = victim.m_21124_(effect);
            require(applied != null, name + " actual melee hit did not apply Guaba");
            require(applied.m_19557_() == 200 && applied.m_19564_() == 0, name + " expected Guaba I for 200 ticks");
            require(!attacker.m_21023_(effect), name + " effect applied to attacker");
            // Shorten the existing effect, then check a hit refreshes rather than accumulates.
            victim.m_21195_(effect);
            victim.m_7292_(new MobEffectInstance(effect, 40, 0));
            hit(attacker, victim, weapon);
            applied = victim.m_21124_(effect);
            require(applied.m_19557_() == 200 && applied.m_19564_() == 0, name + " refresh/stack mismatch");
            victim.m_21195_(effect);
            victim.m_7292_(new MobEffectInstance(effect, 600, 1));
            hit(attacker, victim, weapon);
            applied = victim.m_21124_(effect);
            require(applied.m_19557_() == 600 && applied.m_19564_() == 1, name + " stronger existing effect overwritten");
            System.out.println("GUABA " + name + " actual melee + control + duration/amplifier + target only + refresh + stronger effect preserved PASS");
        }
    }
}

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

@Mod("pinkkicore_tetra_probe")
public final class TetraProbe {
    public TetraProbe() { MinecraftForge.EVENT_BUS.addListener(this::started); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void started(ServerStartedEvent event) {
        try {
            Combat.level = event.getServer().overworld();
            Tetra.check(); Tetra.thunder();
            System.out.println("TETRA_PROBE PASS");
        } catch (Throwable error) { error.printStackTrace(); System.out.println("TETRA_PROBE FAIL " + error); }
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
            var item = (se.mickelus.tetra.items.modular.IModularItem)fast.getItem();
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
            ((se.mickelus.tetra.items.modular.IModularItem) sword.getItem()).assemble(sword, Combat.level, 0);
            return sword;
        }
    }

    private static final class Combat {
        static ServerLevel level;
        static final class Attacker extends FakePlayer {
            float charge = 1.0f;
            Attacker() { super(Combat.level, new GameProfile(UUID.randomUUID(), "GuabaHitProbe")); }
            @Override public float getAttackStrengthScale(float partialTick) { return charge; } // attack strength
        }
        @SuppressWarnings("unchecked")
        static Cow target() {
            var cow = new Cow((EntityType<? extends Cow>) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft:cow")), level);
            cow.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.max_health"))).setBaseValue(1000);
            cow.setHealth(1000);
            return cow;
        }
        static MobEffect effect() { return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("pinkkicore:guaba")); }
        static void hit(Attacker attacker, Cow target, ItemStack weapon) {
            target.invulnerableTime = 0; // invulnerableTime
            attacker.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            attacker.attack(target); // actual Player.attack, including each mod's hooks
        }
        static void verifyThunder(String name, ItemStack weapon, ItemStack control) {
            verify(name, weapon, control, ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("pinkkicore:thunder_guaba")));
        }
        static void lightweight(String name, ItemStack weapon) {
            var movement = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("minecraft:generic.movement_speed"));
            var map = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND);
            require(!map.get(movement).isEmpty(), name + " thunder movement modifier missing in mainhand");
            double amount = map.get(movement).stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
            if (name.equals("tetra")) {
                require(!weapon.getAttributeModifiers(EquipmentSlot.OFFHAND).get(movement).isEmpty(), name + " native offhand movement missing");
                require(amount >= 0.05 && amount <= 0.103, name + " movement outside per-module native bounds: " + amount);
            } else {
                require(Math.abs(amount-0.05)<0.00001, name + " lightweight unexpectedly stacked: " + amount);
                require(weapon.getAttributeModifiers(EquipmentSlot.OFFHAND).get(movement).isEmpty(),name + " movement leaked to offhand");
            }
        }
        static void verify(String name, ItemStack weapon, ItemStack control) { verify(name,weapon,control,effect()); }
        static void verify(String name, ItemStack weapon, ItemStack control, MobEffect effect) {
            var attacker = new Attacker();
            var plain = target();
            hit(attacker, plain, control);
            require(!plain.hasEffect(effect), name + " non-Guaba control applied effect");
            if (name.startsWith("tconstruct/")) {
                attacker.charge = 0.5f;
                var uncharged = target();
                hit(attacker, uncharged, weapon);
                require(uncharged.hasEffect(effect), name + " native 3.11.2 melee hook did not apply effect");
                attacker.charge = 1.0f;
            }
            var victim = target();
            if (effect != effect()) victim.addEffect(new MobEffectInstance(effect(), 80, 0));
            hit(attacker, victim, weapon);
            if (effect != effect()) {
                require(victim.hasEffect(effect()), name + " ordinary Guaba effect removed");
                long next = ((net.minecraftforge.common.extensions.IForgeEntity)victim).getPersistentData().getLong("pinkkicore:thunder_guaba_next_lightning");
                require(next > level.getGameTime(), name + " lightning clock missing");
                hit(attacker, victim, weapon);
                require(((net.minecraftforge.common.extensions.IForgeEntity)victim).getPersistentData().getLong("pinkkicore:thunder_guaba_next_lightning")==next, name + " repeated hit postponed lightning");
            }
            require(victim.isAlive(), name + " victim died before refresh verification");
            var applied = victim.getEffect(effect);
            require(applied != null, name + " actual melee hit did not apply Guaba");
            require(applied.getDuration() == 200 && applied.getAmplifier() == 0, name + " expected Guaba I for 200 ticks");
            require(!attacker.hasEffect(effect), name + " effect applied to attacker");
            // Shorten the existing effect, then check a hit refreshes rather than accumulates.
            victim.removeEffect(effect);
            victim.addEffect(new MobEffectInstance(effect, 40, 0));
            hit(attacker, victim, weapon);
            applied = victim.getEffect(effect);
            require(applied.getDuration() == 200 && applied.getAmplifier() == 0, name + " refresh/stack mismatch");
            victim.removeEffect(effect);
            victim.addEffect(new MobEffectInstance(effect, 600, 1));
            hit(attacker, victim, weapon);
            applied = victim.getEffect(effect);
            require(applied.getDuration() == 600 && applied.getAmplifier() == 1, name + " stronger existing effect overwritten");
            System.out.println("GUABA " + name + " actual melee + control + duration/amplifier + target only + refresh + stronger effect preserved PASS");
        }
    }
}

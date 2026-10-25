package pinkkicore.test;

import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod("pinkkicore_guaba_effect_probe")
public final class GuabaEffectProbe {
    private ServerLevel level;
    private Cow cow;
    private int ticks;
    private boolean done;
    private double lastSpeed, lastArmor, baseSpeed;
    private long rollTime;
    private static final double[][] BOUNDS = {{.5,1.75,0,8},{.65,2.5,2,16},{.8,3.25,4,24},{1,4,6,32}};
    public GuabaEffectProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static Potion potion(String id) { return ForgeRegistries.POTIONS.getValue(new ResourceLocation("pinkkicore",id)); }
    private static ItemStack bottle(Item item, Potion potion) { return PotionUtils.setPotion(new ItemStack(item),potion); }
    private static ItemStack brew(ItemStack input, Item reagent) { return BrewingRecipeRegistry.getOutput(input,new ItemStack(reagent)); }
    private static void result(ItemStack stack, Item item, Potion potion) {
        check(stack.is(item) && PotionUtils.getPotion(stack)==potion,"unexpected brewing result "+stack);
    }
    private void started(ServerStartedEvent event) {
        try {
            level=event.getServer().overworld(); level.setChunkForced(0,0,true);
            var normal=potion("guaba"); var extended=potion("long_guaba"); var strong=potion("strong_guaba");
            check(normal.getEffects().get(0).getDuration()==3600 && extended.getEffects().get(0).getDuration()==9600
                && strong.getEffects().get(0).getDuration()==1800 && strong.getEffects().get(0).getAmplifier()==1,"potion definitions");
            int visibleRecipes = 0;
            for (var recipe : BrewingRecipeRegistry.getRecipes()) {
                if (!(recipe instanceof BrewingRecipe standard)) continue;
                if (!ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(standard.getOutput())).getNamespace().equals("pinkkicore")) continue;
                var inputs = standard.getInput().getItems();
                var reagents = standard.getIngredient().getItems();
                check(inputs.length == 1 && reagents.length == 1, "JEI recipe has no concrete inputs");
                check(ItemStack.isSameItemSameTags(brew(inputs[0], reagents[0].getItem()), standard.getOutput()), "JEI output differs from actual brewing");
                var named = inputs[0].copy(); named.setHoverName(net.minecraft.network.chat.Component.literal("Probe bottle"));
                var namedOutput = brew(named, reagents[0].getItem());
                check(namedOutput.hasCustomHoverName(), "brewing lost existing bottle NBT");
                check(PotionUtils.getPotion(namedOutput) == PotionUtils.getPotion(standard.getOutput()), "named bottle failed brewing");
                check(!standard.isInput(bottle(inputs[0].getItem(), Potions.WATER)), "potion NBT ignored");
                visibleRecipes++;
            }
            check(visibleRecipes == 9, "JEI standard brewing recipes: expected 9, got " + visibleRecipes);
            System.out.println("GUABA_EFFECT JEI-readable 9 recipes + concrete potion NBT + exact outputs + named bottle preservation PASS");
            for (Item container : new Item[]{Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION}) {
                var basic=brew(bottle(container,Potions.AWKWARD),ModItems.GUABA.get()); result(basic,container,normal);
                result(brew(basic,Items.REDSTONE),container,extended); result(brew(basic,Items.GLOWSTONE_DUST),container,strong);
                for (Potion capped : new Potion[]{extended,strong}) for (Item reagent : new Item[]{Items.REDSTONE,Items.GLOWSTONE_DUST,ModItems.GUABA.get()})
                    check(brew(bottle(container,capped),reagent).isEmpty(),"forbidden extension/upgrade");
                check(brew(bottle(container,Potions.WATER),ModItems.GUABA.get()).isEmpty(),"water accepted as awkward");
            }
            for (Potion type : new Potion[]{normal,extended,strong}) {
                result(brew(bottle(Items.POTION,type),Items.GUNPOWDER),Items.SPLASH_POTION,type);
                result(brew(bottle(Items.SPLASH_POTION,type),Items.DRAGON_BREATH),Items.LINGERING_POTION,type);
            }
            result(brew(bottle(Items.POTION,Potions.AWKWARD),Items.SUGAR),Items.POTION,Potions.SWIFTNESS);
            System.out.println("GUABA_EFFECT potion definitions + 3 container brewing matrix + vanilla conversions + no III/IV upgrade PASS");
            // Actual vanilla stand: three bottles, one reagent, blaze fuel and 400 brewing ticks.
            BlockPos pos=new BlockPos(4,100,4); level.setBlockAndUpdate(pos,Blocks.BREWING_STAND.defaultBlockState());
            var stand=(BrewingStandBlockEntity)level.getBlockEntity(pos);
            for(int i=0;i<3;i++) stand.setItem(i,bottle(Items.POTION,Potions.AWKWARD));
            stand.setItem(3,new ItemStack(ModItems.GUABA.get())); stand.setItem(4,new ItemStack(Items.BLAZE_POWDER));
            for(int i=0;i<401;i++) BrewingStandBlockEntity.serverTick(level,pos,stand.getBlockState(),stand);
            for(int i=0;i<3;i++)result(stand.getItem(i),Items.POTION,normal);
            check(stand.getItem(3).isEmpty() && stand.getItem(4).isEmpty(),"stand did not consume reagent/fuel");
            System.out.println("GUABA_EFFECT actual brewing stand 3 bottles + reagent/fuel consumption PASS");
            cow=EntityType.COW.create(level); cow.setNoAi(true); cow.setInvulnerable(true); cow.setNoGravity(true); cow.setPos(8,100,8);
            level.addFreshEntity(cow); baseSpeed=cow.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            for(int amplifier=0;amplifier<4;amplifier++) {
                cow.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),600,amplifier));
                for(int sample=0;sample<400;sample++) {
                    GuabaPotions.GUABA.get().addAttributeModifiers(cow,cow.getAttributes(),amplifier);
                    bounds(amplifier);
                    check(cow.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().size()==1,"stacking modifiers");
                }
                cow.removeAllEffects(); restored();
            }
            cow.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),600,255)); bounds(3); cow.removeAllEffects();
            // Save/load while active: transient modifiers are recreated and never double-counted.
            cow.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),600,1));
            CompoundTag saved=new CompoundTag(); cow.saveWithoutId(saved);
            Cow loaded=EntityType.COW.create(level); loaded.load(saved);
            GuabaPotions.GUABA.get().applyEffectTick(loaded,1);
            check(loaded.hasEffect(GuabaPotions.GUABA.get()) && loaded.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().size()==1,"load modifiers");
            loaded.removeAllEffects(); check(Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED)-baseSpeed)<1e-9,"load cleanup");
            cow.removeAllEffects(); restored();
            // Preserve unrelated effects and permanent equipment-like modifiers.
            UUID extra=UUID.fromString("7ae3ed63-0e53-49a1-b40c-c4bfce43b83d");
            cow.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(extra,"probe armor",7,AttributeModifier.Operation.ADDITION));
            cow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,600,0));
            double otherSpeed=cow.getAttributeValue(Attributes.MOVEMENT_SPEED);
            cow.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),600,1)); cow.removeEffect(GuabaPotions.GUABA.get());
            check(Math.abs(cow.getAttributeValue(Attributes.ARMOR)-7)<1e-9 && Math.abs(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)-otherSpeed)<1e-9,"other modifiers damaged");
            cow.removeAllEffects(); cow.getAttribute(Attributes.ARMOR).removeModifier(extra); restored();
            System.out.println("GUABA_EFFECT I-IV 1600 rolls + clamp + no stacking + save/load + removal + other modifiers PASS");
            cow.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),100,0));
            remember();
        } catch(Throwable error) { fail(error); }
    }
    private void bounds(int amplifier) {
        double speed=cow.getAttributeValue(Attributes.MOVEMENT_SPEED)/baseSpeed, armor=cow.getAttributeValue(Attributes.ARMOR);
        double[] b=BOUNDS[amplifier];check(speed>=b[0] && speed<=b[1] && armor>=b[2] && armor<=b[3],"bounds "+amplifier+" "+speed+"/"+armor);
    }
    private void restored() {
        check(Math.abs(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)-baseSpeed)<1e-9 && cow.getAttributeValue(Attributes.ARMOR)==0,"base attributes not restored");
    }
    private void remember() {
        lastSpeed=cow.getAttributeValue(Attributes.MOVEMENT_SPEED);lastArmor=cow.getAttributeValue(Attributes.ARMOR);
        rollTime=cow.getPersistentData().getLong("pinkkicore:guaba_next_roll")-20;
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || cow==null || done)return;
        try {
            ticks++;
            if(cow.hasEffect(GuabaPotions.GUABA.get())) {
                long time=cow.getPersistentData().getLong("pinkkicore:guaba_next_roll")-20;
                if(time!=rollTime) {
                    check(time-rollTime==20,"roll interval "+(time-rollTime));
                    check(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)!=lastSpeed && cow.getAttributeValue(Attributes.ARMOR)!=lastArmor,"no reroll");
                    remember();
                } else check(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)==lastSpeed && cow.getAttributeValue(Attributes.ARMOR)==lastArmor,"changed before 20 ticks");
            } else if(ticks>=99) {
                restored(); cow.discard(); done=true;
                System.out.println("GUABA_EFFECT real server ticks: 20-tick cadence + stable between rolls + natural expiry PASS");
                System.out.println("GUABA_EFFECT_PROBE PASS");
            }
            check(ticks<110,"effect never expired");
        }catch(Throwable error){fail(error);}
    }
    private void fail(Throwable error) { done=true;System.out.println("GUABA_EFFECT_PROBE FAIL "+error);error.printStackTrace(); }
}

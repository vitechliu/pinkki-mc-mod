package pinkkicore.test;

import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;

final class ThunderGuabaProbe {
    static boolean passed;
    private static final UUID SPEED=UUID.fromString("ec4f8a15-849c-48cb-b85b-13ca786b08ca");
    private static final UUID ARMOR=UUID.fromString("0cdd520a-ee1f-4907-91dc-a98037b8a014");
    private static final String CLOCK="pinkkicore:thunder_guaba_next_lightning";
    private ServerLevel level;
    private Cow target;
    private long due, lastStrike=-1, stoppedAt=-1;
    private int bolts, hits;
    private boolean done;
    ThunderGuabaProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::joined);
        MinecraftForge.EVENT_BUS.addListener(this::struck);
    }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static Potion potion(String id) { return ForgeRegistries.POTIONS.getValue(new ResourceLocation("pinkkicore",id)); }
    private static ItemStack bottle(Item container,Potion potion) { return PotionUtils.setPotion(new ItemStack(container),potion); }
    private static ItemStack brew(ItemStack input,Item ingredient) { return BrewingRecipeRegistry.getOutput(input,new ItemStack(ingredient)); }
    private void start(ServerStartedEvent event) {
        try {
            level=event.getServer().overworld();
            level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,event.getServer());
            for(Item container:new Item[]{Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION}) {
                ItemStack basic=brew(bottle(container,Potions.AWKWARD),ModItems.THUNDER_GUABA.get());
                check(basic.is(container)&&PotionUtils.getPotion(basic)==potion("thunder_guaba"),"thunder base brewing");
                check(PotionUtils.getPotion(brew(basic,Items.REDSTONE))==potion("long_thunder_guaba"),"thunder extension");
                check(PotionUtils.getPotion(brew(basic,Items.GLOWSTONE_DUST))==potion("strong_thunder_guaba"),"thunder strengthening");
                for(String id:new String[]{"long_thunder_guaba","strong_thunder_guaba"}) {
                    check(brew(bottle(container,potion(id)),Items.GLOWSTONE_DUST).isEmpty(),"thunder over-upgrade");
                    check(brew(bottle(container,potion(id)),Items.REDSTONE).isEmpty(),"thunder stacking duration");
                }
                check(brew(bottle(container,Potions.WATER),ModItems.THUNDER_GUABA.get()).isEmpty(),"thunder from water");
            }
            var effect=GuabaPotions.THUNDER_GUABA.get();
            check(potion("thunder_guaba").getEffects().get(0).getEffect()==effect,"wrong potion effect");
            check(potion("thunder_guaba").getEffects().get(0).getDuration()==3600 && potion("long_thunder_guaba").getEffects().get(0).getDuration()==9600
                && potion("strong_thunder_guaba").getEffects().get(0).getDuration()==1800 && potion("strong_thunder_guaba").getEffects().get(0).getAmplifier()==1,"thunder durations/levels");
            for(String id:new String[]{"thunder_guaba","long_thunder_guaba","strong_thunder_guaba"}) {
                var splash=brew(bottle(Items.POTION,potion(id)),Items.GUNPOWDER);
                check(splash.is(Items.SPLASH_POTION)&&PotionUtils.getPotion(splash)==potion(id),"thunder splash");
                check(brew(splash,Items.DRAGON_BREATH).is(Items.LINGERING_POTION),"thunder lingering");
            }
            Cow sample=EntityType.COW.create(level);sample.getRandom().setSeed(261026);
            double[] speed={.75,.975,1.2,1.425},armor={12,24,36,48};
            for(int amp=0;amp<4;amp++) {
                sample.addEffect(new MobEffectInstance(effect,1000,amp));
                int positive=0,negative=0,positiveArmor=0,negativeArmor=0,stopped=0;
                double maxSpeed=0,maxArmor=0;
                long clock=sample.getPersistentData().getLong(CLOCK);
                for(int i=0;i<400;i++) {
                    effect.addAttributeModifiers(sample,sample.getAttributes(),amp);
                    double s=sample.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(SPEED).getAmount();
                    double a=sample.getAttribute(Attributes.ARMOR).getModifier(ARMOR).getAmount();
                    check(Math.abs(s)<=speed[amp]+1e-9&&Math.abs(a)<=armor[amp]+1e-9,"thunder bounds");
                    if(s>0)positive++; else negative++;
                    if(a>0)positiveArmor++; else negativeArmor++;
                    maxSpeed=Math.max(maxSpeed,Math.abs(s));maxArmor=Math.max(maxArmor,Math.abs(a));
                    if(s < -1) { stopped++;check(sample.getAttributeValue(Attributes.MOVEMENT_SPEED)==0,"negative movement not clamped"); }
                    check(clock==sample.getPersistentData().getLong(CLOCK),"attribute roll postponed lightning");
                }
                check(positive>100&&negative>100&&positiveArmor>100&&negativeArmor>100,"one-sided thunder rolls");
                check(maxSpeed>speed[amp]*.9&&maxArmor>armor[amp]*.9,"thunder amplitude not expanded to 150%");
                if(amp>=2)check(stopped>0,"missing strong slowdown");
                sample.removeAllEffects();check(!sample.getPersistentData().contains(CLOCK),"lightning clock not cleared");
            }
            sample.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),1000,0));
            double base=sample.getAttributeValue(Attributes.MOVEMENT_SPEED);
            sample.addEffect(new MobEffectInstance(effect,1000,0)); sample.removeEffect(effect);
            check(Math.abs(sample.getAttributeValue(Attributes.MOVEMENT_SPEED)-base)<1e-9,"removed ordinary Guaba modifier");
            sample.removeAllEffects(); sample.addEffect(new MobEffectInstance(effect,1000,0));
            long clock=sample.getPersistentData().getLong(CLOCK);
            sample.addEffect(new MobEffectInstance(effect,2000,3));
            check(clock==sample.getPersistentData().getLong(CLOCK),"upgrade postponed lightning");
            sample.addEffect(new MobEffectInstance(effect,3000,3));
            check(clock==sample.getPersistentData().getLong(CLOCK),"refresh postponed lightning");
            CompoundTag saved=new CompoundTag();sample.saveWithoutId(saved);
            Cow loaded=EntityType.COW.create(level);loaded.load(saved);effect.applyEffectTick(loaded,3);
            check(loaded.getPersistentData().getLong(CLOCK)==clock,"load postponed lightning");loaded.removeAllEffects();
            sample.removeAllEffects();
            System.out.println("GUABA_EFFECT thunder 9 brewing paths + I-IV 1600 rolls at 150% + coexistence + upgrade/refresh/load clocks PASS");
            target=EntityType.COW.create(level);target.setNoAi(true);target.setNoGravity(true);target.setPos(12,100,12);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);level.addFreshEntity(target);
            target.addEffect(new MobEffectInstance(effect,2000,0));due=target.getPersistentData().getLong(CLOCK);
            interval(due-level.getGameTime());
        }catch(Throwable error){fail(error);}
    }
    private static void interval(long ticks) { check(ticks>=100&&ticks<=200,"lightning interval "+ticks); }
    private void joined(EntityJoinLevelEvent event) {
        if(target==null||done||!(event.getEntity() instanceof LightningBolt bolt)||bolt.distanceToSqr(target)>1)return;
        try {
            check(stoppedAt<0,"lightning generated after effect removal");
            check(level.getGameTime()==due,"strike outside scheduled tick");
            if(lastStrike>=0)interval(level.getGameTime()-lastStrike);
            lastStrike=level.getGameTime();bolts++;
        }catch(Throwable error){fail(error);}
    }
    private void struck(EntityStruckByLightningEvent event) { if(event.getEntity()==target)hits++; }
    private void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||target==null||done)return;
        try {
            if(stoppedAt>=0) {
                if(level.getGameTime()-stoppedAt>25) {
                    check(!target.hasEffect(GuabaPotions.THUNDER_GUABA.get()) && !target.getPersistentData().contains(CLOCK), "natural expiry retained lightning clock");
                }
                if(level.getGameTime()-stoppedAt>220) {
                    check(hits>0 && target.getHealth()<1000,"lightning did not actually hit/damage target");
                    target.discard();done=true;passed=true;
                    System.out.println("GUABA_EFFECT thunder real I/IV lightning at 100-200 ticks + actual damage + removal stops strikes PASS");
                }
                return;
            }
            long next=target.getPersistentData().getLong(CLOCK);
            if(next!=due) {
                check(level.getGameTime()==due && bolts>0,"unscheduled lightning clock change");interval(next-level.getGameTime());due=next;
                if(bolts==1) {
                    target.addEffect(new MobEffectInstance(GuabaPotions.THUNDER_GUABA.get(),2000,3));
                    check(target.getPersistentData().getLong(CLOCK)==due,"I-IV upgrade rescheduled strike");
                } else if(bolts==2) {
                    target.removeEffect(GuabaPotions.THUNDER_GUABA.get());
                    check(!target.getPersistentData().contains(CLOCK),"removed clock retained");
                    target.addEffect(new MobEffectInstance(GuabaPotions.THUNDER_GUABA.get(),20,0));
                    stoppedAt=level.getGameTime();
                }
            }
        }catch(Throwable error){fail(error);}
    }
    private void fail(Throwable error) { done=true;System.out.println("GUABA_EFFECT_PROBE FAIL "+error);error.printStackTrace(); }
}

package pinkkicore.test;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.neoforge.charging.*;
import io.github.vitech.pinkkicore.neoforge.effect.*;
import io.github.vitech.pinkkicore.neoforge.entity.*;
import io.github.vitech.pinkkicore.neoforge.guide.GuideBook;
import io.github.vitech.pinkkicore.neoforge.item.*;
import io.github.vitech.pinkkicore.neoforge.ore.PinkOreContent;
import io.github.vitech.pinkkicore.neoforge.workbench.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod("pinkkicore_port_probe")
public final class PortProbe {
    public PortProbe() { NeoForge.EVENT_BUS.addListener(this::started); }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static ResourceLocation id(String id) { return ResourceLocation.parse(id.contains(":") ? id : "pinkkicore:" + id); }
    static void pass(String message) { System.out.println("PORT_CHECK PASS " + message); }
    private void started(ServerStartedEvent event) {
        try {
            var level = event.getServer().overworld();
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PortProbe"));
            player.setPos(8, 100, 8);
            registrationAndGuide(level, player);
            machines(level);
            brewing(level);
            effects(level);
            combat(level, player);
            entitiesAndLoot(level);
            WorldChecks.run(level);
            if (ModList.get().isLoaded("curios")) CuriosChecks.run(player);
            if (ModList.get().isLoaded("silentgear")) GearChecks.run(level);
            System.out.println("PORT_PROBE PASS");
        } catch (Throwable error) { error.printStackTrace(); System.out.println("PORT_PROBE FAIL " + error); }
    }
    private static void registrationAndGuide(ServerLevel level, FakePlayer player) throws Exception {
        for (String item : List.of("guaba", "thunder_guaba", "guaba_sword", "guaba_bow", "guide_book", "guaba_workbench", "guaba_charging_station", "pink_ore", "deepslate_pink_ore", "pink_ingot", "pink_nugget", "raw_pink", "pink_block", "raw_pink_block", "pinkki_spawn_egg", "bingwa_spawn_egg", "guaba_yingrong_spawn_egg", "charged_creeper_spawn_egg", "guaba_hood", "pink_blessing")) {
            check(BuiltInRegistries.ITEM.containsKey(id(item)), "missing item " + item);
        }
        for (String recipe : List.of("guaba", "guide_book", "guaba_workbench", "guaba_charging_station", "guaba_hood", "pink_blessing", "pink_ingot_from_smelting_raw_pink"))
            check(level.getRecipeManager().byKey(id(recipe)).isPresent(), "missing recipe " + recipe);
        var crafting = (CraftingRecipe)level.getRecipeManager().byKey(id("guaba")).orElseThrow().value();
        var grid = CraftingInput.of(2, 2, List.of(new ItemStack(Items.MELON_SLICE), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.MUD)));
        check(crafting.matches(grid, level) && crafting.assemble(grid, level.registryAccess()).is(ModItems.GUABA.get()), "2x2 Guaba recipe");
        check(!crafting.matches(CraftingInput.of(2, 2, List.of(new ItemStack(Items.MELON_SLICE), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.DIRT))), level), "invalid Guaba ingredient");
        var bookStack = new ItemStack(ModItems.GUIDE_BOOK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bookStack);
        check(bookStack.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "server guide use");
        try (var reader = new InputStreamReader(Objects.requireNonNull(GuideBook.class.getResourceAsStream("/assets/pinkkicore/guide/zh_cn.json")), StandardCharsets.UTF_8)) {
            var book = GuideBook.read(reader);
            check(book.entries().size() == 7, "guide chapters");
            check(book.entries().stream().anyMatch(e -> e.matches("六面")), "Chinese search");
            for (var entry : book.entries()) for (var block : entry.blocks()) if (block.type().equals("recipe")) {
                if (level.getRecipeManager().byKey(id(block.id())).isPresent()) continue;
                check(block.id().contains("irons_spellbooks"), "missing guide recipe " + block.id());
            }
        }
        check(level.getBlockState(new BlockPos(0, 100, 0)) != null, "world");
        check(new ItemStack(PinkOreContent.ORE_ITEM.get()).is(net.minecraft.tags.TagKey.create(Registries.ITEM, id("c:ores/pink"))), "c ore tag");
        pass("20 item IDs, crafting/invalid input, offline guide/server isolation, recipe links and c tags");
    }
    private static void machines(ServerLevel level) {
        var pos = new BlockPos(1, 100, 1);
        level.setBlockAndUpdate(pos, WorkbenchContent.BLOCK.get().defaultBlockState());
        var machine = (GuabaWorkbenchEntity)level.getBlockEntity(pos);
        machine.inventory.setStackInSlot(0, new ItemStack(ModItems.GUABA.get(), 2));
        machine.inventory.setStackInSlot(1, new ItemStack(Items.DIRT, 2));
        for (int i = 0; i < 19; i++) GuabaWorkbenchEntity.serverTick(level, pos, machine.getBlockState(), machine);
        check(machine.inventory.getStackInSlot(2).isEmpty() && machine.progress() == 19, "workbench early output");
        var saved = machine.saveWithFullMetadata(level.registryAccess());
        var loaded = (GuabaWorkbenchEntity)BlockEntity.loadStatic(pos, machine.getBlockState(), saved, level.registryAccess());
        check(loaded != null && loaded.progress() == 19 && loaded.inventory.getStackInSlot(1).getCount() == 2, "machine NBT roundtrip");
        GuabaWorkbenchEntity.serverTick(level, pos, machine.getBlockState(), machine);
        check(machine.inventory.getStackInSlot(2).is(Items.MUD), "workbench result");
        check(machine.inventory.getStackInSlot(0).getCount() == 1, "dirt conversion cost");
        machine.handleButton(0); // requires signal
        for (int i = 0; i < 25; i++) GuabaWorkbenchEntity.serverTick(level, pos, machine.getBlockState(), machine);
        check(machine.progress() == 0, "redstone pause");
        machine.handleButton(0); machine.handleButton(0); machine.handleButton(0);
        var top = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        var bottom = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        check(top != null && bottom != null, "capabilities not registered");
        check(top.insertItem(0, new ItemStack(Items.STICK), true).getCount() == 1, "invalid catalyst inserted");
        check(top.extractItem(2, 1, true).isEmpty() && bottom.extractItem(2, 1, true).is(Items.MUD), "sided extraction");
        machine.inventory.setStackInSlot(2, ItemStack.EMPTY);
        var bow = new ItemStack(Items.BOW); bow.setDamageValue(123); bow.set(DataComponents.CUSTOM_NAME, Component.literal("Named bow"));
        bow.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER), 3);
        machine.inventory.setStackInSlot(1, bow);
        for (int i = 0; i < 20; i++) GuabaWorkbenchEntity.serverTick(level, pos, machine.getBlockState(), machine);
        var output = machine.inventory.getStackInSlot(2);
        check(output.is(ModItems.GUABA_BOW.get()) && output.getDamageValue() == 123 && output.getHoverName().getString().equals("Named bow") && output.isEnchanted(), "conversion components lost");
        machine.inventory.setStackInSlot(0, new ItemStack(ModItems.GUABA.get()));
        machine.inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND_SWORD)); machine.inventory.setStackInSlot(2, ItemStack.EMPTY);
        for (int i=0;i<20;i++) GuabaWorkbenchEntity.serverTick(level,pos,machine.getBlockState(),machine);
        check(machine.inventory.getStackInSlot(2).is(ModItems.GUABA_SWORD.get()) && machine.inventory.getStackInSlot(0).isEmpty(), "sword conversion cost");
        var recipe = level.getRecipeManager().getAllRecipesFor(WorkbenchContent.RECIPE.get()).getFirst().value();
        var buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        try {
            new GuabaConversionRecipe.Serializer().streamCodec().encode(buf, recipe);
            var decoded = new GuabaConversionRecipe.Serializer().streamCodec().decode(buf);
            check(decoded.guabaCost() == recipe.guabaCost() && ItemStack.isSameItemSameComponents(decoded.getResultItem(level.registryAccess()), recipe.getResultItem(level.registryAccess())), "conversion network codec");
        } finally { buf.release(); }
        var cpos = pos.offset(2, 0, 0);
        level.setBlockAndUpdate(cpos, ChargingContent.BLOCK.get().defaultBlockState());
        var charger = (GuabaChargingEntity)level.getBlockEntity(cpos);
        for (var holder : level.getRecipeManager().getAllRecipesFor(ChargingContent.RECIPE.get())) {
            var charging = holder.value();
            charger.inventory.setStackInSlot(0, charging.getIngredients().getFirst().getItems()[0].copy());
            charger.inventory.setStackInSlot(1, ItemStack.EMPTY);
            for (int i=0; i<19; i++) GuabaChargingEntity.serverTick(level,cpos,charger.getBlockState(),charger);
            check(charger.inventory.getStackInSlot(1).isEmpty(), "charging early output");
            GuabaChargingEntity.serverTick(level,cpos,charger.getBlockState(),charger);
            check(ItemStack.isSameItemSameComponents(charger.inventory.getStackInSlot(1),charging.getResultItem(level.registryAccess())) && charger.inventory.getStackInSlot(0).isEmpty(), "charging recipe " + holder.id());
        }
        boolean irons = ModList.get().isLoaded("irons_spellbooks");
        check(level.getRecipeManager().byKey(id("guaba_charging/compat/irons_spellbooks/blank_rune_to_lightning_rune")).isPresent() == irons, "Iron conditional recipe");
        pass("20-tick machines, catalyst, redstone, sided capabilities, NBT, component preservation, network codec, all charging recipes");
    }
    private static void brewing(ServerLevel level) throws Exception {
        try (var reader = new InputStreamReader(Objects.requireNonNull(GuabaPotions.class.getResourceAsStream("/data/pinkkicore/guaba_potions.json")), StandardCharsets.UTF_8)) {
            var spec = JsonParser.parseReader(reader).getAsJsonObject();
            int count = 0;
            for (var family : List.of(spec, spec.getAsJsonObject("thunder"))) for (var element : family.getAsJsonArray("brewing")) {
                var rule = element.getAsJsonObject();
                var input = BuiltInRegistries.POTION.getHolder(id(rule.get("input").getAsString())).orElseThrow();
                var expected = BuiltInRegistries.POTION.getHolder(id(rule.get("output").getAsString())).orElseThrow();
                var reagent = new ItemStack(BuiltInRegistries.ITEM.get(id(rule.get("ingredient").getAsString())));
                for (var bottle : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
                    var stack = PotionContents.createItemStack(bottle, input); stack.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
                    var output = level.potionBrewing().mix(reagent, stack);
                    check(output.is(bottle) && output.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().orElseThrow().equals(expected), "brewing output " + rule);
                    check(output.getHoverName().getString().equals("Named"), "brewing name lost"); count++;
                }
            }
            pass(count + " actual brewing recipes / bottle type / custom name");
        }
    }
    private static void effects(ServerLevel level) {
        var mob = EntityType.COW.create(level); mob.setNoAi(true); mob.setPos(10,100,10); level.addFreshEntity(mob);
        for (var effect : List.of(GuabaPotions.GUABA, GuabaPotions.THUNDER_GUABA)) {
            for (int amplifier = 0; amplifier < 4; amplifier++) {
                mob.addEffect(new MobEffectInstance(effect, 400, amplifier));
                for (int i=0; i<30; i++) {
                    mob.getPersistentData().putLong(effect == GuabaPotions.GUABA ? "pinkkicore:guaba_next_roll" : "pinkkicore:thunder_guaba_next_roll", 0);
                    effect.get().applyEffectTick(mob, amplifier);
                    check(Double.isFinite(mob.getAttributeValue(Attributes.MOVEMENT_SPEED)) && Double.isFinite(mob.getAttributeValue(Attributes.ARMOR)), "effect attribute");
                }
                mob.removeEffect(effect);
                check(mob.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().isEmpty() && mob.getAttribute(Attributes.ARMOR).getModifiers().isEmpty(), "effect modifiers leaked");
            }
        }
        mob.addEffect(new MobEffectInstance(GuabaPotions.GUABA, 100, 0));
        mob.addEffect(new MobEffectInstance(GuabaPotions.THUNDER_GUABA, 100, 0));
        check(mob.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().size() == 2, "effect coexistence");
        mob.removeAllEffects();
        var dropped = new ItemEntity(level, 12,100,12,new ItemStack(ModItems.GUABA.get(), 17));
        dropped.getItem().set(DataComponents.CUSTOM_NAME, Component.literal("Dropped"));
        var lightning = EntityType.LIGHTNING_BOLT.create(level);
        var strike = new EntityStruckByLightningEvent(dropped,lightning); NeoForge.EVENT_BUS.post(strike);
        check(strike.isCanceled() && dropped.getItem().is(ModItems.THUNDER_GUABA.get()) && dropped.getItem().getCount()==17 && dropped.getItem().getHoverName().getString().equals("Dropped"), "lightning conversion");
        mob.addEffect(new MobEffectInstance(GuabaPotions.THUNDER_GUABA, 100, 0));
        mob.getPersistentData().putLong("pinkkicore:thunder_guaba_next_lightning", 0);
        var strikes = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> tracker = e -> { if (e.getEntity() instanceof LightningBolt) strikes.incrementAndGet(); };
        NeoForge.EVENT_BUS.addListener(tracker);
        try { GuabaPotions.THUNDER_GUABA.get().applyEffectTick(mob,0); }
        finally { NeoForge.EVENT_BUS.unregister(tracker); }
        check(strikes.get()==1, "thunder actual lightning " + strikes);
        mob.removeAllEffects(); mob.discard();
        pass("four effect levels, transient cleanup, coexistence, real lightning spawn, dropped stack metadata");
    }
    static final class HitArrow extends Arrow {
        HitArrow(ServerLevel level, LivingEntity owner) { super(level,owner,new ItemStack(Items.ARROW),new ItemStack(ModItems.GUABA_BOW.get())); }
        void hit(Entity target) { onHitEntity(new EntityHitResult(target)); }
    }
    private static void combat(ServerLevel level, FakePlayer player) {
        var target=EntityType.COW.create(level); target.setNoAi(true); target.setPos(9,100,8);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); level.addFreshEntity(target);
        var bow=(GuabaBowItem)ModItems.GUABA_BOW.get();
        boolean[] rolls=new boolean[7];
        for(int i=0;i<250;i++) {
            target.removeAllEffects();target.setHealth(100);target.invulnerableTime=0;
            var arrow=new HitArrow(level,player);bow.customArrow(arrow,new ItemStack(Items.ARROW),new ItemStack(bow));
            arrow.setBaseDamage(2);arrow.setDeltaMovement(2,0,0);arrow.hit(target);
            int damage=Math.round(100-target.getHealth());check(damage>=1&&damage<=7,"bow damage " + damage);rolls[damage-1]=true;
            check(target.hasEffect(GuabaPotions.GUABA),"arrow missing Guaba");
        }
        for(boolean seen:rolls)check(seen,"bow missed roll");
        boolean zeroSeen=false;
        for(int i=0;i<100;i++) {
            target.removeAllEffects(); target.setHealth(100);target.invulnerableTime=0;
            var arrow=new HitArrow(level,player);bow.customArrow(arrow,new ItemStack(Items.ARROW),new ItemStack(bow));arrow.setBaseDamage(1);arrow.setDeltaMovement(1,0,0);arrow.hit(target);
            check(target.hasEffect(GuabaPotions.GUABA),"zero accepted arrow missing effect");zeroSeen|=target.getHealth()==100;
        }
        check(zeroSeen,"zero roll not exercised");
        class Blocker extends FakePlayer {
            Blocker() { super(level,new GameProfile(UUID.randomUUID(),"ShieldProbe")); }
            @Override public boolean isDamageSourceBlocked(net.minecraft.world.damagesource.DamageSource source) { return true; }
        }
        var blocker=new Blocker(); blocker.setPos(10,100,10);
        var blockedArrow=new HitArrow(level,player);bow.customArrow(blockedArrow,new ItemStack(Items.ARROW),new ItemStack(bow));blockedArrow.setDeltaMovement(2,0,0);blockedArrow.hit(blocker);
        check(!blocker.hasEffect(GuabaPotions.GUABA)&&blocker.getHealth()==blocker.getMaxHealth(),"blocked arrow applied Guaba");
        class Fighter extends FakePlayer {
            Fighter() { super(level,new GameProfile(UUID.randomUUID(),"SwordProbe")); }
            @Override public float getAttackStrengthScale(float partial) { return 1F; }
        }
        var fighter=new Fighter(); fighter.setPos(8,100,8); fighter.setOnGround(true);
        fighter.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.GUABA_SWORD.get()));
        fighter.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> fighter.getAttribute(attribute).addTransientModifier(modifier));
        for(int i=0;i<100;i++) {
            target.removeAllEffects();target.setHealth(100);target.invulnerableTime=0;
            fighter.attack(target);
            check(target.hasEffect(GuabaPotions.GUABA),"sword missing Guaba");
            check(fighter.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().stream().noneMatch(m -> m.id().equals(id("sword_roll"))),"sword roll leaked");
        }
        target.discard();pass("real arrow hits exercise all seven rolls; actual player sword attack applies effect and cleans roll");
    }
    private static void entitiesAndLoot(ServerLevel level) {
        for(var type:List.of(ModEntities.BINGWA.get(),ModEntities.PINKKI.get(),ModEntities.GUABA_YINGRONG.get())) {
            var mob=type.create(level);mob.setPos(20,100,20);level.addFreshEntity(mob);
            check(mob.getMaxHealth()==20 && mob.hasCustomName() && mob.isCustomNameVisible(),"NPC defaults");
            var tag=new CompoundTag();mob.save(tag);
            var loaded=EntityType.loadEntityRecursive(tag,level,e -> e);
            check(loaded!=null && loaded.getCustomName().equals(mob.getCustomName()),"entity NBT");
            var table=level.getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable());
            for(int i=0;i<120;i++) {
                var items=table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,mob).withParameter(LootContextParams.ORIGIN,mob.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY));
                check(items.size()==1 && (items.getFirst().is(ModItems.GUABA.get()) || items.getFirst().is(ModItems.THUNDER_GUABA.get()) || items.getFirst().is(PinkOreContent.INGOT.get())),"wildlife loot");
            }
            mob.discard();
        }
        var npc=ModEntities.PINKKI.get().create(level);
        check(npc.canHoldItem(new ItemStack(Items.DIAMOND_SWORD)) && !npc.canHoldItem(new ItemStack(Items.DIRT)),"NPC equipment filter");
        var egg=new ItemStack(ModItems.CHARGED_CREEPER_EGG.get());
        var player=FakePlayerFactory.getMinecraft(level);player.setItemInHand(InteractionHand.MAIN_HAND,egg);
        var pos=new BlockPos(30,100,30);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        var spawned=new java.util.ArrayList<net.minecraft.world.entity.monster.Creeper>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> tracker=e -> { if(e.getEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper)spawned.add(creeper); };
        NeoForge.EVENT_BUS.addListener(tracker);
        try { egg.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))); }
        finally { NeoForge.EVENT_BUS.unregister(tracker); }
        check(spawned.size()==1 && spawned.getFirst().isPowered(),"charged egg " + spawned);
        pass("three entity defaults/NBT and loot tables, NPC equipment filter, charged creeper egg use");
    }
}

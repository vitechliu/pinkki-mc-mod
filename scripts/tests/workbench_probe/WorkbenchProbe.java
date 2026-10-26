package pinkkicore.test;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.workbench.*;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

/** Executes against the actual release JAR in an isolated --nogui server. */
@Mod("pinkkicore_workbench_probe")
public final class WorkbenchProbe {
    private static final BlockPos POS = new BlockPos(4, 100, 4);
    private ServerLevel level;
    private GuabaWorkbenchEntity machine;
    public WorkbenchProbe() { MinecraftForge.EVENT_BUS.addListener(this::started); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void tick(int count) {
        for (int i = 0; i < count; i++) GuabaWorkbenchEntity.serverTick(level, POS, machine.getBlockState(), machine);
    }
    private void reset() {
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(POS, WorkbenchContent.BLOCK.get().defaultBlockState());
        machine = (GuabaWorkbenchEntity) level.getBlockEntity(POS);
    }
    private void feed(int guaba, int dirt) {
        machine.inventory.setStackInSlot(0, new ItemStack(ModItems.GUABA.get(), guaba));
        machine.inventory.setStackInSlot(1, new ItemStack(Items.DIRT, dirt));
        machine.inventory.setStackInSlot(2, ItemStack.EMPTY);
    }
    private IItemHandler port(Direction side) { return machine.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(AssertionError::new); }
    private void setSide(Direction side, SideMode target) {
        for (int i=0; i<SideMode.values().length && machine.sideMode(side)!=target; i++) machine.handleButton(10+side.ordinal());
        check(machine.sideMode(side)==target,"side mode cycle did not reach "+target);
    }
    private void started(ServerStartedEvent event) {
        try {
            level = event.getServer().overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
            level.setChunkForced(0, 0, true);
            for (int x=2; x<=6; x++) for (int z=2; z<=6; z++) for (int y=99; y<=103; y++)
                level.setBlockAndUpdate(new BlockPos(x,y,z), y == 99 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            reset(); feed(3, 3);
            tick(19);
            check(machine.inventory.getStackInSlot(2).isEmpty() && machine.inventory.getStackInSlot(0).getCount()==3, "consumed before tick 20");
            tick(1);
            check(machine.inventory.getStackInSlot(2).is(Items.MUD) && machine.inventory.getStackInSlot(2).getCount()==1, "conversion result");
            check(machine.inventory.getStackInSlot(0).getCount()==2 && machine.inventory.getStackInSlot(1).getCount()==2, "recipe costs");
            tick(40);
            check(machine.inventory.getStackInSlot(2).getCount()==3 && machine.inventory.getStackInSlot(0).isEmpty() && machine.inventory.getStackInSlot(1).isEmpty(), "continuous conversion");
            feed(0, 1); tick(25); check(machine.inventory.getStackInSlot(2).isEmpty(), "worked without guaba");
            feed(1, 1); machine.inventory.setStackInSlot(1, new ItemStack(Items.COBBLESTONE)); tick(25);
            check(machine.inventory.getStackInSlot(0).getCount()==1 && machine.inventory.getStackInSlot(2).isEmpty(), "unknown input consumed");
            feed(1,1); tick(10); machine.inventory.setStackInSlot(2,new ItemStack(Items.MUD,64)); tick(40);
            check(machine.progress()==10 && machine.inventory.getStackInSlot(0).getCount()==1, "full output did not pause");
            machine.inventory.setStackInSlot(2,new ItemStack(Items.STONE)); tick(25);
            check(machine.progress()==10 && machine.inventory.getStackInSlot(2).is(Items.STONE), "incompatible output overwritten");
            machine.inventory.setStackInSlot(2,ItemStack.EMPTY); tick(10);
            check(machine.inventory.getStackInSlot(2).is(Items.MUD), "blocked output did not resume");
            feed(1,1); tick(12); machine.inventory.extractItem(1,1,false); machine.inventory.insertItem(1,new ItemStack(Items.DIRT),false);
            tick(8); check(machine.inventory.getStackInSlot(2).isEmpty(), "input replacement retained old progress");
            System.out.println("WORKBENCH timing + atomic costs + continuous recipes + blocked/unknown/no-fuel + input replacement PASS");

            for (int mode=0; mode<4; mode++) for (boolean signal : new boolean[]{false,true}) {
                reset(); feed(1,1);
                for (int i=0;i<mode;i++) machine.handleButton(0);
                level.setBlockAndUpdate(POS.above(), signal ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
                check(machine.powered()==signal, "actual redstone signal"); tick(20);
                boolean expected = mode==0 || mode==1 && signal || mode==2 && !signal;
                check(machine.inventory.getStackInSlot(2).is(Items.MUD)==expected, "redstone truth table "+mode+"/"+signal);
            }
            reset(); feed(1,1); tick(7);
            for(int i=0;i<3;i++)machine.handleButton(0);
            tick(30); check(machine.progress()==7, "redstone pause lost progress"); machine.handleButton(0); tick(13);
            check(machine.inventory.getStackInSlot(2).is(Items.MUD), "redstone resume failed");
            System.out.println("WORKBENCH actual signal truth table (4 modes x 2 signals) + pause/resume PASS");

            reset();
            for(Direction side:Direction.values()) {
                IItemHandler handler=port(side);
                SideMode initial=machine.sideMode(side);
                for(int i=0;i<SideMode.values().length;i++) {
                    machine.inventory.setStackInSlot(2,new ItemStack(Items.MUD,4));
                    SideMode mode=machine.sideMode(side);
                    check(handler.insertItem(0,new ItemStack(ModItems.GUABA.get()),true).isEmpty()==(mode.input || mode==SideMode.GUABA_INPUT),"side catalyst input "+side+mode);
                    check(handler.insertItem(1,new ItemStack(Items.DIRT),true).isEmpty()==mode.input,"side material input "+side+mode);
                    check(!handler.insertItem(0,new ItemStack(Items.STONE),true).isEmpty(),"non-guaba fuel accepted");
                    check(!handler.insertItem(2,new ItemStack(Items.MUD),true).isEmpty(),"output insertion accepted");
                    check(!handler.extractItem(2,1,true).isEmpty()==mode.output,"side output "+side+mode);
                    check(handler.extractItem(0,1,true).isEmpty() && handler.extractItem(1,1,true).isEmpty(),"inputs extractable");
                    check(handler.isItemValid(0,new ItemStack(ModItems.GUABA.get()))==(mode.input || mode==SideMode.GUABA_INPUT),"reported guaba validity "+side+mode);
                    check(handler.isItemValid(1,new ItemStack(Items.DIRT))==mode.input,"reported input validity "+side+mode);
                    check(machine.inventory.getStackInSlot(0).isEmpty() && machine.inventory.getStackInSlot(1).isEmpty() && machine.inventory.getStackInSlot(2).getCount()==4,"simulation changed inventory");
                    machine.handleButton(10+side.ordinal());
                }
                check(machine.sideMode(side)==initial,"side mode cycle did not wrap");
            }
            check(!machine.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(), "unsided config bypass");
            var cached=port(Direction.NORTH);machine.handleButton(10+Direction.NORTH.ordinal()); // input -> output
            check(!cached.insertItem(1,new ItemStack(Items.DIRT),false).isEmpty(),"cached port ignored mode change");
            var cap=machine.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH);
            machine.invalidateCaps(); check(!cap.isPresent(),"capability invalidation");machine.reviveCaps();check(port(Direction.NORTH)!=cached,"capability revival");
            System.out.println("WORKBENCH all 6 faces x 5 IO modes + simulation + cached ports + capability lifecycle PASS");

            // A cached dedicated port must never fall through into the material/output slots.
            reset();var dedicated=port(Direction.UP);setSide(Direction.UP,SideMode.GUABA_INPUT);
            check(dedicated.insertItem(0,new ItemStack(ModItems.GUABA.get(),64),false).isEmpty(),"dedicated catalyst insert");
            check(!dedicated.insertItem(0,new ItemStack(ModItems.GUABA.get()),false).isEmpty(),"full catalyst slot accepted overflow");
            check(!dedicated.insertItem(1,new ItemStack(ModItems.GUABA.get()),false).isEmpty(),"dedicated port fell through to material slot");
            check(!dedicated.insertItem(2,new ItemStack(Items.MUD),false).isEmpty(),"dedicated port accepted output");
            check(machine.inventory.getStackInSlot(1).isEmpty() && machine.inventory.getStackInSlot(2).isEmpty(),"dedicated port polluted other slots");
            check(dedicated.extractItem(0,1,false).isEmpty(),"dedicated port extracted catalyst");
            var dedicatedSaved=machine.saveWithoutMetadata();var dedicatedRestored=new GuabaWorkbenchEntity(POS,machine.getBlockState());dedicatedRestored.load(dedicatedSaved);
            check(dedicatedRestored.sideMode(Direction.UP)==SideMode.GUABA_INPUT && dedicatedRestored.data.get(2+Direction.UP.ordinal())==4,"new mode NBT/menu sync");
            for(int legacy=0;legacy<4;legacy++) {
                dedicatedSaved.putInt("Side_up",legacy);dedicatedRestored.load(dedicatedSaved);
                check(dedicatedRestored.sideMode(Direction.UP).ordinal()==legacy,"legacy side mode ID changed");
            }
            System.out.println("WORKBENCH dedicated Guaba routing + overflow isolation + saved/menu mode 4 + legacy IDs PASS");

            reset();
            // Actual vanilla hoppers exercise Forge's hopper capability bridge.
            level.setBlockAndUpdate(POS.above(),Blocks.HOPPER.defaultBlockState());
            level.setBlockAndUpdate(POS.below(),Blocks.HOPPER.defaultBlockState());
            var upper=(HopperBlockEntity)level.getBlockEntity(POS.above());
            var lower=(HopperBlockEntity)level.getBlockEntity(POS.below());
            upper.setItem(0,new ItemStack(ModItems.GUABA.get()));upper.setItem(1,new ItemStack(Items.DIRT));
            for(int i=0;i<20;i++) { upper.setCooldown(0);HopperBlockEntity.pushItemsTick(level,POS.above(),upper.getBlockState(),upper); }
            check(machine.inventory.getStackInSlot(0).is(ModItems.GUABA.get()) && machine.inventory.getStackInSlot(1).is(Items.DIRT),"real hopper input");
            tick(20);
            // Default DOWN=OUTPUT. First disable it and verify a hopper cannot bypass it.
            setSide(Direction.DOWN,SideMode.DISABLED);
            lower.setCooldown(0);HopperBlockEntity.pushItemsTick(level,POS.below(),lower.getBlockState(),lower);
            check(lower.isEmpty() && machine.inventory.getStackInSlot(2).is(Items.MUD),"disabled side leaked to real hopper");
            setSide(Direction.DOWN,SideMode.OUTPUT);
            lower.setCooldown(0);HopperBlockEntity.pushItemsTick(level,POS.below(),lower.getBlockState(),lower);
            check(lower.getItem(0).is(Items.MUD) && machine.inventory.getStackInSlot(2).isEmpty(),"real hopper output");
            System.out.println("WORKBENCH real vanilla hopper input/output + disabled-side isolation PASS");

            reset();setSide(Direction.UP,SideMode.GUABA_INPUT);
            level.setBlockAndUpdate(POS.above(),Blocks.HOPPER.defaultBlockState());
            var guabaHopper=(HopperBlockEntity)level.getBlockEntity(POS.above());
            guabaHopper.setItem(0,new ItemStack(Items.DIRT,3));guabaHopper.setItem(1,new ItemStack(ModItems.GUABA.get(),2));
            for(int i=0;i<12;i++) { guabaHopper.setCooldown(0);HopperBlockEntity.pushItemsTick(level,POS.above(),guabaHopper.getBlockState(),guabaHopper); }
            check(machine.inventory.getStackInSlot(0).getCount()==2 && machine.inventory.getStackInSlot(1).isEmpty() && guabaHopper.getItem(0).getCount()==3,"dedicated real hopper routing");
            machine.inventory.setStackInSlot(1,new ItemStack(Items.DIRT));tick(20);
            check(machine.inventory.getStackInSlot(0).getCount()==1 && machine.inventory.getStackInSlot(2).is(Items.MUD),"auto supplied Guaba did not process");
            System.out.println("WORKBENCH real hopper dedicated Guaba input + foreign material retained + conversion PASS");

            reset();feed(2,2);tick(7);machine.handleButton(0);machine.handleButton(10+Direction.EAST.ordinal());
            CompoundTag saved=machine.saveWithoutMetadata();
            var restored=new GuabaWorkbenchEntity(POS,machine.getBlockState());restored.setLevel(level);restored.load(saved);
            check(restored.progress()==7 && restored.redstoneMode()==RedstoneMode.HIGH && restored.sideMode(Direction.EAST)==SideMode.OUTPUT,"NBT state/config");
            check(restored.inventory.getStackInSlot(1).getCount()==2,"NBT items");
            level.setBlockAndUpdate(POS.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            for(int i=0;i<13;i++)GuabaWorkbenchEntity.serverTick(level,POS,restored.getBlockState(),restored);
            check(restored.inventory.getStackInSlot(2).is(Items.MUD),"saved progress did not resume");
            saved.putInt("Progress",999);saved.putInt("Redstone",999);saved.putInt("Side_east",999);saved.getCompound("Inventory").putInt("Size",999);restored.load(saved);
            check(restored.inventory.getSlots()==3 && restored.progress()==19 && restored.redstoneMode()==RedstoneMode.IGNORE && restored.sideMode(Direction.EAST)==SideMode.DISABLED,"invalid NBT bounds");
            System.out.println("WORKBENCH NBT items + progress + IO/redstone + invalid value bounds PASS");

            reset(); var player=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("868c947e-1c3b-49af-9fe3-d1b4b745319b"),"WorkbenchProbe"));
            player.setPos(4,100,5); player.getInventory().clearContent();
            GuabaWorkbenchMenu menu=(GuabaWorkbenchMenu)machine.createMenu(1,player.getInventory(),player);
            check(menu.slots.size()==39 && menu.stillValid(player),"menu slots/validity");
            check(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)) && !menu.getSlot(2).mayPlace(new ItemStack(Items.MUD)),"GUI slot validation");
            player.getInventory().setItem(9,new ItemStack(ModItems.GUABA.get(),4)); menu.quickMoveStack(player,3);
            player.getInventory().setItem(10,new ItemStack(Items.DIRT,4)); menu.quickMoveStack(player,4);
            check(machine.inventory.getStackInSlot(0).getCount()==4 && machine.inventory.getStackInSlot(1).getCount()==4,"shift-click routing");
            tick(20);menu.quickMoveStack(player,2);check(machine.inventory.getStackInSlot(2).isEmpty(),"shift output");
            check(menu.clickMenuButton(player,0) && !menu.clickMenuButton(player,255),"button allowlist");
            player.setPos(100,100,100);check(!menu.clickMenuButton(player,0) && !menu.stillValid(player),"remote config bypass");
            player.setPos(4,100,5);
            var recipe=level.getRecipeManager().byKey(new ResourceLocation("pinkkicore","guaba_conversion/dirt_to_mud")).orElseThrow();
            FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
            WorkbenchContent.SERIALIZER.get().toNetwork(buffer,(GuabaConversionRecipe)recipe);
            var decoded=WorkbenchContent.SERIALIZER.get().fromNetwork(recipe.getId(),buffer);buffer.release();
            check(decoded.matches(new SimpleContainer(ItemStack.EMPTY,new ItemStack(Items.DIRT),ItemStack.EMPTY),level) && decoded.guabaCost()==1 && decoded.getResultItem(level.registryAccess()).is(Items.MUD),"recipe wire sync");
            try {
                WorkbenchContent.SERIALIZER.get().fromJson(new ResourceLocation("pinkkicore","bad"),JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:dirt\"},\"guaba_cost\":-1,\"result\":{\"item\":\"minecraft:mud\"}}").getAsJsonObject());
                throw new AssertionError("invalid recipe accepted");
            } catch(IllegalArgumentException | com.google.gson.JsonSyntaxException expected) {}
            System.out.println("WORKBENCH menu + shift-click + permission bounds + recipe loading/network validation PASS");

            reset();
            ItemStack diamond = new ItemStack(Items.DIAMOND_SWORD);
            diamond.setDamageValue(321);
            diamond.setHoverName(net.minecraft.network.chat.Component.literal("Named blade"));
            diamond.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 3);
            var originalTag = diamond.getTag().copy();
            machine.inventory.setStackInSlot(0,new ItemStack(ModItems.GUABA.get()));
            machine.inventory.setStackInSlot(1,diamond);
            tick(19); check(machine.inventory.getStackInSlot(2).isEmpty(), "sword converted early");
            tick(1);
            var sword = machine.inventory.getStackInSlot(2);
            check(sword.is(ModItems.GUABA_SWORD.get()) && sword.getTag().equals(originalTag), "sword metadata lost");
            check(machine.inventory.getStackInSlot(0).isEmpty() && machine.inventory.getStackInSlot(1).isEmpty(), "sword costs");
            var swordRecipe = (GuabaConversionRecipe)level.getRecipeManager().byKey(new ResourceLocation("pinkkicore","guaba_conversion/diamond_sword_to_guaba_sword")).orElseThrow();
            FriendlyByteBuf swordBuffer = new FriendlyByteBuf(Unpooled.buffer());
            WorkbenchContent.SERIALIZER.get().toNetwork(swordBuffer,swordRecipe);
            var swordDecoded = WorkbenchContent.SERIALIZER.get().fromNetwork(swordRecipe.getId(),swordBuffer); swordBuffer.release();
            var assembled = swordDecoded.assemble(new SimpleContainer(ItemStack.EMPTY,diamond,ItemStack.EMPTY),level.registryAccess());
            check(assembled.getTag().equals(originalTag), "wire lost tag preservation");
            assembled.setDamageValue(322); check(diamond.getDamageValue()==321, "tag alias");
            check(sword.getMaxDamage()==Items.DIAMOND_SWORD.getMaxDamage() && sword.getMaxStackSize()==1, "sword durability/stack");
            check(sword.getItem().isValidRepairItem(sword,new ItemStack(Items.DIAMOND)), "sword diamond repair");
            System.out.println("WORKBENCH sword conversion 19/20 ticks + costs + name/enchantment/damage preservation + wire + diamond repair PASS");

            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.GUABA_SWORD.get()));
            // Equipment attributes are normally applied by entity ticks. Apply the same vanilla modifier set here.
            player.getAttributes().addTransientAttributeModifiers(player.getMainHandItem().getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
            var target = net.minecraft.world.entity.EntityType.COW.create(level);
            target.setNoAi(true); target.setPos(5,100,5); level.addFreshEntity(target);
            var blade = (io.github.vitech.pinkkicore.forge.item.GuabaSwordItem)ModItems.GUABA_SWORD.get();
            boolean[] rolls = new boolean[7];
            for (int i=0;i<400;i++) {
                blade.onLeftClickEntity(player.getMainHandItem(),player,target);
                int value=(int)Math.round(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                check(value>=4 && value<=10, "sword roll bounds: "+value); rolls[value-4]=true;
            }
            for(boolean seen : rolls) check(seen,"missing sword roll");
            io.github.vitech.pinkkicore.forge.item.GuabaSwordItem.endTick(new net.minecraftforge.event.TickEvent.ServerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, () -> true, event.getServer()));
            check(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==7,"roll cleanup");
            blade.hurtEnemy(player.getMainHandItem(),target,player);
            var effect=target.getEffect(io.github.vitech.pinkkicore.forge.effect.GuabaPotions.GUABA.get());
            check(effect!=null && effect.getDuration()==100 && effect.getAmplifier()==0,"sword hit effect");
            target.removeAllEffects();
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(io.github.vitech.pinkkicore.forge.effect.GuabaPotions.GUABA.get(),200,1));
            blade.hurtEnemy(player.getMainHandItem(),target,player);
            check(target.getEffect(io.github.vitech.pinkkicore.forge.effect.GuabaPotions.GUABA.get()).getAmplifier()==1,"strong effect downgraded");
            // Exercise actual Player.attack -> Forge item hook -> vanilla damage -> hurtEnemy.
            class Fighter extends net.minecraftforge.common.util.FakePlayer {
                float strength = 1F;
                Fighter() { super(level,new GameProfile(UUID.randomUUID(),"SwordProbe")); }
                @Override public float getAttackStrengthScale(float partialTick) { return strength; }
            }
            var fighter = new Fighter();
            fighter.setPos(4,100,5);
            fighter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.GUABA_SWORD.get()));
            fighter.getAttributes().addTransientAttributeModifiers(fighter.getMainHandItem().getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
            fighter.setOnGround(true);
            for(int mode=0;mode<4;mode++) {
              fighter.strength = mode==1 ? .5F : 1F;
              fighter.setOnGround(mode!=2); fighter.fallDistance = mode==2 ? 1F : 0F;
              if(mode==3) fighter.getMainHandItem().enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);
              for(int i=0;i<30;i++) {
                target.removeAllEffects(); target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100); target.setHealth(100); target.invulnerableTime=0;
                fighter.attack(target);
                float dealt=100-target.getHealth();
                float min = mode==1 ? 1.6F : mode==2 ? 6 : mode==3 ? 6 : 4;
                float max = mode==1 ? 4 : mode==2 ? 15 : mode==3 ? 12 : 10;
                check(dealt>=min-.001F && dealt<=max+.001F,"actual attack bounds mode="+mode+": "+dealt);
                check(target.hasEffect(io.github.vitech.pinkkicore.forge.effect.GuabaPotions.GUABA.get()),"actual attack has no effect");
                check(fighter.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==7,"actual attack roll leaked");
            }
              }
            target.discard();
            System.out.println("WORKBENCH actual vanilla Player.attack x120 full/cooldown/critical/sharpness + hit effect + cleanup PASS");
            System.out.println("WORKBENCH sword 400 attack rolls + all 7 values + transient cleanup + 100-tick Guaba I + stronger effect retained PASS");

            reset(); BowChecks.run(level,machine,POS,player);

            reset();feed(2,3);machine.inventory.setStackInSlot(2,new ItemStack(Items.MUD,4));
            check(!menu.stillValid(player) && !menu.clickMenuButton(player,0),"same-position replacement reused stale menu");
            level.getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(4)).forEach(ItemEntity::discard);
            var state=machine.getBlockState();state.getBlock().playerWillDestroy(level,POS,state,player);
            level.destroyBlock(POS,true);
            int guaba=0,dirt=0,mud=0,block=0;
            for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(4))) {
                ItemStack item=entity.getItem();
                if(item.is(ModItems.GUABA.get()))guaba+=item.getCount();if(item.is(Items.DIRT))dirt+=item.getCount();
                if(item.is(Items.MUD))mud+=item.getCount();if(item.is(WorkbenchContent.ITEM.get()))block+=item.getCount();
            }
            check(guaba==2 && dirt==3 && mud==4 && block==1,"break drops: "+guaba+"/"+dirt+"/"+mud+"/"+block);
            check(!menu.stillValid(player),"removed machine menu stayed valid");
            System.out.println("WORKBENCH block + inventory drops exactly once + removed menu invalid PASS");
            System.out.println("WORKBENCH_PROBE PASS");
        } catch(Throwable error) { error.printStackTrace();System.out.println("WORKBENCH_PROBE FAIL "+error); }
    }
}

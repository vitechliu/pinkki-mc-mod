package pinkkicore.test;

import io.github.vitech.pinkkicore.forge.charging.*;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.workbench.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.common.util.FakePlayer;
import io.netty.buffer.Unpooled;

final class ChargingChecks {
    static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    static void tick(ServerLevel l,BlockPos p,GuabaChargingEntity e,int count) { for(int i=0;i<count;i++) GuabaChargingEntity.serverTick(l,p,e.getBlockState(),e); }
    static void run(ServerLevel level, FakePlayer player) {
        BlockPos pos=new BlockPos(10,100,4);level.setBlockAndUpdate(pos,ChargingContent.BLOCK.get().defaultBlockState());
        var machine=(GuabaChargingEntity)level.getBlockEntity(pos);
        check(level.getRecipeManager().getAllRecipesFor(ChargingContent.RECIPE.get()).size()==(net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")?4:2),"charging recipes count");
        IronChargingChecks.run(level,pos,machine);
        machine.inventory.setStackInSlot(0,new ItemStack(ModItems.GUABA.get(),2));
        tick(level,pos,machine,19);check(machine.inventory.getStackInSlot(1).isEmpty(),"charging early output");
        tick(level,pos,machine,1);check(machine.inventory.getStackInSlot(1).is(ModItems.THUNDER_GUABA.get()) && machine.inventory.getStackInSlot(0).getCount()==1,"charging result/cost");
        tick(level,pos,machine,20);check(machine.inventory.getStackInSlot(1).getCount()==2 && machine.inventory.getStackInSlot(0).isEmpty(),"charging continuous/no catalyst");
        machine.inventory.setStackInSlot(1,new ItemStack(ModItems.THUNDER_GUABA.get(),64));machine.inventory.setStackInSlot(0,new ItemStack(ModItems.GUABA.get()));
        tick(level,pos,machine,30);check(machine.inventory.getStackInSlot(0).getCount()==1,"charging full output consumed");
        machine.inventory.setStackInSlot(1,ItemStack.EMPTY);machine.inventory.setStackInSlot(0,new ItemStack(Items.CREEPER_SPAWN_EGG));tick(level,pos,machine,20);
        check(machine.inventory.getStackInSlot(1).is(ModItems.CHARGED_CREEPER_EGG.get()) && machine.inventory.getStackInSlot(0).isEmpty(),"charged creeper recipe");
        machine.inventory.setStackInSlot(1,ItemStack.EMPTY);machine.inventory.setStackInSlot(0,new ItemStack(Items.DIRT));tick(level,pos,machine,20);
        check(machine.inventory.getStackInSlot(0).is(Items.DIRT) && machine.inventory.getStackInSlot(1).isEmpty(),"charging unknown consumed");
        for(int mode=0;mode<4;mode++) for(boolean signal:new boolean[]{false,true}) {
            while(machine.redstoneMode().ordinal()!=mode)machine.handleButton(0);
            level.setBlockAndUpdate(pos.above(),signal?Blocks.REDSTONE_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState());
            machine.inventory.setStackInSlot(0,new ItemStack(ModItems.GUABA.get()));machine.inventory.setStackInSlot(1,ItemStack.EMPTY);tick(level,pos,machine,20);
            check(machine.inventory.getStackInSlot(1).isEmpty() != (mode==0 || mode==1&&signal || mode==2&&!signal),"charging redstone truth table");
        }
        while(machine.redstoneMode()!=RedstoneMode.IGNORE)machine.handleButton(0);level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        for(Direction side:Direction.values()) {
            var port=machine.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,side).orElseThrow(AssertionError::new);
            check(port.getSlots()==2,"charging hidden catalyst slot");
            for(int m=0;m<4;m++) {
                while(machine.sideMode(side).ordinal()!=m)machine.handleButton(10+side.ordinal());
                machine.inventory.setStackInSlot(0,ItemStack.EMPTY);machine.inventory.setStackInSlot(1,new ItemStack(ModItems.THUNDER_GUABA.get()));
                boolean in=port.insertItem(0,new ItemStack(ModItems.GUABA.get()),true).isEmpty();
                boolean out=!port.extractItem(1,1,true).isEmpty();
                check(in==(m==1||m==3) && out==(m==2||m==3),"charging side IO");
                check(machine.inventory.getStackInSlot(0).isEmpty(),"charging simulation mutated");
                check(!port.insertItem(1,new ItemStack(ModItems.GUABA.get()),false).isEmpty() && port.extractItem(0,1,false).isEmpty(),"charging slot bypass");
            }
            machine.handleButton(10+side.ordinal());check(machine.sideMode(side)==SideMode.DISABLED,"charging exposes catalyst mode");
        }
        machine.inventory.setStackInSlot(1,ItemStack.EMPTY);machine.inventory.setStackInSlot(0,new ItemStack(ModItems.GUABA.get()));tick(level,pos,machine,7);
        var saved=machine.saveWithFullMetadata();var restored=new GuabaChargingEntity(pos,machine.getBlockState());restored.setLevel(level);restored.load(saved);
        check(restored.progress()==7 && restored.inventory.getSlots()==2,"charging persistence");tick(level,pos,restored,13);check(restored.inventory.getStackInSlot(1).is(ModItems.THUNDER_GUABA.get()),"charging resume");
        player.setPos(10,100,5);player.getInventory().clearContent();
        var menu=(GuabaChargingMenu)machine.createMenu(9,player.getInventory(),player);
        check(menu.slots.size()==38 && menu.stillValid(player),"charging menu two slots");
        machine.inventory.setStackInSlot(0,ItemStack.EMPTY);player.getInventory().setItem(9,new ItemStack(ModItems.GUABA.get()));menu.quickMoveStack(player,2);
        check(machine.inventory.getStackInSlot(0).is(ModItems.GUABA.get()),"charging shift input");
        check(!menu.getSlot(1).mayPlace(new ItemStack(Items.DIRT)),"charging output accepts input");
        player.setPos(50,100,50);check(!menu.clickMenuButton(player,0),"charging remote menu bypass");
        var recipe=level.getRecipeManager().getAllRecipesFor(ChargingContent.RECIPE.get()).get(0);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());ChargingContent.SERIALIZER.get().toNetwork(buffer,recipe);
        var decoded=ChargingContent.SERIALIZER.get().fromNetwork(recipe.getId(),buffer);buffer.release();check(decoded.getResultItem().is(recipe.getResultItem().getItem()),"charging network result");
        var craft=(ShapedRecipe)level.getRecipeManager().byKey(new ResourceLocation("pinkkicore:guaba_charging_station")).orElseThrow(AssertionError::new);
        CraftingContainer grid=new CraftingContainer(menu,3,3);
        for(int i:new int[]{1,3,5})grid.setItem(i,new ItemStack(Items.LIGHTNING_ROD));grid.setItem(4,new ItemStack(Items.ENCHANTING_TABLE));
        for(int i=6;i<9;i++)grid.setItem(i,new ItemStack(ModItems.THUNDER_GUABA.get()));
        check(craft.matches(grid,level) && craft.assemble(grid).is(ChargingContent.ITEM.get()),"charging shaped layout");
        grid.setItem(0,new ItemStack(Items.LIGHTNING_ROD));check(!craft.matches(grid,level),"charging shaped empty corner");
        System.out.println("WORKBENCH charging 19/20 ticks + no catalyst + both conversions + full/unknown + 4 redstone x2 + 6 faces x4 + NBT resume + 38-slot menu + shift/permission + recipe wire + exact shaped recipe PASS");

        player.setPos(12,100,8);player.getAbilities().instabuild=false;player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CHARGED_CREEPER_EGG.get(),2));
        BlockPos floor=new BlockPos(12,99,8);level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        var result=ModItems.CHARGED_CREEPER_EGG.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
        var area=new AABB(floor).inflate(5);
        var creepers=level.getEntitiesOfClass(Creeper.class,area);
        check(result.consumesAction() && creepers.size()==1 && creepers.get(0).isPowered(),"egg did not spawn powered creeper");
        check(player.getMainHandItem().getCount()==1,"charged egg right-click cost");creepers.forEach(Creeper::discard);
        BlockPos dp=new BlockPos(12,100,10);level.setBlockAndUpdate(dp,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));
        var dispenser=(DispenserBlockEntity)level.getBlockEntity(dp);dispenser.setItem(0,new ItemStack(ModItems.CHARGED_CREEPER_EGG.get(),2));
        Blocks.DISPENSER.tick(level.getBlockState(dp),level,dp,level.random);
        creepers=level.getEntitiesOfClass(Creeper.class,new AABB(dp).inflate(5));
        check(creepers.size()==1 && creepers.get(0).isPowered() && dispenser.getItem(0).getCount()==1,"charged egg dispenser");creepers.forEach(Creeper::discard);
        check(net.minecraft.world.item.SpawnEggItem.byId(net.minecraft.world.entity.EntityType.CREEPER)==Items.CREEPER_SPAWN_EGG,"vanilla egg registry replaced");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());check(!menu.stillValid(player),"charging removed menu valid");
        System.out.println("WORKBENCH charged creeper egg actual right-click + vanilla dispenser + powered NBT + item consumption + vanilla egg mapping retained PASS");
    }
}

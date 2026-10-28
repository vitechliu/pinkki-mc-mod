package pinkkicore.test;

import io.github.vitech.pinkkicore.forge.charging.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

final class IronChargingChecks {
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    static void run(ServerLevel level,BlockPos pos,GuabaChargingEntity machine) {
        boolean loaded=ModList.get().isLoaded("irons_spellbooks");
        for(String name:new String[]{"glass_bottle_to_lightning_bottle","blank_rune_to_lightning_rune"}) {
            check(level.getRecipeManager().byKey(new ResourceLocation("pinkkicore:guaba_charging/compat/irons_spellbooks/"+name)).isPresent()==loaded,"Iron recipe condition "+name);
        }
        if(!loaded) {
            machine.inventory.setStackInSlot(0,new ItemStack(Items.GLASS_BOTTLE));
            ChargingChecks.tick(level,pos,machine,25);
            check(machine.inventory.getStackInSlot(0).is(Items.GLASS_BOTTLE) && machine.inventory.getStackInSlot(1).isEmpty(),"absent Iron consumed bottle");
            machine.inventory.setStackInSlot(0,ItemStack.EMPTY);
            System.out.println("WORKBENCH Iron absent: both recipes skipped before unknown item parsing + glass bottle unchanged PASS");
            return;
        }
        for(String[] pair:new String[][]{{"minecraft:glass_bottle","irons_spellbooks:lightning_bottle"},{"irons_spellbooks:blank_rune","irons_spellbooks:lightning_rune"}}) {
            Item input=ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair[0]));
            Item output=ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair[1]));
            check(input!=null && input!=Items.AIR && output!=null && output!=Items.AIR,"Iron registry item missing");
            machine.inventory.setStackInSlot(0,new ItemStack(input,2));machine.inventory.setStackInSlot(1,ItemStack.EMPTY);
            ChargingChecks.tick(level,pos,machine,19);
            check(machine.inventory.getStackInSlot(1).isEmpty() && machine.inventory.getStackInSlot(0).getCount()==2,"Iron conversion early");
            ChargingChecks.tick(level,pos,machine,1);
            check(machine.inventory.getStackInSlot(1).is(output) && machine.inventory.getStackInSlot(1).getCount()==1 && machine.inventory.getStackInSlot(0).getCount()==1,"Iron conversion 1:1/cost");
            ChargingChecks.tick(level,pos,machine,20);
            check(machine.inventory.getStackInSlot(1).getCount()==2 && machine.inventory.getStackInSlot(0).isEmpty(),"Iron continuous conversion");
            machine.inventory.setStackInSlot(1,new ItemStack(output,output.getMaxStackSize()));machine.inventory.setStackInSlot(0,new ItemStack(input));
            ChargingChecks.tick(level,pos,machine,25);
            check(machine.inventory.getStackInSlot(0).getCount()==1,"Iron full output consumed input");
            machine.inventory.setStackInSlot(1,new ItemStack(Items.DIRT));ChargingChecks.tick(level,pos,machine,25);
            check(machine.inventory.getStackInSlot(0).getCount()==1 && machine.inventory.getStackInSlot(1).is(Items.DIRT),"Iron incompatible output overwritten");
        }
        machine.inventory.setStackInSlot(0,ItemStack.EMPTY);machine.inventory.setStackInSlot(1,ItemStack.EMPTY);
        System.out.println("WORKBENCH Iron present: real registered items + 2 recipes 19/20 ticks + 1:1 costs + continuous + full/incompatible outputs PASS");
    }
}

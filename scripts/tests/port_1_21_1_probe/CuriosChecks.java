package pinkkicore.test;
import io.github.vitech.pinkkicore.neoforge.effect.GuabaPotions;
import io.github.vitech.pinkkicore.neoforge.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
final class CuriosChecks {
    static void tick(Player player) { NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(player)); }
    static void run(Player player) {
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow();
        var head=inv.getStacksHandler("head").orElseThrow().getStacks();
        var charm=inv.getStacksHandler("charm").orElseThrow().getStacks();
        PortProbe.check(head.getSlots()==1&&charm.getSlots()==1,"Curios slots");
        var hood=new ItemStack(ModItems.GUABA_HOOD.get());var blessing=new ItemStack(ModItems.PINK_BLESSING.get());
        PortProbe.check(head.isItemValid(0,hood)&&!head.isItemValid(0,blessing),"head tags");
        player.addEffect(new MobEffectInstance(GuabaPotions.GUABA,200));player.addEffect(new MobEffectInstance(GuabaPotions.THUNDER_GUABA,200));
        head.setStackInSlot(0,hood);tick(player);
        PortProbe.check(!player.hasEffect(GuabaPotions.GUABA)&&!player.hasEffect(GuabaPotions.THUNDER_GUABA),"hood cleanup");
        PortProbe.check(head.getStackInSlot(0).is(ModItems.GUABA_HOOD.get()),"hood disappeared");
        PortProbe.check(inv.findFirstCurio(stack -> stack.is(ModItems.GUABA_HOOD.get())).isPresent(),"hood not found by API");
        for(var effect:java.util.List.of(GuabaPotions.GUABA,GuabaPotions.THUNDER_GUABA))for(int amp=0;amp<4;amp++)
            PortProbe.check(!player.addEffect(new MobEffectInstance(effect,200,amp)),"hood immunity "+effect.getId()+" amp="+amp);
        PortProbe.check(player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200)),"unrelated effect blocked");
        head.setStackInSlot(0,ItemStack.EMPTY);tick(player);
        PortProbe.check(player.addEffect(new MobEffectInstance(GuabaPotions.GUABA,200)),"unequip immunity remained");player.removeAllEffects();
        double armor=player.getAttributeValue(Attributes.ARMOR),hp=player.getMaxHealth();
        for(int i=0;i<3;i++) {
            charm.setStackInSlot(0,blessing.copy());tick(player);tick(player);
            PortProbe.check(player.getAttributeValue(Attributes.ARMOR)==armor+2&&player.getMaxHealth()==hp+2,"blessing attributes");
            charm.setStackInSlot(0,ItemStack.EMPTY);tick(player);
            PortProbe.check(player.getAttributeValue(Attributes.ARMOR)==armor&&player.getMaxHealth()==hp,"blessing residual attributes");
        }
        PortProbe.pass("Curios slots/tags, both-family I-IV immunity, unrelated effect, unequip and repeated attributes");
    }
}

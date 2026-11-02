package pinkkicore.test;
import io.github.vitech.pinkkicore.neoforge.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.silentchaos512.gear.api.util.DataResource;
import net.silentchaos512.gear.api.util.PartGearKey;
import net.silentchaos512.gear.gear.material.MaterialInstance;
import net.silentchaos512.gear.setup.gear.GearTypes;
import net.silentchaos512.gear.setup.gear.PartTypes;
final class GearChecks {
    static void run(ServerLevel level) {
        for(var item:java.util.List.of(ModItems.GUABA.get(),ModItems.THUNDER_GUABA.get())) {
            var mat=MaterialInstance.from(new ItemStack(item));
            PortProbe.check(mat!=null&&mat.isValid(),"Silent Gear material registration");
            PortProbe.check(mat.getId().equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)),"material ID");
            PortProbe.check(mat.getIngredient().test(new ItemStack(item)),"material ingredient");
            PortProbe.check(mat.allowedInPart(PartTypes.MAIN.get())&&mat.allowedInPart(PartTypes.ROD.get()),"part coverage");
            PortProbe.check(!mat.getTraits(PartGearKey.of(GearTypes.TOOL,PartTypes.MAIN)).isEmpty(),"material traits");
        }
        for(String id:java.util.List.of("guaba_touch","thunder_guaba_touch","thunder_lightweight")) {
            var trait=DataResource.trait(PortProbe.id(id)).get();
            PortProbe.check(trait!=null&&!trait.getEffects().isEmpty(),"trait codec "+id);
        }
        class Fighter extends net.neoforged.neoforge.common.util.FakePlayer {
            Fighter() { super(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GearProbe")); }
            @Override public float getAttackStrengthScale(float tick) { return 1F; }
        }
        for(String material:java.util.List.of("guaba","thunder_guaba")) {
            var bladeItem=(net.silentchaos512.gear.item.CompoundPartItem)net.minecraft.core.registries.BuiltInRegistries.ITEM.get(PortProbe.id("silentgear:sword_blade"));
            var blade=net.silentchaos512.gear.gear.part.PartInstance.create(DataResource.part("silentgear:sword_blade"),bladeItem,DataResource.material(PortProbe.id(material)));
            var rod=net.silentchaos512.gear.gear.part.PartInstance.create(DataResource.part("silentgear:rod"),net.silentchaos512.gear.setup.SgItems.ROD.get(),DataResource.material(PortProbe.id(material)));
            var sword=((net.silentchaos512.gear.api.item.GearItem)net.minecraft.core.registries.BuiltInRegistries.ITEM.get(PortProbe.id("silentgear:sword"))).construct(java.util.List.of(blade,rod));
            var fighter=new Fighter();fighter.setOnGround(true);fighter.setPos(10,100,10);
            fighter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
            sword.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND,(attribute,modifier)->fighter.getAttribute(attribute).addTransientModifier(modifier));
            var target=net.minecraft.world.entity.EntityType.COW.create(level);target.setNoAi(true);target.setPos(11,100,10);level.addFreshEntity(target);
            fighter.attack(target);
            var effect=material.equals("guaba")?io.github.vitech.pinkkicore.neoforge.effect.GuabaPotions.GUABA:io.github.vitech.pinkkicore.neoforge.effect.GuabaPotions.THUNDER_GUABA;
            PortProbe.check(target.hasEffect(effect)&&target.getEffect(effect).getDuration()==200,"Silent Gear actual attack effect "+material);
            if(material.equals("thunder_guaba"))PortProbe.check(fighter.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifiers().stream().anyMatch(m->Math.abs(m.amount()-0.05)<1e-6),"thunder lightweight");
            target.discard();
        }
        PortProbe.pass("Silent Gear 4 materials/main/rod, actual constructed swords and player attacks, 200-tick touch and thunder lightweight");
    }
}

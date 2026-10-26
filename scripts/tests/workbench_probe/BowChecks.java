package pinkkicore.test;

import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import io.github.vitech.pinkkicore.forge.item.GuabaBowItem;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.workbench.GuabaWorkbenchEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.util.FakePlayer;

final class BowChecks {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static final class HitArrow extends Arrow {
        HitArrow(ServerLevel level, FakePlayer owner) { super(level, owner); }
        void hit(net.minecraft.world.entity.Entity target) { onHitEntity(new EntityHitResult(target)); }
    }
    static void run(ServerLevel level, GuabaWorkbenchEntity machine, BlockPos pos, FakePlayer player) {
        var bow = (GuabaBowItem)ModItems.GUABA_BOW.get();
        var input = new ItemStack(Items.BOW);
        input.setHoverName(Component.literal("Named bow")); input.setDamageValue(123);
        input.enchant(Enchantments.POWER_ARROWS,3);
        var originalTag = input.getTag().copy();
        machine.inventory.setStackInSlot(0,new ItemStack(ModItems.GUABA.get()));
        machine.inventory.setStackInSlot(1,input); machine.inventory.setStackInSlot(2,ItemStack.EMPTY);
        for(int i=0;i<19;i++) GuabaWorkbenchEntity.serverTick(level,pos,machine.getBlockState(),machine);
        check(machine.inventory.getStackInSlot(2).isEmpty(),"bow converted early");
        GuabaWorkbenchEntity.serverTick(level,pos,machine.getBlockState(),machine);
        var output=machine.inventory.getStackInSlot(2);
        check(output.is(bow) && output.getTag().equals(originalTag),"bow conversion metadata");
        check(machine.inventory.getStackInSlot(0).isEmpty() && machine.inventory.getStackInSlot(1).isEmpty(),"bow costs");
        check(output.getMaxDamage()==Items.BOW.getMaxDamage() && output.getMaxStackSize()==1,"bow durability/stack");
        System.out.println("WORKBENCH bow conversion timing + costs + name/enchantment/spent durability PASS");

        var target=EntityType.COW.create(level);
        target.setNoAi(true); target.setPos(8,100,8);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); level.addFreshEntity(target);
        // Real vanilla onHitEntity computes speed-based damage and calls the Forge damage pipeline.
        boolean[] seen=new boolean[7];
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(int i=0;i<400;i++) {
            target.removeAllEffects(); target.setHealth(100); target.invulnerableTime=0;
            HitArrow arrow=new HitArrow(level,player); bow.customArrow(arrow);
            arrow.setBaseDamage(2); arrow.setDeltaMovement(2,0,0); arrow.hit(target);
            float damage=100-target.getHealth();
            check(damage>=1 && damage<=7,"bow damage outside vanilla 4 +/-3: "+damage);
            seen[Math.round(damage)-1]=true;
            var effect=target.getEffect(GuabaPotions.GUABA.get());
            check(effect!=null && effect.getDuration()==100 && effect.getAmplifier()==0,"bow hit effect");
        }
        for(boolean value:seen)check(value,"bow missing random value");
        target.removeAllEffects(); target.setHealth(100); target.invulnerableTime=0;
        var ordinary=new HitArrow(level,player); ordinary.setDeltaMovement(2,0,0); ordinary.hit(target);
        check(100-target.getHealth()==4 && !target.hasEffect(GuabaPotions.GUABA.get()),"ordinary arrow modified");
        boolean zeroSeen=false;
        for(int i=0;i<100;i++) {
            target.removeAllEffects(); target.setHealth(100); target.invulnerableTime=0;
            var arrow=new HitArrow(level,player); bow.customArrow(arrow);
            arrow.setBaseDamage(1); arrow.setDeltaMovement(1,0,0); arrow.hit(target);
            float damage=100-target.getHealth(); check(damage>=0 && damage<=4,"negative bow damage");
            check(target.hasEffect(GuabaPotions.GUABA.get()),"zero-damage accepted hit lost Guaba");
            zeroSeen |= damage==0;
        }
        check(zeroSeen,"zero clamp not exercised");
        target.removeAllEffects(); target.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),200,1));
        target.invulnerableTime=0;
        var strong=new HitArrow(level,player); bow.customArrow(strong); strong.setDeltaMovement(2,0,0); strong.hit(target);
        check(target.getEffect(GuabaPotions.GUABA.get()).getAmplifier()==1,"bow downgraded stronger effect");
        target.removeAllEffects(); target.setInvulnerable(true);
        var blocked=new HitArrow(level,player); bow.customArrow(blocked); blocked.setDeltaMovement(2,0,0); blocked.hit(target);
        check(!target.hasEffect(GuabaPotions.GUABA.get()),"invulnerable target got effect");
        target.setInvulnerable(false); target.discard();
        System.out.println("WORKBENCH bow actual vanilla arrow collisions x500 random +/-3 + all 7 values + zero clamp/effect + switched weapon + normal arrow exclusion + stronger/immune target PASS");

        player.getInventory().clearContent(); player.setPos(4,100,5);
        player.getAbilities().instabuild=false;
        var stack=new ItemStack(bow); stack.enchant(Enchantments.POWER_ARROWS,3);
        stack.enchant(Enchantments.PUNCH_ARROWS,2); stack.enchant(Enchantments.FLAMING_ARROWS,1);
        stack.enchant(Enchantments.INFINITY_ARROWS,1);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        player.getInventory().setItem(9,new ItemStack(Items.ARROW,3));
        var area=new AABB(player.blockPosition()).inflate(32);
        level.getEntitiesOfClass(AbstractArrow.class,area).forEach(AbstractArrow::discard);
        bow.use(level,player,InteractionHand.MAIN_HAND);
        bow.releaseUsing(stack,level,player,72000-20); player.stopUsingItem();
        var arrows=level.getEntitiesOfClass(AbstractArrow.class,area);
        check(arrows.size()==1,"bow did not fire exactly one arrow");
        var shot=arrows.get(0);
        check(shot.getBaseDamage()==4 && shot.getKnockback()==2 && shot.isOnFire() && shot.isCritArrow(),"vanilla bow enchantments");
        check(stack.getDamageValue()==1 && player.getInventory().getItem(9).getCount()==3,"bow durability/infinity ammo");
        check(shot.pickup==AbstractArrow.Pickup.CREATIVE_ONLY,"infinity arrow pickup");
        var saved=new CompoundTag(); shot.saveWithoutId(saved);
        var loaded=new HitArrow(level,player); loaded.load(saved);
        var victim=EntityType.COW.create(level); victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); victim.setHealth(100);
        loaded.setCritArrow(false); loaded.setBaseDamage(2); loaded.setDeltaMovement(2,0,0); loaded.hit(victim);
        check(victim.hasEffect(GuabaPotions.GUABA.get()),"real fired arrow lost origin after save/load");
        shot.discard();
        // Without Infinity the vanilla bow consumes one arrow, with identical durability use.
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(bow));
        bow.releaseUsing(player.getMainHandItem(),level,player,72000-20);
        check(player.getInventory().getItem(9).getCount()==2 && player.getMainHandItem().getDamageValue()==1,"normal ammo consumption");
        level.getEntitiesOfClass(AbstractArrow.class,area).forEach(AbstractArrow::discard);
        var tipped=new Arrow(level,player);
        tipped.setEffectsFromItem(net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW),net.minecraft.world.item.alchemy.Potions.POISON));
        check(bow.customArrow(tipped)==tipped,"tipped arrow replaced");
        var spectral=new SpectralArrow(level,player); check(bow.customArrow(spectral)==spectral,"spectral arrow replaced");
        System.out.println("WORKBENCH actual bow release + Power/Punch/Flame/Infinity + ammo/durability/pickup + projectile save/load + tipped/spectral preservation PASS");
    }
}

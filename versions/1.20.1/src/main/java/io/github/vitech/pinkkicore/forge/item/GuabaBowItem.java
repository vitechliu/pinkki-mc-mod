package io.github.vitech.pinkkicore.forge.item;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Vanilla bow behavior; the projectile carries its origin even after switching weapons or saving. */
@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class GuabaBowItem extends BowItem {
    private static final String GUABA_ARROW = "pinkkicore:guaba_arrow";

    public GuabaBowItem() { super(new Item.Properties().durability(384)); }

    @Override public AbstractArrow customArrow(AbstractArrow arrow) {
        arrow.getPersistentData().putBoolean(GUABA_ARROW, true);
        return arrow;
    }

    private static AbstractArrow guabaArrow(DamageSource source) {
        return source.getDirectEntity() instanceof AbstractArrow arrow
            && arrow.getPersistentData().getBoolean(GUABA_ARROW) ? arrow : null;
    }

    private static void applyEffect(LivingEntity target, AbstractArrow arrow) {
        target.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(), 100, 0), arrow.getOwner());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void randomDamage(LivingHurtEvent event) {
        var arrow = guabaArrow(event.getSource());
        if (arrow == null || event.getEntity().level().isClientSide || event.getAmount() <= 0) return;
        // Add to the actual vanilla arrow hit, including draw strength, Power and critical rolls; armor still applies.
        float damage = Math.max(0, event.getAmount() + event.getEntity().getRandom().nextInt(7) - 3);
        event.setAmount(damage);
        // Vanilla skips LivingDamageEvent when pre-armor damage is zero, but this was still an accepted hit.
        if (damage == 0) applyEffect(event.getEntity(), arrow);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hitEffect(LivingDamageEvent event) {
        var arrow = guabaArrow(event.getSource());
        if (arrow != null && !event.getEntity().level().isClientSide) {
            // Apply after armor calculation so the new Guaba armor roll cannot reduce this same arrow's damage.
            applyEffect(event.getEntity(), arrow);
        }
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.pinkkicore.guaba_bow.damage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.pinkkicore.guaba_sword.effect").withStyle(ChatFormatting.GRAY));
    }
}

package io.github.vitech.pinkkicore.forge.item;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Roll before vanilla reads attack damage, so cooldown, criticals and enchantments retain vanilla rules. */
@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class GuabaSwordItem extends SwordItem {
    private static final UUID ROLL = UUID.fromString("7948b8df-a2b1-45cf-9c18-38f8f3863f56");
    private static final Set<Player> ROLLED = Collections.newSetFromMap(new WeakHashMap<>());

    public GuabaSwordItem() { super(Tiers.DIAMOND, 3, -2.4F, new Item.Properties().tab(net.minecraft.world.item.CreativeModeTab.TAB_COMBAT)); }

    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level.isClientSide) {
            clearRoll(player);
            int delta = player.getRandom().nextInt(7) - 3;
            player.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(
                new AttributeModifier(ROLL, "Guaba sword roll", delta, AttributeModifier.Operation.ADDITION));
            ROLLED.add(player);
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeAttack(AttackEntityEvent event) {
        // A blocked hit has no hurtEnemy callback. Clear it before any subsequent attack, even with another item.
        if (!event.getEntity().level.isClientSide && ROLLED.remove(event.getEntity())) clearRoll(event.getEntity());
    }

    private static void clearRoll(Player player) {
        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ROLL);
    }

    @SubscribeEvent public static void endTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Player player : ROLLED) clearRoll(player);
        ROLLED.clear();
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level.isClientSide) {
            target.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(), 100, 0), attacker);
            // Vanilla has already calculated damage; don't let the roll affect another attack in this tick.
            if (attacker instanceof Player player) { clearRoll(player); ROLLED.remove(player); }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.pinkkicore.guaba_sword.damage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.pinkkicore.guaba_sword.effect").withStyle(ChatFormatting.GRAY));
    }
}

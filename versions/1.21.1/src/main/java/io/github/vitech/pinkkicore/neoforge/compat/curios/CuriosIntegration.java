package io.github.vitech.pinkkicore.neoforge.compat.curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.vitech.pinkkicore.neoforge.effect.GuabaEffectFamily;
import io.github.vitech.pinkkicore.neoforge.item.ModItems;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import net.neoforged.bus.api.EventPriority;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Only loaded after ModList confirms the optional Curios dependency. */
public final class CuriosIntegration {
    private CuriosIntegration() {}
    public static void register() {
        CuriosApi.registerCurio(ModItems.GUABA_HOOD.get(), new ICurioItem() {
            @Override public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) { clearGuaba(context.entity()); }
            @Override public void curioTick(SlotContext context, ItemStack stack) { clearGuaba(context.entity()); }
        });
        CuriosApi.registerCurio(ModItems.PINK_BLESSING.get(), new ICurioItem() {
            @Override public Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext context, net.minecraft.resources.ResourceLocation uuid, ItemStack stack) {
                Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
                modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid, 2, AttributeModifier.Operation.ADD_VALUE));
                modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, 2, AttributeModifier.Operation.ADD_VALUE));
                return modifiers;
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CuriosIntegration::onApplicable);
    }
    private static boolean wearingHood(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(inv -> inv.findFirstCurio(stack -> stack.is(ModItems.GUABA_HOOD.get())).isPresent()).orElse(false);
    }
    private static void onApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect().value() instanceof GuabaEffectFamily && wearingHood(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
    private static void clearGuaba(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        for (var effect : new ArrayList<>(entity.getActiveEffects())) {
            if (effect.getEffect().value() instanceof GuabaEffectFamily) entity.removeEffect(effect.getEffect());
        }
    }
}

package io.github.vitech.pinkkicore.forge.compat.curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.vitech.pinkkicore.forge.effect.GuabaEffectFamily;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
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
            @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
                Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
                modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid, "Pink blessing armor", 2, AttributeModifier.Operation.ADDITION));
                modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, "Pink blessing health", 2, AttributeModifier.Operation.ADDITION));
                return modifiers;
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CuriosIntegration::onApplicable);
    }
    private static boolean wearingHood(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(inv -> inv.findFirstCurio(ModItems.GUABA_HOOD.get()).isPresent()).orElse(false);
    }
    private static void onApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() instanceof GuabaEffectFamily && wearingHood(event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }
    private static void clearGuaba(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        for (var effect : new ArrayList<>(entity.getActiveEffects())) {
            if (effect.getEffect() instanceof GuabaEffectFamily) entity.removeEffect(effect.getEffect());
        }
    }
}

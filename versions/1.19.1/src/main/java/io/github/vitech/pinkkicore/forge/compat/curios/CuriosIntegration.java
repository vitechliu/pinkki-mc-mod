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


/** Only loaded after ModList confirms the optional Curios dependency. */
public final class CuriosIntegration {
    private CuriosIntegration() {}
    public static void register() {
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, CuriosIntegration::attach);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CuriosIntegration::onApplicable);
    }
    public static void registerSlots(net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent event) {
        for (var preset : java.util.List.of(top.theillusivec4.curios.api.SlotTypePreset.HEAD, top.theillusivec4.curios.api.SlotTypePreset.CHARM)) {
            net.minecraftforge.fml.InterModComms.sendTo("curios", top.theillusivec4.curios.api.SlotTypeMessage.REGISTER_TYPE,
                () -> preset.getMessageBuilder().size(1).build());
        }
    }
    private static void attach(net.minecraftforge.event.AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (!stack.is(ModItems.GUABA_HOOD.get()) && !stack.is(ModItems.PINK_BLESSING.get())) return;
        var curio = new top.theillusivec4.curios.api.type.capability.ICurio() {
            @Override public ItemStack getStack() { return stack; }
            @Override public void onEquip(SlotContext context, ItemStack previous) { if (stack.is(ModItems.GUABA_HOOD.get())) clearGuaba(context.entity()); }
            @Override public void curioTick(SlotContext context) { if (stack.is(ModItems.GUABA_HOOD.get())) clearGuaba(context.entity()); }
            @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid) {
                Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
                if (stack.is(ModItems.PINK_BLESSING.get())) {
                    modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid, "Pink blessing armor", 2, AttributeModifier.Operation.ADDITION));
                    modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, "Pink blessing health", 2, AttributeModifier.Operation.ADDITION));
                }
                return modifiers;
            }
        };
        var optional = net.minecraftforge.common.util.LazyOptional.of(() -> curio);
        event.addCapability(new net.minecraft.resources.ResourceLocation("pinkkicore", "accessory"), new net.minecraftforge.common.capabilities.ICapabilityProvider() {
            @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
                return cap == top.theillusivec4.curios.api.CuriosCapability.ITEM ? optional.cast() : net.minecraftforge.common.util.LazyOptional.empty();
            }
        });
        event.addListener(optional::invalidate);
    }
    private static boolean wearingHood(LivingEntity entity) {
        return CuriosApi.getCuriosHelper().findFirstCurio(entity, ModItems.GUABA_HOOD.get()).isPresent();
    }
    private static void onApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() instanceof GuabaEffectFamily && wearingHood(event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }
    private static void clearGuaba(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        for (var effect : new ArrayList<>(entity.getActiveEffects())) {
            if (effect.getEffect() instanceof GuabaEffectFamily) entity.removeEffect(effect.getEffect());
        }
    }
}

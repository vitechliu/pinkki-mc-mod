package io.github.vitech.pinkkicore.forge.item;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Only the struck dropped material changes; normal lightning behavior remains in place. */
@Mod.EventBusSubscriber(modid = ModInfo.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GuabaLightningConversion {
    private GuabaLightningConversion() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLightningStrike(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ItemEntity entity)
                || entity.level.isClientSide || !entity.isAlive()) return;
        ItemStack input = entity.getItem();
        if (input.is(ModItems.THUNDER_GUABA.get())) {
            // Lightning can hit the same entity on several ticks/flashes. Never convert twice
            // or let the newly converted stack be destroyed by a later flash.
            event.setCanceled(true);
            return;
        }
        if (!input.is(ModItems.GUABA.get())) return;
        ItemStack output = new ItemStack(ModItems.THUNDER_GUABA.get(), input.getCount());
        if (input.hasTag()) output.setTag(input.getTag().copy());
        // Keep the entity itself: position, motion, age, owner and pickup delay survive.
        entity.setItem(output);
        entity.clearFire();
        event.setCanceled(true);
    }
}

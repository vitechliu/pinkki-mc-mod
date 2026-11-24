package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.niumo.NiumoSounds;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModInfo.ID, value = Dist.CLIENT)
public final class NiumoSoundClient {
    private NiumoSoundClient() {}

    @SubscribeEvent
    public static void skipEmptyBank(PlaySoundEvent event) {
        var sound = event.getSound();
        if (sound != null && NiumoSounds.RANDOM_ID.equals(sound.getLocation())) {
            var bank = Minecraft.getInstance().getSoundManager().getSoundEvent(sound.getLocation());
            if (bank == null || bank.getWeight() == 0) event.setSound(null);
        }
    }
}

package io.github.vitech.pinkkicore.forge.niumo;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class NiumoSounds {
    public static final ResourceLocation RANDOM_ID = new ResourceLocation(ModInfo.ID, "niumo_random");

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ModInfo.ID);
    private static final RegistryObject<SoundEvent> RANDOM = SOUNDS.register(RANDOM_ID.getPath(),
        () -> SoundEvent.createVariableRangeEvent(RANDOM_ID));

    private NiumoSounds() {}

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    public static void play(ServerLevel level, Vec3 position, SoundSource source) {
        level.playSound(null, position.x, position.y, position.z, RANDOM.get(), source, 1.0F, 1.0F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionThrown(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
            || !(event.getEntity() instanceof ThrownPotion potion) || !(potion.getOwner() instanceof Player)) return;
        var item = potion.getItem();
        if (item.is(Items.SPLASH_POTION) && PotionUtils.getPotion(item) == GuabaPotions.NIUMO_SPLASH.get()) {
            play(level, potion.position(), SoundSource.PLAYERS);
        }
    }
}

package io.github.vitech.pinkkicore.forge.galgame;

import io.github.vitech.pinkkicore.core.ModInfo;
import io.github.vitech.pinkkicore.forge.effect.GuabaPotions;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class GalgameEffect extends MobEffect {
    private static final String BOOSTED = ModInfo.ID + ".galgame_heart_boosted";
    private static final int ARCADE_TIME = 20 * 60 * 10;
    static final int EQUIPMENT_TIME = 20 * 10;
    private static final int MAX_BASE_AMPLIFIER = Byte.MAX_VALUE - 1;

    public GalgameEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xED78AF);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, "889da218-9d13-4b64-92b9-711d518f9b3e", 0.50,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "8bdf4545-43fa-49eb-a8bd-1459c47e9317", 0.10,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public static void useArcade(Player player) {
        syncHeart(player);
        var current = player.getEffect(GuabaPotions.GALGAME.get());
        int base = baseAmplifier(player, current);
        if (base <= 1) {
            grant(player, ARCADE_TIME, base < 0 ? 0 : 1);
        }
    }

    static void grant(LivingEntity entity, int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(GuabaPotions.GALGAME.get(), duration, amplifier));
    }

    private static int baseAmplifier(Player player, MobEffectInstance instance) {
        return instance == null ? -1 : instance.getAmplifier() - (player.getPersistentData().getBoolean(BOOSTED) ? 1 : 0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void added(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide || event.getEffectInstance() == event.getOldEffectInstance()
            || event.getEffectInstance().getEffect() != GuabaPotions.GALGAME.get()) {
            return;
        }
        int bonus = 0;
        if (event.getEntity() instanceof Player player) {
            syncHeart(player);
            bonus = GalgameAccessories.hasHeart(player) ? 1 : 0;
            player.getPersistentData().putBoolean(BOOSTED, bonus != 0);
        }
        for (var effect = event.getEffectInstance(); effect != null; effect = effect.hiddenEffect) {
            effect.amplifier = Math.max(0, Math.min(MAX_BASE_AMPLIFIER, effect.getAmplifier())) + bonus;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            syncHeart(event.player);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() && event.getOriginal().getPersistentData().getBoolean(BOOSTED)) {
            event.getEntity().getPersistentData().putBoolean(BOOSTED, true);
        }
    }

    public static void syncHeart(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        MobEffectInstance active = player.getEffect(GuabaPotions.GALGAME.get());
        boolean before = player.getPersistentData().getBoolean(BOOSTED);
        if (active == null) {
            player.getPersistentData().remove(BOOSTED);
            return;
        }
        boolean boosted = GalgameAccessories.hasHeart(player);
        if (boosted == before) {
            return;
        }
        player.getPersistentData().putBoolean(BOOSTED, boosted);
        for (var effect = active; effect != null; effect = effect.hiddenEffect) {
            effect.amplifier = Math.max(0, Math.min(MAX_BASE_AMPLIFIER, effect.getAmplifier() - (before ? 1 : 0)))
                + (boosted ? 1 : 0);
        }
        player.onEffectUpdated(active, true, null);
    }

    static boolean isDirectMelee(DamageSource source) {
        return source.getDirectEntity() == source.getEntity()
            && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void reduceDamage(LivingDamageEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity)
            || (!isDirectMelee(source) && !(source.getDirectEntity() instanceof Projectile)
                && !source.is(DamageTypeTags.IS_PROJECTILE))
            || source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player player) {
            syncHeart(player);
        }
        MobEffectInstance effect = victim.getEffect(GuabaPotions.GALGAME.get());
        if (effect != null) {
            event.setAmount(event.getAmount() * Math.max(0, 5 - (effect.getAmplifier() + 1)) / 5);
        }
    }
}

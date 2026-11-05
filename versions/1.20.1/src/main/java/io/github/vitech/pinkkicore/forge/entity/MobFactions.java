package io.github.vitech.pinkkicore.forge.entity;

import io.github.vitech.pinkkicore.core.MobFaction;
import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** One server-side rivalry rule for all PinkkiCore mobs, including newly added entity types. */
@Mod.EventBusSubscriber(modid = ModInfo.ID)
public final class MobFactions {
    private MobFactions() {}

    public static boolean isPinkkiCoreMob(Mob mob) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return key != null && ModInfo.ID.equals(key.getNamespace());
    }

    public static MobFaction factionOf(Mob mob) {
        if (!isPinkkiCoreMob(mob)) throw new IllegalArgumentException("Not a PinkkiCore mob");
        if (mob instanceof GuabaYingrongEntity) return MobFaction.SPECIAL;
        return mob.getType().getCategory() == MobCategory.MONSTER ? MobFaction.HOSTILE : MobFaction.NEUTRAL;
    }

    @SubscribeEvent public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)
                || !isPinkkiCoreMob(mob) || mob instanceof GuabaYingrongEntity) return;
        // Dimension transfer can join the same object twice. Never accumulate duplicate goals.
        if (mob.targetSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof AttackYingrongGoal)) {
            // All existing retaliation/player acquisition goals have priority 1 or greater.
            mob.targetSelector.addGoal(0, new AttackYingrongGoal(mob));
        }
    }

    private static final class AttackYingrongGoal extends NearestAttackableTargetGoal<GuabaYingrongEntity> {
        AttackYingrongGoal(Mob mob) {
            super(mob, GuabaYingrongEntity.class, 0, true, false, target -> target != mob);
        }
    }
}

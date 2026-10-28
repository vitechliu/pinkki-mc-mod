package io.github.vitech.pinkkicore.forge.entity;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/** A persistent humanoid NPC. Equipment storage, comparison and pickup use Mob's rules. */
public abstract class HumanoidNpcEntity extends PathfinderMob {
    protected HumanoidNpcEntity(EntityType<? extends HumanoidNpcEntity> type, Level level) {
        super(type, level);
        setCustomName(type.getDescription());
        setCustomNameVisible(true);
        setCanPickUpLoot(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20)
            .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 2)
            .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Migrate old unnamed mobs, while preserving saved/custom names and explicit visibility.
        if (!tag.contains("CustomName", 8)) {
            setCustomName(getType().getDescription());
            setCustomNameVisible(true);
        } else if (!tag.contains("CustomNameVisible", 1)) {
            setCustomNameVisible(true);
        }

        // /summon supplies a partial tag; Mob otherwise clears the constructor default.
        if (!tag.contains("PersistenceRequired")) setPersistenceRequired();
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(2, new CollectEquipmentGoal());
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override public boolean canHoldItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ArmorItem || item instanceof SwordItem || item instanceof AxeItem
            || item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem
            || item instanceof ShieldItem;
    }

    @Override public boolean wantsToPickUp(ItemStack stack) {
        return canHoldItem(stack) && canReplaceCurrentItem(stack, getItemBySlot(getEquipmentSlotForItem(stack)));
    }

    private final class CollectEquipmentGoal extends Goal {
        private ItemEntity item;
        private int nextScanTick;
        private int nextPathTick;
        CollectEquipmentGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean available(ItemEntity candidate) {
            return candidate.isAlive() && !candidate.hasPickUpDelay()
                && canHoldItem(candidate.getItem()) && wantsToPickUp(candidate.getItem());
        }
        @Override public boolean canUse() {
            if (getTarget() != null || !canPickUpLoot()
                    || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
            if (tickCount < nextScanTick) return false;
            nextScanTick = tickCount + 10;
            item = level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8, 3, 8), this::available)
                .stream().filter(candidate -> getSensing().hasLineOfSight(candidate))
                .min(Comparator.comparingDouble(HumanoidNpcEntity.this::distanceToSqr)).orElse(null);
            return item != null;
        }
        @Override public boolean canContinueToUse() {
            return getTarget() == null && canPickUpLoot() && item != null && available(item)
                && distanceToSqr(item) <= 144 && !getNavigation().isDone()
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        }
        @Override public void start() { getNavigation().moveTo(getNavigation().createPath(item, 0), 1.0); nextPathTick = tickCount + 10; }
        @Override public void tick() {
            getLookControl().setLookAt(item, 30, 30);
            if (tickCount >= nextPathTick) {
                getNavigation().moveTo(getNavigation().createPath(item, 0), 1.0); nextPathTick = tickCount + 10;
            }
            // Actual pickup remains in Mob.aiStep, including Forge's griefing hook.
        }
        @Override public void stop() { item = null; getNavigation().stop(); }
    }
}

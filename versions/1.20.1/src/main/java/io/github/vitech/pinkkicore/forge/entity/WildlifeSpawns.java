package io.github.vitech.pinkkicore.forge.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;

/** Rare outdoor spawning; eggs, commands and other explicit spawning keep their existing rules. */
public final class WildlifeSpawns {
    private WildlifeSpawns() {}

    public static void register(SpawnPlacementRegisterEvent event) {
        for (EntityType<? extends Mob> type : java.util.List.<EntityType<? extends Mob>>of(
                ModEntities.PINKKI.get(), ModEntities.GUABA_YINGRONG.get(), ModEntities.BINGWA.get())) {
            event.register(type, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WildlifeSpawns::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    private static boolean canSpawn(EntityType<? extends Mob> type, ServerLevelAccessor level,
                                    MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        if (level.getLevel().dimension() != Level.OVERWORLD || !level.getBiome(pos).is(BiomeTags.IS_OVERWORLD)
                || !level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY()
                || !level.canSeeSky(pos) || level.getRawBrightness(pos, 0) <= 8
                || !Mob.checkMobSpawnRules(type, level, reason, pos, random)
                || random.nextInt(20) != 0) return false;
        // Our persistent NPCs bypass the normal creature cap. Bound total local accumulation.
        return level.getLevel().getEntitiesOfClass(Mob.class, new AABB(pos).inflate(64), mob -> mob.isAlive()
            && (mob instanceof HumanoidNpcEntity || mob instanceof BingwaEntity)
            && mob.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= 4096).size() < 3;
    }
}

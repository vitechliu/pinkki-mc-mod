package io.github.vitech.pinkkicore.forge.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.StructureTags;
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

public final class NiumoSpawns {
    private NiumoSpawns() {}

    public static void register(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.BULLMAN.get(), SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NiumoSpawns::bullman,
            SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.LAO_CAI.get(), SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NiumoSpawns::laoCai,
            SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    public static boolean bullman(EntityType<NiumoMob> type, ServerLevelAccessor level,
                                  MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason != MobSpawnType.NATURAL) return reason != MobSpawnType.CHUNK_GENERATION;
        return level.getLevel().isNight() && outdoors(level, pos)
            && net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(level, pos, random)
            && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
            && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    public static boolean laoCai(EntityType<NiumoMob> type, ServerLevelAccessor level,
                                  MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason != MobSpawnType.NATURAL) return reason != MobSpawnType.CHUNK_GENERATION;
        if (!outdoors(level, pos) || random.nextInt(20) != 0
            || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;

        var server = level.getLevel();
        boolean village = false;
        for (var structure : server.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getTagOrEmpty(StructureTags.VILLAGE)) {
            if (server.structureManager().getStructureAt(pos, structure.value()).isValid()) {
                village = true;
                break;
            }
        }
        return village && server.getEntitiesOfClass(NiumoMob.LaoCai.class, new AABB(pos).inflate(64),
            mob -> mob.isAlive() && mob.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= 4096).isEmpty();
    }

    private static boolean outdoors(ServerLevelAccessor level, BlockPos pos) {
        return level.getLevel().dimension() == Level.OVERWORLD && level.canSeeSky(pos);
    }
}

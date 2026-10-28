package pinkkicore.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.entity.HumanoidNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Dedicated-server checks of final biome spawn lists, placement predicates and real death loot. */
@Mod("pinkkicore_wildlife_probe")
public final class WildlifeProbe {
    private ServerLevel level;
    private ServerPlayer observer;
    private int ticks;
    private boolean done;
    private final Map<EntityType<?>, Integer> naturalCounts = new HashMap<>();
    private static final BlockPos SPAWN = new BlockPos(96, 101, 96);
    private static final AABB ARENA = new AABB(64, 95, 64, 160, 110, 160);
    public WildlifeProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static java.util.List<EntityType<? extends Mob>> types() {
        return java.util.List.of(ModEntities.PINKKI.get(), ModEntities.GUABA_YINGRONG.get(), ModEntities.BINGWA.get());
    }
    private boolean wildlife(EntityType<?> type) { return types().contains(type); }
    private void started(ServerStartedEvent event) {
        try {
            level = event.getServer().overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, event.getServer());
            level.setDayTime(6000); level.setWeatherParameters(10000, 0, false, false);
            level.setDefaultSpawnPos(new BlockPos(0, 101, 0), 0);
            for (int x=4;x<=9;x++) for (int z=4;z<=9;z++) level.setChunkForced(x,z,true);
            for (int x=64;x<160;x++) for(int z=64;z<160;z++) {
                level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState());
                for(int y=101;y<=104;y++) level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            observer = FakePlayerFactory.get(level,new GameProfile(UUID.fromString("23fd4fe1-2b03-45a8-9d85-f041403edb75"),"WildlifeProbe"));
            observer.setPos(128,101,128); level.addNewPlayer(observer);
            for (EntityType<?> type : types()) naturalCounts.put(type,0);
            checkBiomeLists(); checkLoot(); checkDeaths();
        } catch(Throwable error) { fail(error); }
    }
    private void checkBiomeLists() {
        var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        for(var key : java.util.List.of(Biomes.PLAINS,Biomes.FOREST,Biomes.TAIGA,Biomes.SWAMP)) {
            var settings=biomes.getHolderOrThrow(key).value().getMobSettings();
            for(var type : types()) {
                var entries=settings.getMobs(MobCategory.CREATURE).unwrap().stream().filter(data->data.type==type).toList();
                require(entries.size()==1,"missing/duplicate biome spawn " + key + " " + type);
                var entry=entries.get(0);
                require(entry.getWeight().asInt()==1 && entry.minCount==1 && entry.maxCount==1,"wrong spawn weight/group");
            }
        }
        for(var key : java.util.List.of(Biomes.NETHER_WASTES,Biomes.THE_END))
            require(biomes.getHolderOrThrow(key).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .noneMatch(data->wildlife(data.type)),"wildlife added outside overworld");
        for(var type : types()) {
            require(SpawnPlacements.getPlacementType(type)==SpawnPlacements.Type.ON_GROUND,"wrong placement");
            require(SpawnPlacements.getHeightmapType(type)==Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,"wrong heightmap");
        }
        System.out.println("WILDLIFE biome spawn lists + weights + solo packs + Nether/End exclusion PASS");
    }
    private void checkLoot() {
        for(var type : types()) {
            var mob=type.create(level);
            var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,mob)
                .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(SPAWN))
                .withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().genericKill()).create(LootContextParamSets.ENTITY);
            var table=level.getServer().getLootData().getLootTable(type.getDefaultLootTable());
            Map<String,Integer> frequencies=new HashMap<>();
            for(int i=1;i<=1000;i++) {
                var items=table.getRandomItems(params, i*1009L);
                require(items.size()==1,"death reward not exactly one material " + items);
                String item=ForgeRegistries.ITEMS.getKey(items.get(0).getItem()).toString();
                int max=item.equals("pinkkicore:guaba")?3:2;
                require(java.util.Set.of("pinkkicore:guaba","pinkkicore:thunder_guaba","pinkkicore:pink_ingot").contains(item),"wrong reward " + item);
                require(items.get(0).getCount()>=1 && items.get(0).getCount()<=max,"reward count out of range");
                frequencies.merge(item,1,Integer::sum);
            }
            require(frequencies.getOrDefault("pinkkicore:guaba",0)>600 && frequencies.getOrDefault("pinkkicore:guaba",0)<800,"Guaba weight incorrect");
            require(frequencies.getOrDefault("pinkkicore:thunder_guaba",0)>100 && frequencies.getOrDefault("pinkkicore:thunder_guaba",0)<300,"Thunder weight incorrect");
            require(frequencies.getOrDefault("pinkkicore:pink_ingot",0)>30 && frequencies.getOrDefault("pinkkicore:pink_ingot",0)<170,"Pink ingot weight incorrect");
            System.out.println("WILDLIFE loot " + ForgeRegistries.ENTITY_TYPES.getKey(type)+" " + frequencies + " PASS");
        }
    }
    private void checkDeaths() {
        AABB zone=new AABB(90,100,90,103,106,103);
        for(boolean drops : new boolean[]{true,false}) {
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(drops,level.getServer());
            for(var type : types()) {
                level.getEntitiesOfClass(ItemEntity.class,zone).forEach(Entity::discard);
                var mob=type.create(level);mob.setPos(96,101,96);mob.setNoAi(true);level.addFreshEntity(mob);
                if(mob instanceof HumanoidNpcEntity) require(!mob.equipItemIfPossible(new ItemStack(Items.DIAMOND_SWORD)).isEmpty(),"equipment setup failed");
                require(mob.hurt(level.damageSources().genericKill(),1000),"death rejected");
                var items=level.getEntitiesOfClass(ItemEntity.class,zone);
                var rewards=items.stream().filter(it->!it.getItem().is(Items.DIAMOND_SWORD)).toList();
                require(drops ? rewards.size()==1 && rewards.get(0).getItem().getCount()>=1 : rewards.isEmpty(),"death/doMobLoot failed " + type);
                if(drops && mob instanceof HumanoidNpcEntity) require(items.stream().anyMatch(it->it.getItem().is(Items.DIAMOND_SWORD)),"new material loot removed equipment drop");
                mob.discard();
            }
        }
        level.getEntitiesOfClass(ItemEntity.class,zone).forEach(Entity::discard);
        level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true,level.getServer());
        System.out.println("WILDLIFE actual deaths + no-player kills + equipment retained + doMobLoot PASS");
    }
    private void checkPlacement() {
        for(var type : types()) {
            int accepted=0;
            var random=RandomSource.create(876543);
            for(int i=0;i<2000;i++) if(SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,SPAWN,random)) accepted++;
            require(accepted>=50 && accepted<=150,"rare chance/valid grass failed count="+accepted+" brightness="+level.getRawBrightness(SPAWN,0));
            level.setBlockAndUpdate(SPAWN.below(),Blocks.STONE.defaultBlockState());
            for(int i=0;i<100;i++) require(!SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,SPAWN,random),"spawn on stone");
            level.setBlockAndUpdate(SPAWN.below(),Blocks.GRASS_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(SPAWN.above(3),Blocks.STONE.defaultBlockState());
            for(int i=0;i<100;i++) require(!SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,SPAWN,random),"spawn under roof");
            level.setBlockAndUpdate(SPAWN.above(3),Blocks.AIR.defaultBlockState());
            var crowd=new java.util.ArrayList<Mob>();
            for(var other : types()) {var mob=other.create(level);mob.setPos(100,101,100);mob.setNoAi(true);level.addFreshEntity(mob);crowd.add(mob);}
            for(int i=0;i<100;i++) require(!SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,SPAWN,random),"local density cap ignored");
            require(SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.SPAWN_EGG,SPAWN,random),"density cap blocks explicit spawn");
            crowd.forEach(Entity::discard);
            var nether=level.getServer().getLevel(Level.NETHER);var end=level.getServer().getLevel(Level.END);
            for(int i=0;i<100;i++) {
                require(!SpawnPlacements.checkSpawnRules(type,nether,MobSpawnType.NATURAL,SPAWN,random),"Nether placement accepted");
                require(!SpawnPlacements.checkSpawnRules(type,end,MobSpawnType.NATURAL,SPAWN,random),"End placement accepted");
            }
            System.out.println("WILDLIFE placement "+ForgeRegistries.ENTITY_TYPES.getKey(type)+" accepted="+accepted+"/2000 PASS");
        }
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||level==null||done)return;
        try {
            ticks++;
            if(ticks==30) { checkPlacement(); level.random.setSeed(987654321L); }
            if(ticks>30) {
                // Exercise vanilla's actual natural-spawn selection and insertion pipeline.
                for(int i=0;i<100;i++)NaturalSpawner.spawnCategoryForPosition(MobCategory.CREATURE,level,SPAWN);
                for(var mob:level.getEntitiesOfClass(Mob.class,ARENA)) {
                    if(wildlife(mob.getType())) {
                        require(mob.hasCustomName()&&mob.isCustomNameVisible(),"natural mob lost default name");
                        naturalCounts.merge(mob.getType(),1,Integer::sum);
                    }
                    mob.discard();
                }
                if(naturalCounts.values().stream().allMatch(n->n>0)) {
                    System.out.println("WILDLIFE vanilla natural spawning inserted all three types "+naturalCounts+" PASS");
                    level.removePlayerImmediately(observer,Entity.RemovalReason.DISCARDED);
                    System.out.println("WILDLIFE_PROBE PASS");done=true;
                }
            }
            if(ticks>=330)throw new AssertionError("natural-spawn pipeline did not insert all types: "+naturalCounts);
        }catch(Throwable error){fail(error);}
    }
    private void fail(Throwable error){done=true;System.out.println("WILDLIFE_PROBE FAIL "+error);error.printStackTrace();}
}

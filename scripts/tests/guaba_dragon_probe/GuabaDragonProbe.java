package pinkkicore.test;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.entity.GuabaDragonEntity;
import io.github.vitech.pinkkicore.forge.entity.ModEntities;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("pinkkicore_guaba_dragon_probe")
public final class GuabaDragonProbe {
    private ServerLevel level;
    private ServerPlayer player;
    private GuabaDragonEntity dragon;
    private Cow victim;
    private int ticks, activeTicks, damageEvents, lastRemaining;
    private float previousHealth;
    private boolean done, acquired, breathing, wallInserted;
    private GuabaDragonEntity wallDragon, farDragon, cancelDragon;
    private Cow wallVictim, farVictim, cancelVictim;
    public GuabaDragonProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private GuabaDragonEntity dragon(double x, double z) {
        var mob = ModEntities.GUABA_DRAGON.get().create(level);
        mob.setPos(x, 100, z); mob.setPersistenceRequired();
        mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        level.addFreshEntity(mob); return mob;
    }
    private Cow cow(double x, double z) {
        Cow cow = EntityType.COW.create(level); cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20); cow.setHealth(20); cow.setPos(x,100,z); cow.setNoAi(true);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        level.addFreshEntity(cow); return cow;
    }
    private void started(ServerStartedEvent event) {
        try {
            level=event.getServer().overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,event.getServer());
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,event.getServer());
            level.setDayTime(18000);
            level.setChunkForced(0,0,true); level.setChunkForced(1,0,true);
            for(int x=0;x<32;x++) for(int z=0;z<16;z++) {
                level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
                for(int y=100;y<107;y++) level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            checkBiomesAndPlacement();
            player=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("24b5b333-f2ef-4b89-94f5-3ea562a37271"),"DragonProbe"));
            player.setGameMode(GameType.SURVIVAL); player.setPos(20,100,8); level.addNewPlayer(player);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.GUABA_DRAGON_SPAWN_EGG.get()));
            var use=ModItems.GUABA_DRAGON_SPAWN_EGG.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(25.5,100,8.5),Direction.UP,new BlockPos(25,99,8),false)));
            require(use.consumesAction(),"egg rejected");
            var eggs=level.getEntitiesOfClass(GuabaDragonEntity.class,new AABB(24,99,7,28,104,11));
            require(eggs.size()==1,"egg did not create dragon");eggs.forEach(Entity::discard);
            dragon=dragon(8,8);
            require(dragon.hasCustomName() && dragon.isCustomNameVisible(),"default name missing/hidden");
            require(dragon.getMaxHealth()==30 && dragon.getAttributeValue(Attributes.ATTACK_DAMAGE)==4,"attributes wrong");
            require(dragon.fireImmune() && dragon.getType().getCategory()==MobCategory.MONSTER,"not fire immune monster");
            CompoundTag saved=dragon.saveWithoutId(new CompoundTag());
            saved.putString("CustomName",Component.Serializer.toJson(Component.literal("renamed-dragon")));
            saved.putBoolean("CustomNameVisible",false);
            var restored=ModEntities.GUABA_DRAGON.get().create(level);restored.load(saved);
            require(restored.getName().getString().equals("renamed-dragon") && !restored.isCustomNameVisible(),"name NBT lost");
            saved.remove("CustomName");saved.remove("CustomNameVisible");restored.load(saved);
            require(restored.hasCustomName() && restored.isCustomNameVisible() && restored.getBreathTicks()==0,"unnamed migration failed");
            // Kill a real entity; no custom loot pool and no inherited wildlife materials.
            var doomed=dragon(27,3);doomed.hurt(level.damageSources().genericKill(),1000);
            require(level.getEntitiesOfClass(ItemEntity.class,new AABB(25,99,1,30,104,6)).isEmpty(),"dragon dropped items");
            System.out.println("GUABA_DRAGON registry + spawn egg + default/custom names + NBT + attributes + actual empty death loot PASS");
        } catch(Throwable error) { fail(error); }
    }
    private void checkBiomesAndPlacement() {
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        for(var key: java.util.List.of(Biomes.NETHER_WASTES,Biomes.SOUL_SAND_VALLEY,Biomes.CRIMSON_FOREST,
                Biomes.WARPED_FOREST,Biomes.BASALT_DELTAS,Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS)) {
            var list=registry.getHolderOrThrow(key).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                .filter(s -> s.type==ModEntities.GUABA_DRAGON.get()).toList();
            require(list.size()==1 && list.get(0).getWeight().asInt()==6 && list.get(0).minCount==1 && list.get(0).maxCount==1,"biome spawn missing/duplicate/wrong "+key);
        }
        for(var key:java.util.List.of(Biomes.PLAINS,Biomes.DESERT,Biomes.THE_END))
            require(registry.getHolderOrThrow(key).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                .noneMatch(s -> s.type==ModEntities.GUABA_DRAGON.get()),"wrong biome "+key);
        var type=ModEntities.GUABA_DRAGON.get(); var pos=new BlockPos(8,100,8);var random=RandomSource.create(123);
        require(SpawnPlacements.getPlacementType(type)==SpawnPlacements.Type.ON_GROUND,"wrong placement type");
        var source=level.getServer().createCommandSourceStack();
        level.getServer().getCommands().performPrefixedCommand(source,"fillbiome 0 96 0 15 112 15 minecraft:badlands");
        require(level.getBiome(pos).is(net.minecraft.tags.BiomeTags.IS_BADLANDS),"fillbiome failed");
        // Roof guarantees darkness even before skylight updates settle.
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)level.setBlockAndUpdate(new BlockPos(x,106,z),Blocks.STONE.defaultBlockState());
        // The predicate is also verified after light engine updates during tick().
        var nether=level.getServer().getLevel(Level.NETHER);
        nether.setBlockAndUpdate(pos.below(),Blocks.NETHERRACK.defaultBlockState());
        nether.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        require(SpawnPlacements.checkSpawnRules(type,nether,MobSpawnType.NATURAL,pos,random),"Nether placement rejected");
        var end=level.getServer().getLevel(Level.END);
        require(!SpawnPlacements.checkSpawnRules(type,end,MobSpawnType.NATURAL,pos,random),"End accepted");
        require(!SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,new BlockPos(24,100,8),random),"plains accepted");
        System.out.println("GUABA_DRAGON eight biome spawn lists + exclusions + placement registration PASS");
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || level==null || done)return;
        try {
            ticks++;
            if(dragon.getTarget()==player)acquired=true;
            if(ticks==45) {
                require(acquired,"did not acquire survival player unprovoked");
                player.setGameMode(GameType.SPECTATOR);
                victim=cow(13,8);dragon.setTarget(victim);previousHealth=victim.getHealth();
                int accepts=0;
                for(int i=0;i<200;i++)if(SpawnPlacements.checkSpawnRules(ModEntities.GUABA_DRAGON.get(),level,MobSpawnType.NATURAL,new BlockPos(8,100,8),RandomSource.create(i)))accepts++;
                require(accepts>0,"dark badlands placement never accepted");
                System.out.println("GUABA_DRAGON unprovoked player targeting + dark badlands placement PASS");
            }
            if(ticks>45 && ticks<175 && victim!=null) {
                int remaining=dragon.getBreathTicks();
                if(remaining>0)breathing=true;
                // aiStep emitted for elapsed=(96-before), then decremented the synchronized value.
                if(remaining>=16 && remaining<76)activeTicks++;
                if(victim.getHealth()<previousHealth) {
                    require(previousHealth-victim.getHealth()<=2.01,"flame pulse too strong");damageEvents++;
                    previousHealth=victim.getHealth();
                }
                if(remaining>0)require(!dragon.doHurtTarget(victim),"melee overlapped breath");
                if(lastRemaining>0 && remaining==0) {
                    CompoundTag save=dragon.saveWithoutId(new CompoundTag());
                    require(save.getInt("GuabaBreathCooldown")>=150,"cooldown not applied");
                }
                lastRemaining=remaining;
            }
            if(ticks==175) {
                require(breathing && activeTicks==60,"spray duration wrong "+activeTicks);
                require(damageEvents==3 && victim.getHealth()==14,"flame pulse timeline wrong events="+damageEvents+" health="+victim.getHealth());
                var melee=dragon(22,3);var close=cow(23.5,3);
                require(melee.doHurtTarget(close) && close.getHealth()<20,"melee failed");melee.discard();close.discard();
                dragon.discard();victim.discard();
                wallDragon=dragon(3,3);wallVictim=cow(8,3);wallDragon.setTarget(wallVictim);
                farDragon=dragon(3,8);farVictim=cow(8,8);farDragon.setTarget(farVictim);
                cancelDragon=dragon(18,12);cancelVictim=cow(23,12);cancelDragon.setTarget(cancelVictim);
                System.out.println("GUABA_DRAGON melee + 60 active ticks + three 2-damage pulses + no overlapping melee + cooldown PASS");
            }
            if(ticks>175 && wallDragon!=null) {
                if(wallDragon.getBreathTicks()>80 && !wallInserted) {
                    wallInserted=true;
                    for(int y=100;y<105;y++)for(int z=1;z<=4;z++)level.setBlockAndUpdate(new BlockPos(5,y,z),Blocks.STONE.defaultBlockState());
                }
                if(farDragon.getBreathTicks()>80)farVictim.setPos(15,100,8);
                if(cancelDragon.getBreathTicks()>80) {
                    CompoundTag save=cancelDragon.saveWithoutId(new CompoundTag());
                    var reload=ModEntities.GUABA_DRAGON.get().create(level);reload.load(save);
                    require(reload.getBreathTicks()==0 && save.getInt("GuabaBreathCooldown")==160,"mid-attack reload resumed breath/cooldown lost");
                    cancelDragon.setNoAi(true);
                }
            }
            if(ticks==330) {
                require(wallInserted,"wall scenario never entered wind-up");
                require(wallVictim.getHealth()==20,"fire passed wall");
                require(farVictim.getHealth()==20,"fire exceeded six blocks");
                require(cancelVictim.getHealth()==20 && cancelDragon.getBreathTicks()==0,"NoAI did not cancel fire");
                require(level.getEntitiesOfClass(ItemEntity.class,new AABB(0,99,0,32,106,16)).isEmpty(),"unexpected items");
                // Peaceful rejection and normal hostile despawn behavior.
                level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
                require(!SpawnPlacements.checkSpawnRules(ModEntities.GUABA_DRAGON.get(),level,MobSpawnType.NATURAL,new BlockPos(8,100,8),RandomSource.create()),"peaceful spawn allowed");
                wallDragon.checkDespawn();require(wallDragon.isRemoved(),"monster remained in peaceful");
                System.out.println("GUABA_DRAGON wall occlusion + six-block limit + NoAI cancellation + mid-breath NBT reset + peaceful rejection PASS");
                System.out.println("GUABA_DRAGON_PROBE PASS");done=true;
            }
        }catch(Throwable error){fail(error);}
    }
    private void fail(Throwable error) { done=true;System.out.println("GUABA_DRAGON_PROBE FAIL "+error);error.printStackTrace(); }
}

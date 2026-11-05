package pinkkicore.test;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.core.MobFaction;
import io.github.vitech.pinkkicore.forge.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

/** Real server ticks, acquisition and damage; no client or direct goal invocation. */
@Mod("pinkkicore_faction_probe")
public final class FactionProbe {
    private ServerLevel level;
    private Mob pinkki, frog, dragon, blocked, neutralPinkki, neutralFrog, noAi;
    private GuabaYingrongEntity pinkkiTarget, frogTarget, dragonTarget, wallTarget, sameA, sameB, noAiTarget;
    private int ticks;
    private boolean done, pinkkiSeen, frogSeen, dragonSeen, blockedSeen;
    public FactionProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private <T extends Mob> T spawn(EntityType<T> type, double x, double z, boolean stationary) {
        T mob=type.create(level);mob.setPos(x,100,z);mob.setPersistenceRequired();
        if(stationary)mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        level.addFreshEntity(mob);return mob;
    }
    private GuabaYingrongEntity victim(double x, double z) {
        var mob=spawn(ModEntities.GUABA_YINGRONG.get(),x,z,true);
        mob.setNoAi(true);mob.setCustomName(Component.literal("Renamed rivalry target"));
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);
        mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);return mob;
    }
    private void wall(boolean present) {
        for(int y=100;y<106;y++)for(int z=28;z<37;z++)level.setBlockAndUpdate(new BlockPos(10,y,z),
            (present?Blocks.STONE:Blocks.AIR).defaultBlockState());
    }
    private void started(ServerStartedEvent event) {
        try {
            level=event.getServer().overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,event.getServer());
            for(int x=0;x<8;x++)for(int z=0;z<3;z++)level.setChunkForced(x,z,true);
            for(int x=0;x<128;x++)for(int z=0;z<40;z++) {
                level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
                for(int y=100;y<106;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            pinkki=spawn(ModEntities.PINKKI.get(),8,8,false);pinkkiTarget=victim(13,8);
            frog=spawn(ModEntities.BINGWA.get(),40,8,true);frogTarget=victim(41.6,8);
            dragon=spawn(ModEntities.GUABA_DRAGON.get(),72,8,true);dragonTarget=victim(77,8);
            blocked=spawn(ModEntities.PINKKI.get(),8,32,true);wallTarget=victim(12,32);wall(true);
            neutralPinkki=spawn(ModEntities.PINKKI.get(),112,8,true);
            neutralFrog=spawn(ModEntities.BINGWA.get(),114,8,true);
            sameA=spawn(ModEntities.GUABA_YINGRONG.get(),40,32,true);
            sameB=spawn(ModEntities.GUABA_YINGRONG.get(),42,32,true);
            noAi=spawn(ModEntities.PINKKI.get(),96,32,true);noAi.setNoAi(true);noAiTarget=victim(99,32);
            require(MobFactions.factionOf(pinkki)==MobFaction.NEUTRAL && MobFactions.factionOf(frog)==MobFaction.NEUTRAL,"neutral faction wrong");
            require(MobFactions.factionOf(dragon)==MobFaction.HOSTILE && MobFactions.factionOf(sameA)==MobFaction.SPECIAL,"hostile/special faction wrong");
            for(Mob mob:java.util.List.of(pinkki,frog,dragon)) {
                int before=mob.targetSelector.getAvailableGoals().size();
                MobFactions.onJoin(new EntityJoinLevelEvent(mob,level));
                MobFactions.onJoin(new EntityJoinLevelEvent(mob,level));
                require(mob.targetSelector.getAvailableGoals().size()==before,"duplicate goals on rejoin");
                require(mob.targetSelector.getAvailableGoals().stream().filter(g->g.getPriority()==0).count()==1,"rivalry priority missing");
            }
            var cow=spawn(EntityType.COW,112,12,true);
            var creeper=EntityType.CREEPER.create(level);creeper.setNoAi(true);creeper.setPos(112,100,14);
            int before=creeper.targetSelector.getAvailableGoals().size();level.addFreshEntity(creeper);
            require(!MobFactions.isPinkkiCoreMob(cow) && !MobFactions.isPinkkiCoreMob(creeper),"vanilla included in factions");
            require(creeper.targetSelector.getAvailableGoals().size()==before && cow.targetSelector.getAvailableGoals().isEmpty(),"vanilla AI modified");creeper.discard();
            var nearbyPlayer=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("677d9683-3cb4-4c74-aa34-6e0bbfa32dd1"),"FactionPlayer"));
            nearbyPlayer.setGameMode(GameType.SURVIVAL);nearbyPlayer.setPos(72,100,12);level.addNewPlayer(nearbyPlayer);
            dragon.setTarget(nearbyPlayer);
            var neutralPlayer=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("677d9683-3cb4-4c74-aa34-6e0bbfa32dd2"),"NeutralPlayer"));
            neutralPlayer.setGameMode(GameType.SURVIVAL);neutralPlayer.setPos(112,100,10);level.addNewPlayer(neutralPlayer);
            var attacker=spawn(EntityType.COW,8,11,true);attacker.setNoAi(true);
            pinkki.hurt(level.damageSources().mobAttack(attacker),1);
            System.out.println("FACTION classification + exact namespace + repeat join deduplication + target priority registration PASS");
        }catch(Throwable error){fail(error);}
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || level==null || done)return;
        try {
            ticks++;
            pinkkiSeen |= pinkki.getTarget()==pinkkiTarget;
            frogSeen |= frog.getTarget()==frogTarget;
            dragonSeen |= dragon.getTarget()==dragonTarget;
            blockedSeen |= blocked.getTarget()==wallTarget;
            if(ticks==75) {
                require(pinkkiSeen && frogSeen && dragonSeen,"missing automatic rivalry acquisition");
                require(dragon.getTarget()==dragonTarget,"player acquisition overrode rivalry");
                require(pinkki.getTarget()==pinkkiTarget,"retaliation overrode rivalry");
                require(!blockedSeen,"rival acquired through solid wall");
                require(neutralPinkki.getTarget()==null && neutralFrog.getTarget()==null,"neutral mobs attacked players/each other");
                require(sameA.getTarget()==null && sameB.getTarget()==null,"Yingrong attacked same type");
                require(noAi.getTarget()==null && noAiTarget.getHealth()==200,"NoAI mob attacked");
                wall(false);blocked.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.25);
                System.out.println("FACTION automatic targeting + renamed identity + player/retaliation preemption + neutral/same-type/NoAI exclusions + wall rejection PASS");
            }
            if(ticks==150) {
                require(pinkkiTarget.getHealth()<200 && frogTarget.getHealth()<200 && dragonTarget.getHealth()<200,"acquired rival but did not damage it: "+pinkkiTarget.getHealth()+","+frogTarget.getHealth()+","+dragonTarget.getHealth());
                require(blockedSeen && wallTarget.getHealth()<200,"did not attack after wall removed");
                CompoundTag saved=pinkki.saveWithoutId(new CompoundTag());pinkki.discard();
                var restored=ModEntities.PINKKI.get().create(level);restored.load(saved);level.addFreshEntity(restored);pinkki=restored;
                pinkkiSeen=false;
                System.out.println("FACTION real NPC melee + frog delayed tongue + dragon flame damage + wall removal acquisition PASS");
            }
            if(ticks==210) {
                require(pinkkiSeen && pinkki.getTarget()==pinkkiTarget,"loaded entity did not regain rivalry");
                require(neutralPinkki.getTarget()==null && neutralFrog.getTarget()==null,"neutral behavior regressed");
                System.out.println("FACTION save/reload reattachment PASS");
                System.out.println("FACTION_PROBE PASS");done=true;
            }
        }catch(Throwable error){fail(error);}
    }
    private void fail(Throwable error){done=true;System.out.println("FACTION_PROBE FAIL "+error);error.printStackTrace();}
}

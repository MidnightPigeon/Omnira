package com.mcmagic.omnira.mire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Vanilla aquatic spawning is sea-level bounded; upper-stratum ponds need a local pass. */
@EventBusSubscriber(modid="omnira")
public final class MireEelSpawning {
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%400!=0||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        for(var player:level.players()){
            if(player.isSpectator()||level.getEntitiesOfClass(TimeflowEel.class,player.getBoundingBox().inflate(80)).size()>=6)continue;
            for(int i=0;i<24;i++){
                int x=player.getBlockX()+level.random.nextInt(129)-64,z=player.getBlockZ()+level.random.nextInt(129)-64;
                var pos=new BlockPos(x,player.getBlockY(),z);
                if(!level.hasChunkAt(pos))continue;
                pos=new BlockPos(x,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z)-2,z);
                if(!level.hasChunkAt(pos)||!(level.getBiome(pos).is(MireCycle.BIOME)||level.getBiome(pos).is(com.mcmagic.omnira.time.RecurrenceGarden.BIOME))
                        ||!MireContent.fluid(level.getFluidState(pos))||!MireContent.fluid(level.getFluidState(pos.above())))continue;
                if(level.getNearestPlayer(pos.getX()+.5,pos.getY(),pos.getZ()+.5,24,false)!=null||level.getEntitiesOfClass(TimeflowEel.class,new net.minecraft.world.phys.AABB(pos).inflate(16)).size()>=2)continue;
                var eel=MireContent.EEL.get().create(level);if(eel==null)break;
                eel.moveTo(pos.getX()+.5,pos.getY()+.2,pos.getZ()+.5,level.random.nextFloat()*360,0);
                if(level.noCollision(eel)){eel.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null);level.addFreshEntity(eel);}break;
            }
        }
    }
}

package com.mcmagic.omnira.space.sea;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid="omnira")
public final class VoidSeaSpawning {
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%400!=0||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        for(var player:level.players()){
            if(player.isSpectator())continue;
            boolean manta=level.random.nextInt(5)==0;
            var vicinity=player.getBoundingBox().inflate(96);
            if(manta?level.getEntitiesOfClass(VoidManta.class,vicinity).size()>=2:level.getEntitiesOfClass(StarJellyfish.class,vicinity).size()>=8)continue;
            for(int i=0;i<32;i++){
                BlockPos pos=player.blockPosition().offset(level.random.nextInt(129)-64,level.random.nextInt(65)-32,level.random.nextInt(129)-64);
                if(!level.hasChunkAt(pos)||!level.getBiome(pos).is(VoidSeaContent.BIOME)||!VoidSeaContent.fluid(level.getFluidState(pos))
                        ||!VoidSeaContent.fluid(level.getFluidState(pos.above()))||level.getNearestPlayer(pos.getX()+.5,pos.getY(),pos.getZ()+.5,24,false)!=null)continue;
                StarSeaCreature mob=manta?VoidSeaContent.MANTA.get().create(level):VoidSeaContent.JELLYFISH.get().create(level);if(mob==null)break;
                mob.moveTo(pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5,level.random.nextFloat()*360,0);
                if(level.noCollision(mob)){mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null);level.addFreshEntity(mob);}break;
            }
        }
    }
    private VoidSeaSpawning(){}
}

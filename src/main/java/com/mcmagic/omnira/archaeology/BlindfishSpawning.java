package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.mire.MireContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid="omnira")
public final class BlindfishSpawning {
    public static boolean habitat(ServerLevel level,BlockPos pos){
        return level.hasChunkAt(pos)&&level.getBiome(pos).is(net.minecraft.resources.ResourceLocation.parse("omnira:epochal_cliffs"))
                &&MireContent.fluid(level.getFluidState(pos))&&MireContent.fluid(level.getFluidState(pos.below()));
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%400!=0||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        for(var player:level.players()){
            if(player.isSpectator()||level.getEntitiesOfClass(ChronalBlindfish.class,player.getBoundingBox().inflate(80)).size()>=6)continue;
            for(int i=0;i<24;i++){
                var pos=EpochalAquifers.center(Math.floorDiv(player.getBlockX(),32)+level.random.nextInt(5)-2,
                        Math.floorDiv(player.getBlockZ(),32)+level.random.nextInt(5)-2,level.random.nextInt(5));
                if(pos==null||!habitat(level,pos)||level.getNearestPlayer(pos.getX()+.5,pos.getY(),pos.getZ()+.5,24,false)!=null
                        ||level.getEntitiesOfClass(ChronalBlindfish.class,new AABB(pos).inflate(16)).size()>=2)continue;
                var fish=ArchaeologyContent.BLINDFISH.get().create(level);if(fish==null)break;
                fish.moveTo(pos.getX()+.5,pos.getY()-.4,pos.getZ()+.5,level.random.nextFloat()*360,0);
                if(level.noCollision(fish)){fish.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null);level.addFreshEntity(fish);}break;
            }
        }
    }
    private BlindfishSpawning(){}
}

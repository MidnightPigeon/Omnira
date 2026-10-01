package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.TimeNatureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class TemporalBloom {
    public static boolean spread(ServerLevel level,BlockPos soil){
        if(!TimePlantBlock.canGrowAt(level,soil.above()))return false;
        boolean placed=false;
        for(int i=0;i<96;i++){
            var pos=soil.above();
            for(int step=0;step<i/12;step++)pos=pos.offset(level.random.nextInt(3)-1,level.random.nextInt(3)-1,level.random.nextInt(3)-1);
            if(!level.isLoaded(pos)||!TimePlantBlock.canGrowAt(level,pos))continue;
            level.setBlockAndUpdate(pos,(level.random.nextInt(5)==0?TimeNatureContent.FLOWER:TimeNatureContent.GRASS).get().defaultBlockState());placed=true;
        }
        if(placed)level.levelEvent(1505,soil.above(),0);
        return placed;
    }
    private TemporalBloom(){}
}

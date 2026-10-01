package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class FleetingVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public FleetingVegetationFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var level=c.level();var random=c.random();var origin=c.origin();boolean placed=false;
        for(int i=0;i<12;i++){
            int x=origin.getX()+random.nextInt(16),z=origin.getZ()+random.nextInt(16);
            var pos=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
            if(!level.getBiome(pos).is(FleetingTime.BIOME)||!TimePlantBlock.canGrowAt(level,pos))continue;
            if(i<7)placed|=DreamContent.TIME_TREE_FEATURE.get().place(new FeaturePlaceContext<>(java.util.Optional.empty(),level,c.chunkGenerator(),random,pos,NoneFeatureConfiguration.INSTANCE));
            else if(level.isEmptyBlock(pos)){level.setBlock(pos,(random.nextInt(4)==0?TimeNatureContent.FLOWER:TimeNatureContent.GRASS).get().defaultBlockState(),3);placed=true;}
        }
        return placed;
    }
}

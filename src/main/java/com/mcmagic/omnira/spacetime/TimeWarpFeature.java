package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.Blocks;

public final class TimeWarpFeature extends Feature<NoneFeatureConfiguration> {
    public TimeWarpFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var level=context.level();var top=context.origin();var random=context.random();
        if(!silt(level.getBlockState(top.below())))return false;
        if(random.nextInt(8)!=0){
            var pos=top.above(1+random.nextInt(2));
            if(!level.getBlockState(pos).isAir())return false;
            return level.setBlock(pos,ModBlocks.TIME_WARP_POINT.get().defaultBlockState(),2);
        }
        var center=new BlockPos(top.getX(),Math.max(195,top.getY()-9-random.nextInt(9)),top.getZ());
        final int radius=4;
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
            int distance=x*x+y*y+z*z;if(distance>radius*radius)continue;
            if(!silt(level.getBlockState(center.offset(x,y,z))))return false;
        }
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
            int distance=x*x+y*y+z*z;if(distance>radius*radius)continue;
            var pos=center.offset(x,y,z);
            if(distance>=12 && random.nextInt(9)==0)
                level.setBlock(pos,DreamContent.SOLIDIFIED_LIGHT_CRYSTAL_CORE.get().defaultBlockState(),2);
            else level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        level.setBlock(center,ModBlocks.TIME_WARP_POINT.get().defaultBlockState(),2);
        return true;
    }
    private static boolean silt(net.minecraft.world.level.block.state.BlockState state){
        return state.is(DreamContent.TEMPORAL_SILT.get()) || state.is(DreamContent.LIVING_TEMPORAL_SILT.get());
    }
}

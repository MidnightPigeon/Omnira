package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.TimeNatureContent;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.LinkedHashMap;

public final class TimeTreeFeature extends Feature<NoneFeatureConfiguration> {
    public TimeTreeFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var level=context.level();var root=context.origin();var random=context.random();
        if(!TemporalSoils.timeTree(level.getBlockState(root.below())))return false;
        int height=9+random.nextInt(5);
        if(root.getY()+height+3>=level.getMaxBuildHeight())return false;
        var shape=new LinkedHashMap<BlockPos,BlockState>();
        var log=TimeNatureContent.LOG.get().defaultBlockState();
        var leaves=TimeNatureContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,1);
        for(int y=0;y<height;y++)shape.put(root.above(y),log);
        // High, offset branch crowns leave a clear trunk silhouette like a eucalyptus.
        for(int n=0;n<4;n++){
            var direction=Direction.from2DDataValue(n);int y=height-4+(n%2);
            for(int d=1;d<=2;d++)shape.put(root.above(y).relative(direction,d),log.setValue(RotatedPillarBlock.AXIS,direction.getAxis()));
            crown(shape,root.above(y+1).relative(direction,2),leaves,2);
        }
        crown(shape,root.above(height),leaves,3);
        for(var at:shape.keySet())if(!replaceable(level,at))return false;
        shape.forEach((pos,state)->level.setBlock(pos,state,3));
        if(level.getBiome(root).is(com.mcmagic.omnira.mire.MireCycle.BIOME)){
            var server=level.getLevel();var positions=java.util.List.copyOf(shape.keySet());var savedRoot=root.immutable();
            server.getServer().execute(()->com.mcmagic.omnira.mire.MireTrees.get(server).register(savedRoot,positions));
        }
        return true;
    }
    private static void crown(LinkedHashMap<BlockPos,BlockState> shape,BlockPos center,BlockState leaves,int radius){
        for(int y=-1;y<=1;y++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            int r=radius-Math.abs(y);if(x*x+z*z>r*r+1)continue;
            shape.putIfAbsent(center.offset(x,y,z),leaves);
        }
    }
    private static boolean replaceable(WorldGenLevel level,BlockPos pos){var state=level.getBlockState(pos);return state.isAir()||state.is(BlockTags.REPLACEABLE_BY_TREES)||state.is(BlockTags.SAPLINGS)||state.is(BlockTags.LEAVES);}
}

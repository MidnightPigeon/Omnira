package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class SpatialMatrixBlock extends AmethystBlock {
    public static final MapCodec<SpatialMatrixBlock> CODEC=simpleCodec(SpatialMatrixBlock::new);
    public SpatialMatrixBlock(Properties p){super(p);}
    @Override public MapCodec<? extends AmethystBlock> codec(){return CODEC;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(random.nextInt(5)==0)grow(level,pos,Direction.values()[random.nextInt(6)]);}
    public static boolean grow(ServerLevel level,BlockPos pos,Direction face){
        var at=pos.relative(face);var old=level.getBlockState(at);Block next=null;
        if(BuddingAmethystBlock.canClusterGrowAtState(old))next=TimeNatureContent.SMALL_SPATIAL_BUD.get();
        else if(old.hasProperty(AmethystClusterBlock.FACING)&&old.getValue(AmethystClusterBlock.FACING)==face){
            if(old.is(TimeNatureContent.SMALL_SPATIAL_BUD.get()))next=TimeNatureContent.MEDIUM_SPATIAL_BUD.get();
            else if(old.is(TimeNatureContent.MEDIUM_SPATIAL_BUD.get()))next=TimeNatureContent.LARGE_SPATIAL_BUD.get();
            else if(old.is(TimeNatureContent.LARGE_SPATIAL_BUD.get()))next=TimeNatureContent.SPATIAL_CLUSTER.get();
        }
        return next!=null&&level.setBlockAndUpdate(at,next.defaultBlockState().setValue(AmethystClusterBlock.FACING,face).setValue(AmethystClusterBlock.WATERLOGGED,old.getFluidState().getType()==Fluids.WATER));
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){NatureParticles.space(level,pos,random);}
}

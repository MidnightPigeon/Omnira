package com.mcmagic.omnira.space.geodes;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class DeflectionMatrixBlock extends AmethystBlock {
    public static final MapCodec<DeflectionMatrixBlock> CODEC=simpleCodec(DeflectionMatrixBlock::new);
    public DeflectionMatrixBlock(Properties properties){super(properties);}
    @Override public MapCodec<? extends AmethystBlock> codec(){return CODEC;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(random.nextInt(5)==0)grow(level,pos,Direction.values()[random.nextInt(6)]);
    }
    public static boolean grow(ServerLevel level,BlockPos pos,Direction face){
        var at=pos.relative(face);var old=level.getBlockState(at);Block next=null;
        if(BuddingAmethystBlock.canClusterGrowAtState(old))next=DeflectionContent.SMALL.get();
        else if(old.hasProperty(AmethystClusterBlock.FACING)&&old.getValue(AmethystClusterBlock.FACING)==face){
            var stages=DeflectionContent.stages();for(int i=0;i<3;i++)if(old.is(stages[i]))next=stages[i+1];
        }
        return next!=null&&level.setBlockAndUpdate(at,next.defaultBlockState().setValue(AmethystClusterBlock.FACING,face)
                .setValue(AmethystClusterBlock.WATERLOGGED,old.getFluidState().is(Fluids.WATER)));
    }
}

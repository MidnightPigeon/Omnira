package com.mcmagic.omnira.block;

import com.mcmagic.omnira.registry.DreamContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class DreamCrystalMatrixBlock extends AmethystBlock {
    public static final MapCodec<DreamCrystalMatrixBlock> CODEC=simpleCodec(DreamCrystalMatrixBlock::new);
    public DreamCrystalMatrixBlock(Properties properties) {super(properties);}
    @Override public MapCodec<? extends AmethystBlock> codec() {return CODEC;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(random.nextInt(5)==0) grow(level,pos,Direction.values()[random.nextInt(6)]);
    }
    public static boolean grow(ServerLevel level,BlockPos pos,Direction face) {
        var at=pos.relative(face);var old=level.getBlockState(at);
        Block next=null;
        if(BuddingAmethystBlock.canClusterGrowAtState(old)) next=DreamContent.SMALL_DREAM_BUD.get();
        else if(old.hasProperty(AmethystClusterBlock.FACING) && old.getValue(AmethystClusterBlock.FACING)==face) {
            if(old.is(DreamContent.SMALL_DREAM_BUD.get())) next=DreamContent.MEDIUM_DREAM_BUD.get();
            else if(old.is(DreamContent.MEDIUM_DREAM_BUD.get())) next=DreamContent.LARGE_DREAM_BUD.get();
            else if(old.is(DreamContent.LARGE_DREAM_BUD.get())) next=DreamContent.DREAM_CRYSTAL.get();
        }
        return next!=null && level.setBlockAndUpdate(at,next.defaultBlockState().setValue(AmethystClusterBlock.FACING,face)
                .setValue(AmethystClusterBlock.WATERLOGGED,old.getFluidState().getType()==Fluids.WATER));
    }
}

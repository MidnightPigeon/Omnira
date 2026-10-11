package com.mcmagic.omnira.spacetime;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ExcitedSpatialCrystalBlock extends TransparentBlock {

    public ExcitedSpatialCrystalBlock(BlockBehaviour.Properties properties){super(properties);}

    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(random.nextInt(4)==0)SpatialCrystalMotes.observe.accept(level,pos);
    }
}

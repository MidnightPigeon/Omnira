package com.mcmagic.omnira.time;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SpatialBudBlock extends AmethystClusterBlock {
    public SpatialBudBlock(int height,int inset,Properties p){super(height,inset,p);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){NatureParticles.space(level,pos,random);}
}

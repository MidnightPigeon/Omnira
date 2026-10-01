package com.mcmagic.omnira.time;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TimeLeavesBlock extends LeavesBlock {
    public TimeLeavesBlock(Properties p){super(p);}
    @Override public boolean isRandomlyTicking(BlockState state){return false;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        // Animated inset leaf veins provide continuous internal flow without falling particles.
    }
}

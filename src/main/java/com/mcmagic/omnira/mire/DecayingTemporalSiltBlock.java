package com.mcmagic.omnira.mire;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class DecayingTemporalSiltBlock extends Block {
    public static final int DECAY_TICKS=6000;
    public DecayingTemporalSiltBlock(Properties p){super(p);}
    public static boolean touching(Level level,BlockPos pos){
        for(var d:Direction.values())if(level.hasChunkAt(pos.relative(d))&&MireContent.fluid(level.getFluidState(pos.relative(d))))return true;
        return false;
    }
    private void start(Level level,BlockPos pos){
        if(level instanceof ServerLevel server&&!server.getBiome(pos).is(com.mcmagic.omnira.time.RecurrenceGarden.BIOME)
                &&!server.getBiome(pos).is(com.mcmagic.omnira.time.EventideRuins.BIOME)&&touching(level,pos)
                &&!server.getBlockTicks().hasScheduledTick(pos,this))server.scheduleTick(pos,this,DECAY_TICKS);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);if(!old.is(this))start(level,pos);}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block from,BlockPos other,boolean moving){super.neighborChanged(state,level,pos,from,other,moving);start(level,pos);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){start(level,pos);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!level.getBiome(pos).is(com.mcmagic.omnira.time.RecurrenceGarden.BIOME)
                &&!level.getBiome(pos).is(com.mcmagic.omnira.time.EventideRuins.BIOME)&&touching(level,pos))
            level.setBlockAndUpdate(pos,MireContent.SILT.get().defaultBlockState());
    }
}

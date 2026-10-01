package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.TimeNatureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** One persisted scheduled tick per aging block; no per-block entity or per-tick scan. */
public final class TemporalSiltBlock extends Block {
    public static final int SEDIMENT_TICKS=6000;
    public static final ResourceKey<Biome> FROZEN_TERRA=StilledTime.BIOME;
    private final boolean living;
    public TemporalSiltBlock(boolean living,Properties properties){super(properties);this.living=living;}
    private void begin(ServerLevel level,BlockPos pos){
        if(!StilledTime.stopped(level,pos)&&!level.getBiome(pos).is(RecurrenceGarden.BIOME)
                &&!level.getBiome(pos).is(EventideRuins.BIOME)&&!level.getBlockTicks().hasScheduledTick(pos,this))
            level.scheduleTick(pos,this,SEDIMENT_TICKS);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!old.is(this)&&level instanceof ServerLevel server)begin(server,pos);
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        // Starts timers for natural/legacy terrain without resetting an existing timer.
        begin(level,pos);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!StilledTime.stopped(level,pos)&&!level.getBiome(pos).is(RecurrenceGarden.BIOME)
                &&!level.getBiome(pos).is(EventideRuins.BIOME))
            level.setBlockAndUpdate(pos,(living?TimeNatureContent.LIVING_SILT:TimeNatureContent.SILT).get().defaultBlockState());
    }
}

package com.mcmagic.omnira.archaeology;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Era is part of the block state, so treasure provenance survives chunk serialization. */
public final class TimeSandBlock extends BrushableBlock {
    public static final IntegerProperty ERA=IntegerProperty.create("era",0,4);
    public TimeSandBlock(Properties p){super(ArchaeologyContent.SAND.get(),SoundEvents.BRUSH_SAND,SoundEvents.BRUSH_SAND_COMPLETED,p);registerDefaultState(defaultBlockState().setValue(ERA,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ERA);}
}

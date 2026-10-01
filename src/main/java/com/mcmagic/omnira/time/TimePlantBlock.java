package com.mcmagic.omnira.time;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TimePlantBlock extends BushBlock implements net.minecraft.world.level.block.EntityBlock {
    private final boolean flower;
    public TimePlantBlock(boolean flower,Properties p){super(p);this.flower=flower;}
    @Override public MapCodec<? extends BushBlock> codec(){return simpleCodec(p->new TimePlantBlock(flower,p));}
    @Override protected boolean mayPlaceOn(BlockState soil,BlockGetter level,BlockPos pos){return soil.is(TemporalSoils.SILTS)&&soil.getFluidState().isEmpty();}
    public static boolean canGrowAt(LevelReader level,BlockPos pos){
        return level.isEmptyBlock(pos)&&level.getFluidState(pos).isEmpty()
                &&level.getBlockState(pos.below()).is(TemporalSoils.SILTS)&&level.getFluidState(pos.below()).isEmpty();
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TimePlantBlockEntity(pos,state);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){}
}

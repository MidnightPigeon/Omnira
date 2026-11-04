package com.mcmagic.omnira.time;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TimePlantBlock extends BushBlock implements net.minecraft.world.level.block.EntityBlock {
    private static final net.minecraft.world.phys.shapes.VoxelShape GRASS_SHAPE=box(1.5,0,3.5,13,11,14);
    private static final net.minecraft.world.phys.shapes.VoxelShape FLOWER_SHAPE=box(3,0,3,13,12,13);
    private final boolean flower;
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return flower?FLOWER_SHAPE:GRASS_SHAPE;}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.empty();}
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

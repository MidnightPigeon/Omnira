package com.mcmagic.omnira.space.phase;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class PhaseClusterBlock extends Block {
    private static final VoxelShape SHAPE=Block.box(2,0,2,14,14,14);
    public PhaseClusterBlock(Properties p){super(p);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        PhaseParticles.around(level,pos,random);
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public boolean canSurvive(BlockState s,LevelReader level,BlockPos pos){return !level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).isEmpty();}
    @Override public BlockState updateShape(BlockState s,Direction face,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos other){
        return face==Direction.DOWN&&!s.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():s;
    }
}

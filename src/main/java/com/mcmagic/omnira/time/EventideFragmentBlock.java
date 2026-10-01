package com.mcmagic.omnira.time;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class EventideFragmentBlock extends HorizontalDirectionalBlock {
    private static final MapCodec<EventideFragmentBlock> PILLAR_CODEC=simpleCodec(p->new EventideFragmentBlock(false,p));
    private static final MapCodec<EventideFragmentBlock> DEBRIS_CODEC=simpleCodec(p->new EventideFragmentBlock(true,p));
    private static final VoxelShape PILLAR=box(4,0,4,12,12,12);
    private static final VoxelShape DEBRIS=box(1,0,1,15,3,15);
    private final boolean debris;
    public EventideFragmentBlock(boolean debris,Properties properties){
        super(properties);this.debris=debris;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return debris?DEBRIS_CODEC:PILLAR_CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return debris?DEBRIS:PILLAR;
    }
}

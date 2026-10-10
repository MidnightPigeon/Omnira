package com.mcmagic.omnira.space.phase;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

public final class PhaseBlock extends TransparentBlock {
    public static final BooleanProperty INVERTED=BooleanProperty.create("inverted");
    public static final BooleanProperty TRANSITIONING=BooleanProperty.create("transitioning");
    public static final IntegerProperty PHASE=IntegerProperty.create("phase",0,16);
    public PhaseBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(INVERTED,false).setValue(TRANSITIONING,false).setValue(PHASE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(INVERTED,TRANSITIONING,PHASE);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        PhaseParticles.inside(level,pos,random,state.getValue(PHASE)/16F);
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(INVERTED)?Shapes.empty():Shapes.block();
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        if(!level.isClientSide)PhaseNetworks.schedule(level,pos);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){PhaseNetworks.tick(level,pos);}
}

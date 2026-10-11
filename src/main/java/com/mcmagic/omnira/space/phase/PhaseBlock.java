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
    public static final BooleanProperty TARGET=BooleanProperty.create("target_inverted");
    public static final IntegerProperty PHASE=IntegerProperty.create("phase",0,16);
    public PhaseBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(INVERTED,false).setValue(TRANSITIONING,false).setValue(TARGET,false).setValue(PHASE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(INVERTED,TRANSITIONING,TARGET,PHASE);}
    @Override public boolean skipRendering(BlockState state,BlockState adjacent,Direction side){return adjacent.is(this)||super.skipRendering(state,adjacent,side);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        PhaseParticles.inside(level,pos,random,state.getValue(PHASE)/16F);
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(INVERTED)?Shapes.empty():Shapes.block();
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        if(!level.isClientSide){if(!old.is(this))PhaseNetworks.invalidate(level);PhaseNetworks.schedule(level,pos);}
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!next.is(this)&&!level.isClientSide){PhaseNetworks.invalidate(level);for(var face:Direction.values())PhaseNetworks.schedule(level,pos.relative(face));}
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){PhaseNetworks.tick(level,pos);}
}

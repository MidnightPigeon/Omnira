package com.mcmagic.omnira.time;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

public final class TimeSaplingBlock extends SaplingBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty REWINDING=net.minecraft.world.level.block.state.properties.BooleanProperty.create("rewinding");
    private static final TreeGrower GROWER=new TreeGrower("omnira:time",Optional.empty(),Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("omnira","time_tree"))),Optional.empty());
    public static final MapCodec<TimeSaplingBlock> CODEC=simpleCodec(TimeSaplingBlock::new);
    public TimeSaplingBlock(Properties p){super(GROWER,p);registerDefaultState(defaultBlockState().setValue(REWINDING,false));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(REWINDING);}
    @Override public MapCodec<? extends SaplingBlock> codec(){return CODEC;}
    @Override protected boolean mayPlaceOn(BlockState soil,BlockGetter level,BlockPos pos){return TemporalSoils.timeTree(soil);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(state.getValue(REWINDING))return;
        if(level.getBlockState(pos.below()).is(TemporalSoils.SILTS)||random.nextBoolean())super.randomTick(state,level,pos,random);
    }
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state){return !state.getValue(REWINDING)&&(level.getBlockState(pos.below()).is(TemporalSoils.SILTS)||random.nextBoolean())&&super.isBonemealSuccess(level,random,pos,state);}
    @Override public void advanceTree(ServerLevel level,BlockPos pos,BlockState state,RandomSource random){if(!state.getValue(REWINDING))super.advanceTree(level,pos,state,random);}
}

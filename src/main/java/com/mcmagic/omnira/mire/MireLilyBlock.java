package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.time.TimePlantBlockEntity;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.*;

public final class MireLilyBlock extends BushBlock implements EntityBlock {
    public static final BooleanProperty NATURAL=BooleanProperty.create("natural");
    public final boolean reborn;
    public MireLilyBlock(boolean reborn){super(Properties.ofFullCopy(Blocks.LILY_PAD).noOcclusion());this.reborn=reborn;registerDefaultState(stateDefinition.any().setValue(NATURAL,!reborn));}
    @Override protected com.mojang.serialization.MapCodec<? extends BushBlock> codec(){return simpleCodec(p->new MireLilyBlock(reborn));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(NATURAL);}
    @Override protected boolean mayPlaceOn(BlockState soil,BlockGetter level,BlockPos pos){var fluid=level.getFluidState(pos);return fluid.isSource()&&(MireContent.fluid(fluid)||fluid.is(FluidTags.WATER));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(1,0,1,15,reborn?5:1,15);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TimePlantBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide||!state.getValue(NATURAL))return null;
        return (l,p,s,be)->{if(l.getGameTime()%20==0)updateNatural(l,p,s);};
    }
    public static void updateNatural(Level level,BlockPos pos,BlockState state){
        updateNatural(level,pos,state,level.getGameTime());
    }
    public static void updateNatural(Level level,BlockPos pos,BlockState state,long time){
        if(!(state.getBlock() instanceof MireLilyBlock lily)||!state.getValue(NATURAL)||!level.getBiome(pos).is(MireCycle.BIOME))return;
        boolean active=MireCycle.reversing(time);
        if(active!=lily.reborn)level.setBlockAndUpdate(pos,(active?MireContent.REBORN:MireContent.DECAYED).get().defaultBlockState().setValue(NATURAL,true));
    }
}

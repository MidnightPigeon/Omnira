package com.mcmagic.omnira.space;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class SpatialRemnantBlock extends BaseEntityBlock {
    public static final MapCodec<SpatialRemnantBlock> CODEC=simpleCodec(SpatialRemnantBlock::new);
    public SpatialRemnantBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SpatialRemnantBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,com.mcmagic.omnira.registry.ModBlockEntityTypes.SPATIAL_REMNANT.get(),
                (world,pos,current,entity)->entity.tick());
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(6,6,6,10,10,10);}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){SpaceWarpBlock.particles(l,p,r,true);}
}

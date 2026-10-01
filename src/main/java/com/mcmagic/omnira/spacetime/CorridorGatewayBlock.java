package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class CorridorGatewayBlock extends BaseEntityBlock {
    public static final MapCodec<CorridorGatewayBlock> CODEC=simpleCodec(CorridorGatewayBlock::new);
    public CorridorGatewayBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CorridorGatewayBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return type==ModBlockEntityTypes.CORRIDOR_GATEWAY.get()?(l,p,s,e)->((CorridorGatewayBlockEntity)e).tick():null;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CorridorGatewayBlockEntity gateway)gateway.release();
        super.onRemove(state,level,pos,next,moving);
    }
}

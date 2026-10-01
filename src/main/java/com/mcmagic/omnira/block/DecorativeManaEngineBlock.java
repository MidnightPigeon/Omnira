package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class DecorativeManaEngineBlock extends BaseEntityBlock {
    public DecorativeManaEngineBlock(Properties properties) {
        super(properties);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {return simpleCodec(DecorativeManaEngineBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(BlockStateProperties.HORIZONTAL_FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,context.getHorizontalDirection());}
    @Override protected BlockState rotate(BlockState state,Rotation rotation) {return state.setValue(BlockStateProperties.HORIZONTAL_FACING,rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror) {return rotate(state,mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new DecorativeManaEngineBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return type==ModBlockEntityTypes.MANA_ENGINE.get()?(world,pos,block,entity)->((DecorativeManaEngineBlockEntity)entity).tick():null;
    }
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ManaEngineAccess engine) server.openMenu(engine,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ManaEngineAccess engine)
            Containers.dropContents(level,pos,engine.engineState().inventory);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return Shapes.or(box(2,0,2,14,12,14),box(1,12,1,15,14,15),
                state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()==Direction.Axis.Z?box(6,6,0,10,10,16):box(0,6,6,16,10,10));
    }
}

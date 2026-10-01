package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.ManaEngineBlockEntity;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ManaEngineBlock extends HorizontalKineticBlock implements IBE<ManaEngineBlockEntity> {
    public ManaEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING,Direction.NORTH));
    }
    @Override public Class<ManaEngineBlockEntity> getBlockEntityClass() {return ManaEngineBlockEntity.class;}
    @Override public BlockEntityType<? extends ManaEngineBlockEntity> getBlockEntityType() {return (BlockEntityType)ModBlockEntityTypes.MANA_ENGINE.get();}
    @Override public Direction.Axis getRotationAxis(BlockState state) {return state.getValue(HORIZONTAL_FACING).getAxis();}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face) {
        return face==state.getValue(HORIZONTAL_FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING,context.getHorizontalDirection());
    }
    @Override public BlockState getRotatedBlockState(BlockState state,Direction targetedFace) {
        return state.setValue(HORIZONTAL_FACING,state.getValue(HORIZONTAL_FACING).getClockWise());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player.getMainHandItem().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","wrench")))) return InteractionResult.PASS;
        if(level.getBlockEntity(pos) instanceof ManaEngineBlockEntity engine) {
            if(!level.isClientSide && player instanceof ServerPlayer server) {
                // The small rear control reverses rotation; the other surfaces open the menu.
                double y=hit.getLocation().y-pos.getY();
                double x=state.getValue(HORIZONTAL_FACING).getAxis()==Direction.Axis.Z
                        ?hit.getLocation().x-pos.getX():hit.getLocation().z-pos.getZ();
                if(hit.getDirection()==state.getValue(HORIZONTAL_FACING).getOpposite()
                        && y>=.375 && y<=.625 && x>=.375 && x<=.625) engine.engineState().control(2);
                else server.openMenu(engine,pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ManaEngineBlockEntity engine)
            Containers.dropContents(level,pos,engine.engineState().inventory);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return Shapes.or(box(2,0,2,14,12,14),box(1,12,1,15,14,15),
                state.getValue(HORIZONTAL_FACING).getAxis()==Direction.Axis.Z?box(6,6,0,10,10,16):box(0,6,6,16,10,10));
    }
}

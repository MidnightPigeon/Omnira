package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.KineticAssemblyBlockEntity;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class KineticAssemblyBlock extends HorizontalKineticBlock implements IBE<KineticAssemblyBlockEntity> {
    public KineticAssemblyBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING,Direction.NORTH));
    }
    @Override public Class<KineticAssemblyBlockEntity> getBlockEntityClass() {return KineticAssemblyBlockEntity.class;}
    @Override public BlockEntityType<? extends KineticAssemblyBlockEntity> getBlockEntityType() {
        return (BlockEntityType)ModBlockEntityTypes.ARCANE_ASSEMBLY_TABLE.get();
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {return state.getValue(HORIZONTAL_FACING).getAxis();}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face) {
        return face==state.getValue(HORIZONTAL_FACING).getOpposite();
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState getRotatedBlockState(BlockState state,Direction face) {
        return state.setValue(HORIZONTAL_FACING,state.getValue(HORIZONTAL_FACING).getClockWise());
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return AssemblyGeometry.shape(state.getValue(HORIZONTAL_FACING));
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player.getMainHandItem().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","wrench")))) return InteractionResult.PASS;
        if(level.getBlockEntity(pos) instanceof KineticAssemblyBlockEntity host) {
            if(!level.isClientSide && !player.isSpectator()) {
                var work=host.assembly();
                if(work.finished()) {
                    work.collect(player);
                } else player.openMenu(work,pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof KineticAssemblyBlockEntity host)
            Containers.dropContents(level,pos,host.assembly());
        super.onRemove(state,level,pos,next,moving);
    }
}

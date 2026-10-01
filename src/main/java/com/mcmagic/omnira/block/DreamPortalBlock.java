package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.DreamPortalBlockEntity;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class DreamPortalBlock extends BaseEntityBlock {
    public static final MapCodec<DreamPortalBlock> CODEC=simpleCodec(DreamPortalBlock::new);
    public DreamPortalBlock(Properties properties) {super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new DreamPortalBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return Shapes.empty();}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return type==ModBlockEntityTypes.DREAM_PORTAL.get()?(l,p,s,entity)->((DreamPortalBlockEntity)entity).tick():null;
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof DreamPortalBlockEntity portal) portal.enter(entity);
    }
}

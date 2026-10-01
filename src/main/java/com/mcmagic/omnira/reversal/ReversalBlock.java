package com.mcmagic.omnira.reversal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class ReversalBlock extends com.mcmagic.omnira.block.CrystalProcessingTableBlock {
    public ReversalBlock(Properties p){super(p);}
    @Override protected com.mojang.serialization.MapCodec<? extends com.mcmagic.omnira.block.CrystalProcessingTableBlock> codec(){return simpleCodec(ReversalBlock::new);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ReversalBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return Block.box(1,0,1,15,16,15);}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return player.isShiftKeyDown()?net.minecraft.world.InteractionResult.PASS:super.useWithoutItem(s,l,p,player,hit);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,ReversalContent.ENTITY.get(),(world,pos,state,be)->be.tick());}
}

package com.mcmagic.omnira.shop;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class MarisaOrbBlock extends BaseEntityBlock {
    public MarisaOrbBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT,net.minecraft.core.Direction.UP));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> b){b.add(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT,c.getClickedFace());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT,r.rotate(s.getValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.setValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT,m.mirror(s.getValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT)));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(MarisaOrbBlock::new);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MarisaOrbBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    private static final java.util.Map<net.minecraft.core.Direction,VoxelShape> SHAPES=new java.util.EnumMap<>(net.minecraft.core.Direction.class);
    static{for(var face:net.minecraft.core.Direction.values())SHAPES.put(face,com.mcmagic.omnira.block.OrbMount.shape(face,box(1,0,1,15,15,15)));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES.get(s.getValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT));}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:(l,p,s,be)->{if(be instanceof MarisaOrbBlockEntity orb)orb.tick();};
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player.isSecondaryUseActive())return InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof MarisaOrbBlockEntity orb)orb.open(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MarisaOrbBlockEntity orb)orb.close();
        super.onRemove(state,level,pos,next,moving);
    }
}

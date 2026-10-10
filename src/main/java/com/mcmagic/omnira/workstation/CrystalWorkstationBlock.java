package com.mcmagic.omnira.workstation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public class CrystalWorkstationBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public final int index;
    public CrystalWorkstationBlock(Properties p,int index){super(p);this.index=index;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new CrystalWorkstationBlock(p,index));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation rotation){return s.setValue(FACING,rotation.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror mirror){return s.rotate(mirror.getRotation(s.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return CrystalWorkstations.entity(p,s);}
    @Override protected MenuProvider getMenuProvider(BlockState s,Level l,BlockPos p){return CrystalWorkstations.work(l.getBlockEntity(p));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,15,15);}
    public boolean shaft(BlockState state,Direction side){return index>=3&&index!=4&&(side==state.getValue(FACING).getOpposite()||index==5&&side==state.getValue(FACING));}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
        if(l.isClientSide||t!=CrystalWorkstations.ENTITY.get())return null;
        return (level,pos,state,be)->{var work=CrystalWorkstations.work(be);if(work!=null)work.tickDirect();};
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        var work=CrystalWorkstations.work(l.getBlockEntity(p));
        if(work==null)return InteractionResult.PASS;
        if(!l.isClientSide)player.openMenu(work,p);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())){var work=CrystalWorkstations.work(l.getBlockEntity(p));if(work!=null)Containers.dropContents(l,p,work);}
        super.onRemove(s,l,p,next,moving);
    }
}

package com.mcmagic.omnira.fate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class SuitcaseBlock extends BaseEntityBlock {
    public static final MapCodec<SuitcaseBlock> CODEC=simpleCodec(SuitcaseBlock::new);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public SuitcaseBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING).getAxis()==Direction.Axis.Z?box(2,0,0,14,11,16):box(0,0,2,16,11,14);}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos other){return d==Direction.DOWN&&!s.canSurvive(l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,d,n,l,p,other);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SuitcaseEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
        return createTickerHelper(t,com.mcmagic.omnira.registry.ModBlockEntityTypes.SUITCASE.get(),(world,pos,state,be)->{
            if(world.isClientSide)com.mcmagic.omnira.client.SuitcaseClient.tick(be);else be.tickPerformance();
        });
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.isEmpty())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!l.isClientSide&&l.getBlockEntity(p) instanceof SuitcaseEntity be)be.insert(stack,player.getAbilities().instabuild);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!l.isClientSide&&l.getBlockEntity(p) instanceof SuitcaseEntity be){
            var stack=be.removeDisplay();if(!stack.isEmpty()&&!player.getInventory().add(stack))player.drop(stack,false);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof SuitcaseEntity be&&!l.isClientSide)Block.popResource(l,p,be.removeDisplay());
        super.onRemove(s,l,p,next,moving);
    }
}

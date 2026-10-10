package com.mcmagic.omnira.space;

import com.mcmagic.omnira.registry.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** A two-block doorway; only its lower half owns the link. */
public final class FoldGateBlock extends BaseEntityBlock {
    public static final MapCodec<FoldGateBlock> CODEC=simpleCodec(FoldGateBlock::new);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public FoldGateBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(HALF,DoubleBlockHalf.LOWER));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,HALF);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    public static BlockPos base(BlockState state,BlockPos pos){return state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos;}
    public static boolean stable(BlockState s){return s.is(ModBlocks.STABLE_FOLD_GATE.get());}
    public static boolean active(BlockState s){return stable(s)||s.is(ModBlocks.UNSTABLE_FOLD_GATE.get());}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return c.getClickedPos().getY()+1<c.getLevel().getMaxBuildHeight()&&c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c)?defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()):null;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){l.setBlock(p.above(),s.setValue(HALF,DoubleBlockHalf.UPPER),3);}
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide&&s.getValue(HALF)==DoubleBlockHalf.UPPER&&l.getBlockState(p.below()).is(this))l.destroyBlock(p.below(),!player.isCreative(),player);
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q){
        boolean upper=s.getValue(HALF)==DoubleBlockHalf.UPPER;
        if(d==(upper?Direction.DOWN:Direction.UP)&&(!(other.getBlock() instanceof FoldGateBlock)||other.getValue(HALF)==s.getValue(HALF)))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,d,other,l,p,q);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        var frame=frameShape(s);
        if(!active(s))return frame;
        return Shapes.or(frame,s.getValue(FACING).getAxis()==Direction.Axis.X?box(7,0,2,9,16,14):box(2,0,7,14,16,9));
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return frameShape(s);}
    private static VoxelShape frameShape(BlockState s){
        boolean x=s.getValue(FACING).getAxis()==Direction.Axis.X;
        var rails=x?Shapes.or(box(5,0,0,11,16,2),box(5,0,14,11,16,16)):Shapes.or(box(0,0,5,2,16,11),box(14,0,5,16,16,11));
        return s.getValue(HALF)==DoubleBlockHalf.UPPER?Shapes.or(rails,x?box(5,14,2,11,16,14):box(2,14,5,14,16,11)):rails;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!state.is(ModBlocks.FOLD_GATE_FRAME.get()))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Block next=stack.is(ModItems.SPACE_WARP_POINT.get())?ModBlocks.STABLE_FOLD_GATE.get():stack.is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get())?ModBlocks.UNSTABLE_FOLD_GATE.get():null;
        if(next==null)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var base=base(state,pos);
        if(!player.mayBuild()||!world.mayInteract(player,base)||!world.mayInteract(player,base.above()))return ItemInteractionResult.FAIL;
        if(!world.getBlockState(base.above()).is(this))return ItemInteractionResult.FAIL;
        if(!world.isClientSide){
            var replacement=next.defaultBlockState().setValue(FACING,state.getValue(FACING));
            world.setBlock(base,replacement,2);world.setBlock(base.above(),replacement.setValue(HALF,DoubleBlockHalf.UPPER),3);
            world.updateNeighborsAt(base,next);if(!player.isCreative())stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(HALF)==DoubleBlockHalf.LOWER?new FoldGateBlockEntity(p,s):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==ModBlockEntityTypes.FOLD_GATE.get()?(a,b,c,e)->((FoldGateBlockEntity)e).tick():null;}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(s.getBlock()!=next.getBlock()&&l.getBlockEntity(p) instanceof FoldGateBlockEntity gate)gate.unlink();
        if(!l.isClientSide&&!(next.getBlock() instanceof FoldGateBlock)&&s.getValue(HALF)==DoubleBlockHalf.UPPER&&l.getBlockState(p.below()).is(this))l.destroyBlock(p.below(),!moving);
        super.onRemove(s,l,p,next,moving);
    }
}

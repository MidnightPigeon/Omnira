package com.mcmagic.omnira.block;

import com.mcmagic.omnira.throne.GoldenToilet;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GoldenToiletBlock extends Block {
    public static final MapCodec<GoldenToiletBlock> CODEC=simpleCodec(GoldenToiletBlock::new);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER_SHAPE=box(2,0,1,14,16,16);
    private static final VoxelShape UPPER_SHAPE=box(2,0,11,14,9,16);
    public GoldenToiletBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any()
            .setValue(FACING,net.minecraft.core.Direction.NORTH).setValue(HALF,DoubleBlockHalf.LOWER));}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,HALF);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        BlockPos pos=context.getClickedPos();
        return pos.getY()<context.getLevel().getMaxBuildHeight()-1 && context.getLevel().getBlockState(pos.above()).canBeReplaced(context)
                ?defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        level.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
    }
    @Override protected BlockState updateShape(BlockState state,net.minecraft.core.Direction direction,BlockState adjacent,
                                               LevelAccessor level,BlockPos pos,BlockPos other){
        boolean lower=state.getValue(HALF)==DoubleBlockHalf.LOWER;
        if(direction==(lower?net.minecraft.core.Direction.UP:net.minecraft.core.Direction.DOWN)
                && (!adjacent.is(this)||adjacent.getValue(HALF)==state.getValue(HALF)))return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,direction,adjacent,level,pos,other);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide && player.isCreative() && state.getValue(HALF)==DoubleBlockHalf.UPPER
                && level.getBlockState(pos.below()).is(this))level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),35);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(HALF)==DoubleBlockHalf.LOWER?LOWER_SHAPE:UPPER_SHAPE;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player.isSpectator() || player.isShiftKeyDown())return InteractionResult.PASS;
        if(!level.isClientSide)GoldenToilet.sit(player,state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
                                                        Player player,InteractionHand hand,BlockHitResult hit){
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}

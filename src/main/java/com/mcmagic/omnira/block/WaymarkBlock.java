package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.WaymarkBlockEntity;
import com.mcmagic.omnira.menu.WaymarkMenu;
import com.mcmagic.omnira.travel.WaymarkDirectory;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class WaymarkBlock extends BaseEntityBlock {
    public static final MapCodec<WaymarkBlock> CODEC=simpleCodec(WaymarkBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public WaymarkBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(HALF);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER?new WaymarkBlockEntity(pos,state):null;
    }
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(HALF)==DoubleBlockHalf.UPPER?box(4,0,4,12,14,12):
                Shapes.or(box(1,0,3,15,5,13),box(4,5,4,12,16,12));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var pos=context.getClickedPos();
        return pos.getY()<context.getLevel().getMaxBuildHeight()-1 && context.getLevel().getBlockState(pos.above()).canBeReplaced(context)
                && defaultBlockState().canSurvive(context.getLevel(),pos)?defaultBlockState():null;
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        return state.getValue(HALF)==DoubleBlockHalf.UPPER?level.getBlockState(pos.below()).is(this):
                level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        level.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
        if(level.getBlockEntity(pos) instanceof WaymarkBlockEntity mark) {
            if(placer!=null) mark.placedByPlayer();
            mark.register();
            if(placer instanceof ServerPlayer player) WaymarkMenu.openEdit(player,mark);
        }
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos other) {
        var half=state.getValue(HALF);
        if(direction==(half==DoubleBlockHalf.LOWER?Direction.UP:Direction.DOWN))
            return adjacent.is(this) && adjacent.getValue(HALF)!=half?state:Blocks.AIR.defaultBlockState();
        if(half==DoubleBlockHalf.LOWER && direction==Direction.DOWN && !state.canSurvive(level,pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,direction,adjacent,level,pos,other);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide && player.isCreative() && state.getValue(HALF)==DoubleBlockHalf.UPPER && level.getBlockState(pos.below()).is(this))
            level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),35);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof WaymarkBlockEntity mark)
            WaymarkDirectory.get(server).remove(mark.id());
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        var base=state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos;
        if(player instanceof ServerPlayer server && !player.isSpectator() && level.getBlockEntity(base) instanceof WaymarkBlockEntity mark) {
            mark.discover(server);
            WaymarkMenu.openEdit(server,mark);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}

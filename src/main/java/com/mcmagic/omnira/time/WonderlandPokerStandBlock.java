package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Two-block display; only the lower half owns state and the loading ticket. */
public final class WonderlandPokerStandBlock extends BaseEntityBlock {
    public static final MapCodec<WonderlandPokerStandBlock> CODEC=simpleCodec(WonderlandPokerStandBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public WonderlandPokerStandBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HALF);}
    @Override protected RenderShape getRenderShape(BlockState state){return state.getValue(HALF)==DoubleBlockHalf.LOWER?RenderShape.MODEL:RenderShape.INVISIBLE;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        BlockPos pos=context.getClickedPos();
        return pos.getY()<context.getLevel().getMaxBuildHeight()-1&&context.getLevel().getBlockState(pos.above()).canBeReplaced(context)
                ?defaultBlockState():null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        level.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos other){
        boolean lower=state.getValue(HALF)==DoubleBlockHalf.LOWER;
        if(direction==(lower?Direction.UP:Direction.DOWN)&&(!adjacent.is(this)||adjacent.getValue(HALF)==state.getValue(HALF)))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,direction,adjacent,level,pos,other);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()&&state.getValue(HALF)==DoubleBlockHalf.UPPER
                &&level.getBlockState(pos.below()).is(this))level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),35);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&state.getValue(HALF)==DoubleBlockHalf.LOWER
                &&level.getBlockEntity(pos) instanceof WonderlandPokerStandBlockEntity stand)stand.releaseForcedChunk();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(HALF)==DoubleBlockHalf.LOWER?box(0,0,0,16,13,16):box(4,0,4,12,14,12);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(HALF)==DoubleBlockHalf.LOWER?new WonderlandPokerStandBlockEntity(pos,state):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide||state.getValue(HALF)==DoubleBlockHalf.UPPER?null:createTickerHelper(type,ModBlockEntityTypes.WONDERLAND_POKER_STAND.get(),(world,pos,s,stand)->stand.tick());
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        return state.getValue(HALF)==DoubleBlockHalf.LOWER?java.util.List.of(new ItemStack(asItem())):java.util.List.of();
    }
}

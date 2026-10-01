package com.mcmagic.omnira.aggregation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class AggregationRingBlock extends BaseEntityBlock implements com.mcmagic.omnira.item.bottle.BottleMultiblock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,8);
    public static final MapCodec<AggregationRingBlock> CODEC=simpleCodec(AggregationRingBlock::new);
    public AggregationRingBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,4));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(PART)==4?new AggregationRingBlockEntity(pos,state):null;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(FACING).getAxis()==Direction.Axis.Z?box(0,0,4,16,16,12):box(4,0,0,12,16,16);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return AggregationCollision.shape(state.getValue(PART),state.getValue(FACING));
    }
    @Override public BlockPos bottleOrigin(BlockPos pos,BlockState state){return AggregationLayout.center(pos,state);}
    @Override public java.util.Set<BlockPos> bottleParts(Level level,BlockPos pos,BlockState state){return AggregationStructure.parts(level,bottleOrigin(pos,state),state);}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        var result=super.playerWillDestroy(level,pos,state,player);if(player.isCreative())AggregationStructure.remove(level,pos,state);return result;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()))AggregationStructure.remove(level,pos,state);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return interact(state,level,pos,player,hand,hit)?ItemInteractionResult.sidedSuccess(level.isClientSide):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit)?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.PASS;
    }
    private boolean interact(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isShiftKeyDown()||player.isSpectator())return false;
        var center=AggregationLayout.center(pos,state);
        if(!(level.getBlockEntity(center) instanceof AggregationRingBlockEntity machine))return false;
        var local=AggregationLayout.local(hit.getLocation(),center,state.getValue(FACING));
        double time=level.getGameTime(),bob=AggregationLayout.bob(time,machine.powered());
        int slot=AggregationLayout.node(local,bob);
        if(AggregationLayout.button(local,bob)){if(!level.isClientSide)machine.pressButton(player);return true;}
        if(slot<0&&machine.getItem(AggregationLayout.OUTPUT).isEmpty())for(int orb=0;orb<2;orb++){
            var at=AggregationLayout.orb(time,orb,machine.powered());
            if(Math.hypot(local.x-at.x,local.y-bob-at.y)<.2){slot=AggregationLayout.SUBSTRATE+orb;break;}
        }
        if(!level.isClientSide){
            if(slot>=0)machine.interactSlot(player,hand,slot);
            else if(player.getItemInHand(hand).isEmpty()&&Math.hypot(local.x,local.y-bob)<.3&&machine.collect(player)){}
            else player.openMenu(machine,center);
        }
        return true;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,AggregationContent.ENTITY.get(),(l,p,s,m)->m.tick());
    }
}

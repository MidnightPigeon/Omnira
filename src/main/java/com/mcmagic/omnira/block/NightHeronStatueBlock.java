package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.NightHeronStatueBlockEntity;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
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

public final class NightHeronStatueBlock extends BaseEntityBlock {
    public static final MapCodec<NightHeronStatueBlock> CODEC=simpleCodec(NightHeronStatueBlock::new);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED,STANDING=BooleanProperty.create("standing");
    public NightHeronStatueBlock(Properties properties) {
        super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(HALF,DoubleBlockHalf.LOWER).setValue(POWERED,false).setValue(STANDING,true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    /** Right as seen by a player facing the front button, not by the block facing outward. */
    public static Direction viewerRight(Direction front){return front.getCounterClockWise();}
    public static float modelYaw(Direction front){return -front.toYRot();}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,HALF,POWERED,STANDING);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(HALF)==DoubleBlockHalf.LOWER?new NightHeronStatueBlockEntity(p,s):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t) {
        return l.isClientSide?null:createTickerHelper(t,ModBlockEntityTypes.NIGHT_HERON_STATUE.get(),(level,pos,state,statue)->statue.tick());
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {
        return s.getValue(HALF)==DoubleBlockHalf.UPPER?box(2,0,3,14,8,13):Shapes.or(box(0,0,0,16,8,16),box(3,8,3,13,16,13));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        var p=c.getClickedPos();return p.getY()<c.getLevel().getMaxBuildHeight()-1 && c.getLevel().getBlockState(p.above()).canBeReplaced(c)
                ?defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()):null;
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack) {
        l.setBlock(p.above(),s.setValue(HALF,DoubleBlockHalf.UPPER),3);
        signal(l,p);
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState adjacent,LevelAccessor l,BlockPos p,BlockPos other) {
        boolean lower=s.getValue(HALF)==DoubleBlockHalf.LOWER;
        if(d==(lower?Direction.UP:Direction.DOWN)) {
            if(!adjacent.is(this) || adjacent.getValue(HALF)==s.getValue(HALF))return Blocks.AIR.defaultBlockState();
            if(!lower)return adjacent.setValue(HALF,DoubleBlockHalf.UPPER);
        }
        return super.updateShape(s,d,adjacent,l,p,other);
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player) {
        if(!l.isClientSide && player.isCreative() && s.getValue(HALF)==DoubleBlockHalf.UPPER && l.getBlockState(p.below()).is(this))l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),35);
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block neighbor,BlockPos from,boolean moving){signal(l,s.getValue(HALF)==DoubleBlockHalf.UPPER?p.below():p);}
    public static void signal(Level l,BlockPos p) {
        if(l.isClientSide || !(l.getBlockEntity(p) instanceof NightHeronStatueBlockEntity statue))return;
        var state=l.getBlockState(p);boolean powered=l.hasNeighborSignal(p)||l.hasNeighborSignal(p.above());
        if(powered==state.getValue(POWERED))return;
        var next=state.setValue(POWERED,powered);
        if(powered)next=next.cycle(STANDING);
        l.setBlock(p,next,3);
        if(powered)statue.call();
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit) {
        if(player.isSpectator())return InteractionResult.PASS;
        if(s.getValue(HALF)==DoubleBlockHalf.UPPER || hit.getLocation().y-p.getY()>.5) {
            BlockPos base=s.getValue(HALF)==DoubleBlockHalf.UPPER?p.below():p;
            if(!l.isClientSide && l.getBlockEntity(base) instanceof NightHeronStatueBlockEntity statue) {
                l.setBlock(base,l.getBlockState(base).cycle(STANDING),3);statue.call();
            }
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
        boolean front=hit.getDirection()==s.getValue(FACING),back=hit.getDirection()==s.getValue(FACING).getOpposite();
        if(s.getValue(HALF)!=DoubleBlockHalf.LOWER || (!front && !back) || hit.getLocation().y-p.getY()>.5)return InteractionResult.PASS;
        var right=s.getValue(FACING).getClockWise();var local=hit.getLocation().subtract(p.getX()+.5,p.getY(),p.getZ()+.5);
        double offset=local.x*right.getStepX()+local.z*right.getStepZ();
        if(Math.abs(offset)>.125 || local.y<.125 || local.y>.3125)return InteractionResult.PASS;
        if(!l.isClientSide && l.getBlockEntity(p) instanceof NightHeronStatueBlockEntity statue) {
            if(front)statue.cycleColor();
            else {
                statue.toggleAutoMilk();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.heron_milk."+(statue.autoMilk()?"enabled":"disabled")),true);
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override protected BlockState rotate(BlockState s,Rotation rotation){return s.setValue(FACING,rotation.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror mirror){return rotate(s,mirror.getRotation(s.getValue(FACING)));}
}

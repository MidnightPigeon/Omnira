package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
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

public final class CatalysisBlock extends BaseEntityBlock implements com.mcmagic.omnira.item.bottle.BottleMultiblock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,2);
    public CatalysisBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,0));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(CatalysisBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,PART);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected boolean isRandomlyTicking(BlockState state){return state.getValue(PART)==0;}
    @Override protected void randomTick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        var be=controller(level,pos);if(be!=null)be.naturalTick(random);
    }
    public static BlockPos master(BlockPos pos,BlockState state){return pos.below(state.getValue(PART));}
    public static CatalysisBlockEntity controller(Level level,BlockPos pos){
        if(level==null||!level.hasChunkAt(pos))return null;var state=level.getBlockState(pos);
        return state.is(CatalysisContent.BLOCK.get())&&level.getBlockEntity(master(pos,state)) instanceof CatalysisBlockEntity be?be:null;
    }
    public static boolean place(Level level,BlockPos pos,Direction facing){
        var snapshots=new java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();
        for(int i=0;i<3;i++)snapshots.add(net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,pos.above(i)));
        for(int i=0;i<3;i++)if(!level.setBlock(pos.above(i),CatalysisContent.BLOCK.get().defaultBlockState().setValue(FACING,facing).setValue(PART,i),18)){
            snapshots.forEach(s->s.restore(18));return false;
        }
        var be=controller(level,pos);if(be==null||!be.complete()){snapshots.forEach(s->s.restore(18));return false;}
        for(int i=0;i<3;i++)level.updateNeighborsAt(pos.above(i),CatalysisContent.BLOCK.get());return true;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CatalysisBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return !level.isClientSide&&state.getValue(PART)==0&&type==CatalysisContent.ENTITY.get()?(l,p,s,be)->((CatalysisBlockEntity)be).tick():null;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(PART)==0?box(0,0,0,16,16,16):box(1,0,1,15,state.getValue(PART)==2?9.6:16,15);
    }
    @Override protected MenuProvider getMenuProvider(BlockState state,Level level,BlockPos pos){return controller(level,pos);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var be=controller(level,pos);if(be==null)return ItemInteractionResult.FAIL;
        if(stack.is(ModItems.RESONANCE_CORE.get())&&state.getValue(PART)==2&&hit.getDirection()==Direction.UP){
            var at=hit.getLocation().with(net.minecraft.core.Direction.Axis.Y,pos.getY()+1);
            return stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,new BlockHitResult(at,Direction.UP,pos,false))).consumesAction()
                    ?ItemInteractionResult.SUCCESS:ItemInteractionResult.FAIL;
        }
        if(state.getValue(PART)==0&&hit.getDirection()==state.getValue(FACING).getOpposite()
                &&net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()){
            if(!level.isClientSide){
                var inventory=new net.neoforged.neoforge.items.wrapper.InvWrapper(player.getInventory());
                var result=net.neoforged.neoforge.fluids.FluidUtil.tryEmptyContainerAndStow(stack,be.tank,inventory,Integer.MAX_VALUE,player,true);
                if(result.isSuccess())player.setItemInHand(hand,result.getResult());
                else if(net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(stack).isEmpty())
                    net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,be.tank);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(state.getValue(PART)!=0)return InteractionResult.PASS;var be=controller(level,pos);if(be==null)return InteractionResult.FAIL;
        if(!level.isClientSide)player.openMenu(be,be.getBlockPos());return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()){
            var base=master(pos,state);var be=controller(level,pos);if(be!=null)be.dropContents();
            for(int i=0;i<3;i++)if(level.getBlockState(base.above(i)).is(this)){level.removeBlockEntity(base.above(i));level.setBlock(base.above(i),Blocks.AIR.defaultBlockState(),18);}
        }return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public BlockPos bottleOrigin(BlockPos pos,BlockState state){return master(pos,state);}
    @Override public java.util.Set<BlockPos> bottleParts(Level level,BlockPos pos,BlockState state){
        var be=controller(level,pos);if(be==null||!be.complete())return java.util.Set.of();var p=be.getBlockPos();return java.util.Set.of(p,p.above(),p.above(2));
    }
    public static final class PlacementItem extends BlockItem {
        public PlacementItem(Properties properties){super(CatalysisContent.BLOCK.get(),properties);}
        @Override public InteractionResult place(BlockPlaceContext context){
            var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
            if(player==null||!player.isCreative()||!context.canPlace())return InteractionResult.FAIL;
            for(int i=0;i<3;i++){var p=pos.above(i);if(!level.hasChunkAt(p)||level.isOutsideBuildHeight(p)||!level.getWorldBorder().isWithinBounds(p)
                    ||!level.mayInteract(player,p)||!player.mayUseItemAt(p,context.getClickedFace(),context.getItemInHand())
                    ||!level.getBlockState(p).canBeReplaced()||level.getBlockEntity(p)!=null
                    ||!level.isUnobstructed(getBlock().defaultBlockState().setValue(PART,i),p,CollisionContext.empty()))return InteractionResult.FAIL;}
            return level.isClientSide?InteractionResult.SUCCESS:CatalysisBlock.place(level,pos,context.getHorizontalDirection().getOpposite())?InteractionResult.CONSUME:InteractionResult.FAIL;
        }
    }
}

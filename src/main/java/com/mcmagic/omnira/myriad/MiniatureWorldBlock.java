package com.mcmagic.omnira.myriad;

import java.util.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class MiniatureWorldBlock extends BaseEntityBlock {
    public static final int SIZE=5,PARTS=125;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,342);
    // Missing property in the old 7-cube save resolves to false; picking it up safely converts it.
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty COMPACT=net.minecraft.world.level.block.state.properties.BooleanProperty.create("compact");
    private static final ThreadLocal<Boolean> CHANGING=ThreadLocal.withInitial(()->false);
    public MiniatureWorldBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(PART,0).setValue(COMPACT,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(MiniatureWorldBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PART,COMPACT);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    public static BlockPos offset(int part){return offset(part,SIZE);}
    public static BlockPos offset(int part,int size){return new BlockPos(part%size,part/(size*size),part/size%size);}
    public static int size(BlockState state){return state.getValue(COMPACT)?5:7;}
    public static int lightLevel(BlockState state){
        int side=size(state);var p=offset(state.getValue(PART),side);
        return p.getX()==0||p.getY()==0||p.getZ()==0||p.getX()==side-1||p.getY()==side-1||p.getZ()==side-1?12:0;
    }
    public static BlockPos origin(BlockPos pos,BlockState state){return pos.subtract(offset(state.getValue(PART),size(state)));}
    public static MiniatureWorldBlockEntity controller(Level level,BlockPos pos){
        if(!level.hasChunkAt(pos))return null;var state=level.getBlockState(pos);
        if(!state.is(MiniatureWorldContent.BLOCK.get()))return null;var root=origin(pos,state);
        return level.hasChunkAt(root)&&level.getBlockEntity(root) instanceof MiniatureWorldBlockEntity be?be:null;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(PART)==0?new MiniatureWorldBlockEntity(pos,state):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,MiniatureWorldContent.ENTITY.get(),MiniatureWorldBlockEntity::tick);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        int side=size(state);var p=offset(state.getValue(PART),side);
        VoxelShape result=p.getY()==0?box(0,0,0,16,4,16):p.getY()==side-1?box(0,15.5,0,16,16,16):Shapes.empty();
        if(p.getX()==0)result=Shapes.or(result,box(0,0,0,.5,16,16));
        if(p.getX()==side-1)result=Shapes.or(result,box(15.5,0,0,16,16,16));
        if(p.getZ()==0)result=Shapes.or(result,box(0,0,0,16,16,.5));
        if(p.getZ()==side-1)result=Shapes.or(result,box(0,0,15.5,16,16,16));
        return result;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.isEmpty())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var be=controller(level,pos);if(be==null)return ItemInteractionResult.FAIL;
        if(!level.isClientSide&&!be.enterDimension()){int n=be.insert(stack);if(!player.isCreative())stack.shrink(n);}
        return ItemInteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        var be=controller(level,pos);if(be==null)return InteractionResult.FAIL;
        if(!level.isClientSide){
            if(be.enterDimension())return InteractionResult.SUCCESS;
            var item=be.takeProduct();if(item.isEmpty())item=be.take();
            if(!item.isEmpty()){if(!player.getInventory().add(item))player.drop(item,false);}else be.query(hit.getDirection(),WorldDevelopmentSeries.panelAt(hit.getDirection(),hit.getLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(be.getBlockPos()))));
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,LevelReader reader,BlockPos pos,Player player){
        var be=reader instanceof Level level?controller(level,pos):null;
        return be!=null?be.packed():new ItemStack(MiniatureWorldContent.ITEM.get());
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide&&!CHANGING.get())dismantle(level,pos,state,true);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide)dismantle(level,pos,state,!player.isCreative());
        return super.playerWillDestroy(level,pos,state,player);
    }
    public static void dismantle(Level level,BlockPos pos,BlockState state,boolean drop){
        if(CHANGING.get())return;var base=origin(pos,state);
        var be=level.hasChunkAt(base)&&level.getBlockEntity(base) instanceof MiniatureWorldBlockEntity root?root:null;
        var packed=be==null?ItemStack.EMPTY:be.packed();CHANGING.set(true);
        try{
            int side=size(state);
            for(int i=0;i<side*side*side;i++){
                var at=base.offset(offset(i,side));if(!level.hasChunkAt(at))continue;var part=level.getBlockState(at);
                if(part.is(state.getBlock())&&part.getValue(PART)==i)level.removeBlock(at,false);
            }
        }finally{CHANGING.set(false);}
        if(drop&&!packed.isEmpty())popResource(level,pos,packed);
    }
    public static boolean place(Level level,BlockPos base){
        var snapshots=new ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();
        for(int i=0;i<PARTS;i++){
            var at=base.offset(offset(i));
            if(!level.hasChunkAt(at)||level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at)
                    ||!level.getBlockState(at).canBeReplaced()||level.getBlockEntity(at)!=null)return false;
            snapshots.add(net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,at));
        }
        CHANGING.set(true);boolean success=false;
        try{
            for(int i=0;i<PARTS;i++)if(!level.setBlock(base.offset(offset(i)),MiniatureWorldContent.BLOCK.get().defaultBlockState().setValue(COMPACT,true).setValue(PART,i),18))return false;
            success=level.getBlockEntity(base) instanceof MiniatureWorldBlockEntity;return success;
        }finally{
            if(!success)for(var snapshot:snapshots)snapshot.restore(18);
            CHANGING.set(false);
            if(success)for(int i=0;i<PARTS;i++)level.updateNeighborsAt(base.offset(offset(i)),MiniatureWorldContent.BLOCK.get());
        }
    }
    public static final class PlacementItem extends BlockItem implements com.mcmagic.omnira.item.MultiblockPlacementItem {
        public PlacementItem(Properties properties){super(MiniatureWorldContent.BLOCK.get(),properties);}
        @Override public boolean canFitInsideContainerItems(ItemStack stack){return !com.mcmagic.omnira.item.PortableStorageRules.packedFrame(stack);}
        @Override public InteractionResult place(BlockPlaceContext context){
            var player=context.getPlayer();if(player==null)return InteractionResult.FAIL;
            var level=context.getLevel();var base=context.getClickedPos().relative(context.getHorizontalDirection(),2).offset(-2,0,-2);
            for(int i=0;i<PARTS;i++){
                var at=base.offset(offset(i));
                if(!level.hasChunkAt(at)||level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at)
                        ||!level.mayInteract(player,at)||!player.mayUseItemAt(at,context.getClickedFace(),context.getItemInHand())
                        ||!level.getBlockState(at).canBeReplaced()||level.getBlockEntity(at)!=null
                        ||!level.isUnobstructed(getBlock().defaultBlockState().setValue(COMPACT,true).setValue(PART,i),at,CollisionContext.empty()))return InteractionResult.FAIL;
            }
            if(level.isClientSide)return InteractionResult.SUCCESS;
            if(!MiniatureWorldBlock.place(level,base))return InteractionResult.FAIL;
            var be=controller(level,base);var data=context.getItemInHand().get(DataComponents.BLOCK_ENTITY_DATA);
            if(data!=null){be.loadWithComponents(data.copyTag(),level.registryAccess());be.resumeClock();level.sendBlockUpdated(base,be.getBlockState(),be.getBlockState(),3);}
            if(!player.isCreative())context.getItemInHand().shrink(1);
            level.playSound(null,base,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,.7F);
            return InteractionResult.CONSUME;
        }
    }
}

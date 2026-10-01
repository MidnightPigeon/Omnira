package com.mcmagic.omnira.archaeology;

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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;

/** All eighteen cells are real selectable parts; one controller owns charge and movement. */
public final class HyperDrillBlock extends BaseEntityBlock implements com.mcmagic.omnira.item.bottle.BottleMultiblock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,17);
    public static final BooleanProperty CHARGED=BooleanProperty.create("charged");
    private static final MapCodec<HyperDrillBlock> CODEC=simpleCodec(HyperDrillBlock::new);
    static final ThreadLocal<Boolean> MOVING=ThreadLocal.withInitial(()->false);
    public HyperDrillBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(PART,4).setValue(CHARGED,true));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PART,CHARGED);}
    public static BlockPos origin(BlockPos p,BlockState s){int n=s.getValue(PART);return p.offset(1-n%3,-n/9,1-(n%9)/3);}
    public static BlockPos offset(int n){return new BlockPos(n%3-1,n/9,(n%9)/3-1);}
    public static Set<BlockPos> parts(Level level,BlockPos origin){
        var result=new LinkedHashSet<BlockPos>();for(int n=0;n<18;n++){
            var p=origin.offset(offset(n));var state=level.getBlockState(p);
            if(!state.is(ArchaeologyContent.DRILL.get())||state.getValue(PART)!=n)return Set.of();result.add(p);
        }return result;
    }
    public BlockPos bottleOrigin(BlockPos p,BlockState s){return origin(p,s);}
    public Set<BlockPos> bottleParts(Level l,BlockPos p,BlockState s){return parts(l,origin(p,s));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(PART)==4?new HyperDrillBlockEntity(p,s):null;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ArchaeologyContent.DRILL_ENTITY.get(),(world,pos,state,drill)->drill.tick());}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isShiftKeyDown())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(l.getBlockEntity(origin(p,s)) instanceof HyperDrillBlockEntity drill){
            if(!l.isClientSide)drill.interact(player,stack,s.getValue(PART)==10);return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player.isShiftKeyDown())return InteractionResult.PASS;
        if(l.getBlockEntity(origin(p,s)) instanceof HyperDrillBlockEntity drill){if(!l.isClientSide)drill.interact(player,ItemStack.EMPTY,s.getValue(PART)==10);return InteractionResult.sidedSuccess(l.isClientSide);}return InteractionResult.PASS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        var root=origin(pos,state);var be=level.getBlockEntity(root);
        if(!level.isClientSide&&be instanceof HyperDrillBlockEntity drill&&!player.isCreative())Block.popResource(level,pos,drill.item());
        clear(level,root);return super.playerWillDestroy(level,pos,state,player);
    }
    static void clear(Level l,BlockPos root){
        boolean before=MOVING.get();MOVING.set(true);
        try{for(int n=0;n<18;n++){var p=root.offset(offset(n));if(l.getBlockState(p).is(ArchaeologyContent.DRILL.get())){l.removeBlockEntity(p);l.setBlock(p,Blocks.AIR.defaultBlockState(),2);}}}
        finally{MOVING.set(before);}
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState after,boolean moving){
        if(!after.is(this)&&!MOVING.get()&&!l.isClientSide){var root=origin(p,s);var be=l.getBlockEntity(root);if(be instanceof HyperDrillBlockEntity drill)Block.popResource(l,p,drill.item());clear(l,root);}
        super.onRemove(s,l,p,after,moving);
    }
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder params){return List.of();}
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return l.getBlockEntity(origin(p,s)) instanceof HyperDrillBlockEntity drill?drill.item():HyperDrillItem.withCharge(new ItemStack(ArchaeologyContent.DRILL_ITEM.get()),s.getValue(CHARGED)?1200:0);}
}

package com.mcmagic.omnira.codex;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public class MyriadCodexBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<MyriadCodexBlock> CODEC=simpleCodec(MyriadCodexBlock::new);
    private static final VoxelShape SHAPE=Shapes.or(box(2,0,2,14,3,14),box(5,3,5,11,9,11),box(1,11,1,15,20,15));
    public MyriadCodexBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return MyriadCodexContent.entity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,MyriadCodexContent.ENTITY.get(),(w,p,b,e)->{
        if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.create.CodexKinetics.tick(e);else if(!w.isClientSide)MyriadCodexEntity.from(e).tick();
    });}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player.isSpectator())return InteractionResult.PASS;
        var codex=MyriadCodexEntity.from(l.getBlockEntity(p));if(codex!=null){
            if(codex.busy())return InteractionResult.FAIL;
            if(!l.isClientSide){
                if(player.getMainHandItem().isEmpty()&&codex.potential>=8192&&codex.heartPos==null)codex.takeHeart((ServerPlayer)player);
                else ((ServerPlayer)player).openMenu(codex,p);
            }
            return InteractionResult.sidedSuccess(l.isClientSide);
        }return InteractionResult.PASS;
    }
    @Override protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND||player.isSpectator())return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if(stack.isEmpty())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var codex=MyriadCodexEntity.from(l.getBlockEntity(p));if(codex!=null&&!codex.busy()){
            if(!l.isClientSide)((ServerPlayer)player).openMenu(codex,p);
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        return ItemInteractionResult.FAIL;
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        var codex=MyriadCodexEntity.from(l.getBlockEntity(p));if(!s.is(next.getBlock())&&codex!=null&&!l.isClientSide){codex.recall();for(int i=0;i<2;i++)Block.popResource(l,p,codex.materials.getStackInSlot(i));}
        super.onRemove(s,l,p,next,moving);
        if(!s.is(next.getBlock()))com.mcmagic.omnira.pulse.PulseNetworks.changed(l,p);
    }
}

package com.mcmagic.omnira.codex;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class WorldHeartBlock extends BaseEntityBlock {
    public static final MapCodec<WorldHeartBlock> CODEC=simpleCodec(WorldHeartBlock::new);
    public WorldHeartBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(4,4,4,12,12,12);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WorldHeartEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,MyriadCodexContent.HEART_ENTITY.get(),(w,p,b,e)->e.tick());}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(l.getBlockEntity(p) instanceof WorldHeartEntity heart&&heart.link!=null&&!l.isClientSide){var codex=heart.link.find(l.getServer());if(codex!=null&&player.getMainHandItem().isEmpty())codex.takeHeart((net.minecraft.server.level.ServerPlayer)player);}
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        var link=l.getBlockEntity(p) instanceof WorldHeartEntity e?e.link:null;
        super.onRemove(s,l,p,next,moving);
        if(!s.is(next.getBlock())&&!l.isClientSide&&link!=null){var codex=link.find(l.getServer());if(codex!=null&&p.equals(codex.heartPos))codex.recall();}
    }
}

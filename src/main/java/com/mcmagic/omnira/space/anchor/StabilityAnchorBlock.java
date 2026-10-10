package com.mcmagic.omnira.space.anchor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class StabilityAnchorBlock extends BaseEntityBlock {
    public final boolean active;
    public StabilityAnchorBlock(boolean active){super(Properties.of().strength(2).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s->active?12:0));this.active=active;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return MapCodec.unit(this);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.or(box(1,0,1,15,3,15),box(3,3,3,13,14,13));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new StabilityAnchorEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,AnchorContent.ENTITY.get(),StabilityAnchorEntity::tick);}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);if(l instanceof ServerLevel server)StabilityField.update(server,p,active);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l instanceof ServerLevel server)StabilityField.update(server,p,false);super.onRemove(s,l,p,next,moving);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(active||!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("omnira:space_microcore"))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!l.isClientSide&&l.setBlockAndUpdate(p,AnchorContent.ACTIVE.get().defaultBlockState())&&!player.getAbilities().instabuild)stack.shrink(1);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
}

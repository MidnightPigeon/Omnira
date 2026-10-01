package com.mcmagic.omnira.aggregation;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public final class AggregationRingItem extends BlockItem {
    public AggregationRingItem(Block block,Properties properties){super(block,properties);}
    @Override public InteractionResult place(BlockPlaceContext context){
        var player=context.getPlayer();var level=context.getLevel();
        if(player==null||!player.isCreative()||!context.canPlace())return InteractionResult.FAIL;
        var center=context.getClickedPos().above();var facing=context.getHorizontalDirection().getOpposite();
        for(int i=0;i<9;i++){
            var pos=AggregationLayout.part(center,facing,i);
            if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)
                    ||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())
                    ||!level.getBlockState(pos).canBeReplaced()||level.getBlockEntity(pos)!=null)return InteractionResult.FAIL;
        }
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!AggregationStructure.assemble(level,center,facing))return InteractionResult.FAIL;
        level.playSound(null,center,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        return InteractionResult.CONSUME;
    }
}

package com.mcmagic.omnira.forging;

import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Creative placement represents the complete machine, never an isolated controller. */
public final class AdvancedForgeItem extends BlockItem {
    public AdvancedForgeItem(Properties properties){super(ModBlocks.ADVANCED_FORGE.get(),properties);}
    @Override public InteractionResult place(BlockPlaceContext context){
        var player=context.getPlayer();var level=context.getLevel();
        if(player==null || !player.isCreative() || !context.canPlace())return InteractionResult.FAIL;
        var center=context.getClickedPos();var facing=context.getHorizontalDirection();
        for(int part=0;part<18;part++){
            var pos=center.offset(ForgeLayout.offset(facing,part));
            if(!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())
                    || !level.getBlockState(pos).canBeReplaced() || level.getBlockEntity(pos)!=null)return InteractionResult.FAIL;
            var state=getBlock().defaultBlockState().setValue(AdvancedForgeBlock.FACING,facing).setValue(AdvancedForgeBlock.PART,part);
            if(!level.isUnobstructed(state,pos,net.minecraft.world.phys.shapes.CollisionContext.empty()))return InteractionResult.FAIL;
        }
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!ForgeLayout.assemble(level,center,facing))return InteractionResult.FAIL;
        level.playSound(null,center,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        return InteractionResult.CONSUME;
    }
}

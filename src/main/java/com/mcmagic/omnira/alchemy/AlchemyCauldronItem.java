package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;

public final class AlchemyCauldronItem extends BlockItem {
    public AlchemyCauldronItem(Properties properties){super(ModBlocks.ROUGH_ALCHEMY_CAULDRON.get(),properties);}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var original=level.getBlockState(pos);
        if(!original.is(BlockTags.CAMPFIRES))return super.useOn(context);
        var player=context.getPlayer();
        if(player!=null&&(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var previous=level.getBlockEntity(pos);
        CompoundTag data=previous==null?new CompoundTag():previous.saveWithFullMetadata(level.registryAccess());
        // Move the campfire's stored state instead of triggering its cooking-item drops.
        level.removeBlockEntity(pos);
        boolean placed=level.setBlock(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState(),3);
        if(!placed||!(level.getBlockEntity(pos) instanceof RoughAlchemyCauldronBlockEntity vessel)){
            level.setBlock(pos,original,3);
            var restored=level.getBlockEntity(pos);
            if(restored!=null&&!data.isEmpty())restored.loadWithComponents(data,level.registryAccess());
            return InteractionResult.FAIL;
        }
        vessel.rememberCampfire(original,data);
        if(player==null||!player.getAbilities().instabuild)context.getItemInHand().shrink(1);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        return InteractionResult.CONSUME;
    }
}

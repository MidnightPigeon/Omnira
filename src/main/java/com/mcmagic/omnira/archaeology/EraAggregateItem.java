package com.mcmagic.omnira.archaeology;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;

/** A crafted relic bundle imbues ordinary time sand with one era's archaeology pool. */
public final class EraAggregateItem extends Item {
    private final int era;
    public EraAggregateItem(Properties p,int era){super(p);this.era=era;}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        if(player==null||player.isShiftKeyDown()||!level.getBlockState(pos).is(ArchaeologyContent.SAND.get()))return InteractionResult.PASS;
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        if(net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snapshot,context.getClickedFace()))return InteractionResult.FAIL;
        if(!level.setBlock(pos,ArchaeologyContent.SUSPICIOUS.get().defaultBlockState().setValue(TimeSandBlock.ERA,era),3))return InteractionResult.FAIL;
        if(level.getBlockEntity(pos) instanceof BrushableBlockEntity sand){
            sand.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("omnira:archaeology/epoch_"+era)),level.random.nextLong());sand.setChanged();
            if(!player.isCreative())context.getItemInHand().shrink(1);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1.1F);
            return InteractionResult.CONSUME;
        }
        snapshot.restore();return InteractionResult.FAIL;
    }
}

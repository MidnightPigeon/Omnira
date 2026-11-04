package com.mcmagic.omnira.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;

public final class CrystallizedPhilosophersStoneItem extends Item {
    public static final TagKey<Block> STONES = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:stones"));

    public CrystallizedPhilosophersStoneItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }

    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copyWithCount(1); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();
        var pos=context.getClickedPos();
        var player=context.getPlayer();
        if (!level.getBlockState(pos).is(STONES) || player==null || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())) return InteractionResult.FAIL;
        if (!level.isClientSide && !level.setBlockAndUpdate(pos,Blocks.RAW_GOLD_BLOCK.defaultBlockState())) return InteractionResult.FAIL;
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

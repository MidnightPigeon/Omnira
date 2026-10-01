package com.mcmagic.omnira.block;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class InfusableCrystalBlock extends TransparentBlock {
    public static final MapCodec<InfusableCrystalBlock> CODEC=simpleCodec(InfusableCrystalBlock::new);
    public InfusableCrystalBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends TransparentBlock> codec(){return CODEC;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isShiftKeyDown()||!stack.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide){
            level.setBlockAndUpdate(pos,ModBlocks.INFUSED_CRYSTAL_CASING.get().defaultBlockState());
            if(!player.isCreative())stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}

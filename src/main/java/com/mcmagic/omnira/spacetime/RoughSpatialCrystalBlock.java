package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class RoughSpatialCrystalBlock extends TransparentBlock {
    public RoughSpatialCrystalBlock(Properties properties){super(properties);}

    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
                                                       Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isShiftKeyDown()||!player.mayBuild()||!stack.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide&&level.setBlockAndUpdate(pos,DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState())){
            if(!player.getAbilities().instabuild)stack.shrink(1);
            level.levelEvent(1505,pos,0);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}

package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CrystalPedestalBlock extends CrystalProcessingTableBlock {
    public static final MapCodec<CrystalPedestalBlock> CODEC=simpleCodec(CrystalPedestalBlock::new);
    public CrystalPedestalBlock(Properties properties) {super(properties);}
    @Override protected MapCodec<? extends CrystalProcessingTableBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new CrystalPedestalBlockEntity(pos,state);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return Shapes.or(box(3,0,3,13,1,13),box(4,1,4,12,2,12),
                box(5,2,5,11,8,11),box(4,8,4,12,9,12),
                box(3,9,3,13,10,4),box(3,9,12,13,10,13),
                box(3,9,4,4,10,12),box(12,9,4,13,10,12));
    }
    private InteractionResult interact(Level level,BlockPos pos,Player player,InteractionHand hand) {
        if(player.isSpectator()) return InteractionResult.PASS;
        if(!(level.getBlockEntity(pos) instanceof CrystalPedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if(level.isClientSide) return InteractionResult.SUCCESS;
        if(!pedestal.getItem(0).isEmpty()) {
            var item=pedestal.removeItem(0,1);
            if(!player.getInventory().add(item)) player.drop(item,false);
            pedestal.setChanged();
        } else if(!player.getItemInHand(hand).isEmpty()) {
            var held=player.getItemInHand(hand);
            pedestal.setItem(0,held.copyWithCount(1));
            held.consume(1,player);
        }
        return InteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        return interact(level,pos,player,InteractionHand.MAIN_HAND);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
                                                       Player player,InteractionHand hand,BlockHitResult hit) {
        interact(level,pos,player,hand);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}

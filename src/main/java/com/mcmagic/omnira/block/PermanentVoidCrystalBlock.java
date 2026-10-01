package com.mcmagic.omnira.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Permanent terrain and construction share a block-entity-free implementation. */
public final class PermanentVoidCrystalBlock extends TransparentBlock {
    public PermanentVoidCrystalBlock(Properties properties) {super(properties);}
    @Override public net.minecraft.world.item.Item asItem() {return com.mcmagic.omnira.registry.ModItems.VOID_CRYSTAL.get();}
    @Override public String getDescriptionId() {return "block.omnira.void_crystal";}
    @Override protected boolean skipRendering(BlockState state,BlockState neighbor,Direction face) {
        return neighbor.getBlock() instanceof PermanentVoidCrystalBlock || neighbor.getBlock() instanceof VoidCrystalBlock
                || super.skipRendering(state,neighbor,face);
    }
}

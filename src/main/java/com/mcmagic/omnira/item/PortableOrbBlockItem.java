package com.mcmagic.omnira.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class PortableOrbBlockItem extends BlockItem {
    public PortableOrbBlockItem(Block block,Properties properties){super(block,properties);}
    @Override public boolean canFitInsideContainerItems(ItemStack stack){return !PortableStorageRules.stabilized(stack);}
}

package com.mcmagic.omnira.item.bottle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Set;

/** Opt-in for whole Omnira machines; never captures arbitrary adjacent terrain. */
public interface BottleMultiblock {
    BlockPos bottleOrigin(BlockPos pos,BlockState state);
    Set<BlockPos> bottleParts(Level level,BlockPos pos,BlockState state);
}

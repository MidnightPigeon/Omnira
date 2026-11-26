package com.mcmagic.omnira.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Identifies the block that owns a component's native menu. */
public interface MenuControllerPart {
    BlockPos menuController(BlockPos part,BlockState state);
}

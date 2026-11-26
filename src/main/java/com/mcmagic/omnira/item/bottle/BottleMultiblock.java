package com.mcmagic.omnira.item.bottle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Set;

/** Opt-in for whole Omnira machines; never captures arbitrary adjacent terrain. */
public interface BottleMultiblock extends com.mcmagic.omnira.storage.MenuControllerPart {
    @Override default BlockPos menuController(BlockPos part,BlockState state){return bottleOrigin(part,state);}
    BlockPos bottleOrigin(BlockPos pos,BlockState state);
    Set<BlockPos> bottleParts(Level level,BlockPos pos,BlockState state);
}

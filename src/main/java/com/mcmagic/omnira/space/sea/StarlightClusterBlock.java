package com.mcmagic.omnira.space.sea;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class StarlightClusterBlock extends Block {
    private static final VoxelShape OUTLINE = box(3, 4, 3, 13, 13, 13);

    public StarlightClusterBlock() {
        super(Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.AMETHYST_CLUSTER).lightLevel(state -> 15));
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }
}

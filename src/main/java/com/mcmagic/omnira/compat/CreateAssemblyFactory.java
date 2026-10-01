package com.mcmagic.omnira.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;

public final class CreateAssemblyFactory {
    public static Block block(BlockBehaviour.Properties properties) {
        return new com.mcmagic.omnira.block.KineticAssemblyBlock(properties);
    }
    public static BlockEntity entity(BlockPos pos,BlockState state) {
        return new com.mcmagic.omnira.block.entity.KineticAssemblyBlockEntity(pos,state);
    }
}

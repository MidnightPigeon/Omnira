package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.block.ManaEngineBlock;
import com.mcmagic.omnira.block.entity.ManaEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;

/** The common registries only invoke this factory when Create is loaded. */
public final class CreateEngineFactory {
    private CreateEngineFactory() {}
    public static Block block(BlockBehaviour.Properties properties) {return new ManaEngineBlock(properties);}
    public static BlockEntity entity(BlockPos pos,BlockState state) {return new ManaEngineBlockEntity(pos,state);}
}

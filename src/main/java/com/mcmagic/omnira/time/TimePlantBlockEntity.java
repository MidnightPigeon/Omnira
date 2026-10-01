package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TimePlantBlockEntity extends BlockEntity {
    public TimePlantBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.TIME_PLANT.get(),pos,state);}
}

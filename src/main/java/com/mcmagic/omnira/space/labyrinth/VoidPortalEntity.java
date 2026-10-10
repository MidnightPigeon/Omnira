package com.mcmagic.omnira.space.labyrinth;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class VoidPortalEntity extends BlockEntity {
    public VoidPortalEntity(BlockPos pos,BlockState state){super(LabyrinthContent.ENTITY.get(),pos,state);}
}

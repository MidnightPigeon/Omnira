package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Render-only block entity; the portal owns the ritual lifetime. */
public final class RitualEnergyCoreBlockEntity extends BlockEntity {
    public RitualEnergyCoreBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.RITUAL_ENERGY_CORE.get(),pos,state);}
}

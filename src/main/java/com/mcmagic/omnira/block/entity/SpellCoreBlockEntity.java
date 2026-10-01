package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Render-only core: no inventory, server ticker or area attribute effects. */
public final class SpellCoreBlockEntity extends BlockEntity {
    public SpellCoreBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.SPELL_CORE.get(),pos,state);}
}

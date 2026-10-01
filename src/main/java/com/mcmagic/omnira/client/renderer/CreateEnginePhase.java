package com.mcmagic.omnira.client.renderer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Isolated optional Create linkage; same clock, speed and positional phase as connected shafts. */
final class CreateEnginePhase {
    static float degrees(BlockEntity entity) {
        var kinetic=(KineticBlockEntity)entity;
        var axis=entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
        return (float)Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(kinetic,entity.getBlockPos(),axis));
    }
}

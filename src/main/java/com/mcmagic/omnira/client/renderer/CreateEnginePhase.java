package com.mcmagic.omnira.client.renderer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Isolated optional Create linkage; same clock, speed and positional phase as connected shafts. */
public final class CreateEnginePhase {
    public static float degrees(BlockEntity entity) {
        var kinetic=(KineticBlockEntity)entity;
        var axis=entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
        return degrees(kinetic,axis);
    }
    public static float degrees(BlockEntity entity,net.minecraft.core.Direction.Axis axis){
        if(!(entity instanceof KineticBlockEntity kinetic))return 0;
        return (float)Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(kinetic,entity.getBlockPos(),axis));
    }
}

package com.mcmagic.omnira.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Fixed-rate current around an input face, independent of rendering frame rate. */
public final class ShaftCurrent {
    private ShaftCurrent() {}
    public static void spark(Level level,BlockPos pos,Direction face) {
        if(level instanceof ServerLevel server&&level.getGameTime()%5==0)
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,pos.getX()+.5+face.getStepX()*.48,
                    pos.getY()+.5,pos.getZ()+.5+face.getStepZ()*.48,2,.08,.08,.08,.01);
    }
}

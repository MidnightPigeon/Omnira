package com.mcmagic.omnira.space.phase;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Client-only decoration discovery; no particle objects or server-side work. */
public final class PhaseParticles {
    public static java.util.function.BiConsumer<Level,BlockPos> observe=(level,pos)->{};
    static void inside(Level level,BlockPos pos,RandomSource random,float phase){
        observe.accept(level,pos);
    }
    static void around(Level level,BlockPos pos,RandomSource random){
        observe.accept(level,pos);
    }
    private PhaseParticles(){}
}

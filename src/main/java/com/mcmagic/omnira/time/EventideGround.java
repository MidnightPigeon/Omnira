package com.mcmagic.omnira.time;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/** Small irregular masonry patches soften the transition into decayed soil. */
public final class EventideGround {
    private static final SimplexNoise PATCH=new SimplexNoise(RandomSource.create(0xE7E01DEL));
    private EventideGround() {}
    public static boolean masonry(int x,int y,int z,int surface) {
        double patch=.75*PATCH.getValue(x/11.0,z/11.0)+.25*PATCH.getValue(x/3.0,z/3.0);
        return patch>(y>=surface-2?.18:.48);
    }
}

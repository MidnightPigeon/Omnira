package com.mcmagic.omnira.spacetime;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Client decoration hook; no particle lifetimes, block entities or server work. */
public final class SpatialCrystalMotes {
    public static final int CACHE_LIMIT=256,RANGE=24;
    public static final float RADIUS=1F/64;
    public static final int COLOR=0xB5C2F5;
    public static BiConsumer<Level,BlockPos> observe=(level,pos)->{};
    public static float height(double time){return .5F+(float)Math.sin(time*.027)*.2F;}
    private SpatialCrystalMotes(){}
}

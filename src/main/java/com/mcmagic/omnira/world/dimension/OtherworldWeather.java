package com.mcmagic.omnira.world.dimension;

import net.minecraft.world.level.Level;

/** Weather suppression is dimension-wide, independent of biome height and shared world data. */
public final class OtherworldWeather {
    public static boolean disabled(Level level){
        return level.dimension().equals(ModDimensions.DREAM_REALM)||level.dimension().equals(ModDimensions.SPACETIME_CORRIDOR);
    }
    private OtherworldWeather(){}
}

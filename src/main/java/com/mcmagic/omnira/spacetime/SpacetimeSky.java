package com.mcmagic.omnira.spacetime;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class SpacetimeSky {
    public static final ResourceLocation EFFECTS = ResourceLocation.fromNamespaceAndPath("omnira", "spacetime_corridor");
    public static final int SKY_LIGHT = 10;
    public static final int DARKEN = 15 - SKY_LIGHT;

    private SpacetimeSky() {}

    public static boolean applies(Level level) {
        return level.dimensionType().effectsLocation().equals(EFFECTS);
    }

    public static int effectiveLight(int skyLight) {
        return Math.max(0, skyLight - DARKEN);
    }

    public static void prepareChunkLighting(net.minecraft.nbt.CompoundTag tag) {
        if (tag.getInt("omnira:sky_lighting_version") >= 1) return;
        tag.putBoolean("isLightOn", false);
        for (var entry : tag.getList("sections", 10)) {
            var section = (net.minecraft.nbt.CompoundTag)entry;
            section.remove("SkyLight");
            section.remove("BlockLight");
        }
    }
}

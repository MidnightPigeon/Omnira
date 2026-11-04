package com.mcmagic.omnira.time;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

/** Cache the seed-dependent sample coordinates, never the biome stored there. Owned by one ticking level. */
public final class MachineBiomeSamples {
    private record Sample(long position, int x, int y, int z) {}
    private static final int CAPACITY = 4096;
    private final Sample[] samples = new Sample[CAPACITY];
    private final BiomeManager manager;
    private final BiomeManager locator;
    private long locatingPosition;

    public MachineBiomeSamples(BiomeManager manager) {
        this.manager = manager;
        locator = manager.withDifferentSource(this::recordSample);
    }

    private Holder<Biome> recordSample(int x, int y, int z) {
        samples[index(locatingPosition)] = new Sample(locatingPosition, x, y, z);
        return manager.getNoiseBiomeAtQuart(x, y, z);
    }

    private static int index(long position) {
        return (int) HashCommon.mix(position) & (CAPACITY - 1);
    }

    public boolean matches(Level level, BlockPos pos, ResourceKey<Biome> biome) {
        if (pos == null || !level.hasChunkAt(pos)) return false;
        if (level.getBiomeManager() != manager || manager.getClass() != BiomeManager.class)
            return level.getBiome(pos).is(biome);
        long key = pos.asLong();
        Sample sample = samples[index(key)];
        if (sample != null && sample.position == key)
            return manager.getNoiseBiomeAtQuart(sample.x, sample.y, sample.z).is(biome);
        locatingPosition = key;
        return locator.getBiome(pos).is(biome);
    }
}

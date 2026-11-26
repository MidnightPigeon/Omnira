package com.mcmagic.omnira.spacetime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Eventide Ruins replaced the original sixth time region. */
public final class LegacyTimeBiome {
    private static final ResourceLocation LEGACY = ResourceLocation.parse("omnira:temporal_continent");
    private static final ResourceLocation SUCCESSOR = ResourceLocation.parse("omnira:eventide_ruins");

    public static ResourceLocation migrate(ResourceLocation id) {
        return LEGACY.equals(id) ? SUCCESSOR : id;
    }

    public static void migrateChunk(CompoundTag chunk) {
        for (var section : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            var palette = ((CompoundTag)section).getCompound("biomes").getList("palette", Tag.TAG_STRING);
            for (int i = 0; i < palette.size(); i++) {
                if (LEGACY.toString().equals(palette.getString(i))) palette.set(i, StringTag.valueOf(SUCCESSOR.toString()));
            }
        }
    }

    private LegacyTimeBiome() {}
}

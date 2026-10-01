package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.Omnira;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ModDimensions {
    public static final net.neoforged.neoforge.registries.DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> GENERATORS=
            net.neoforged.neoforge.registries.DeferredRegister.create(Registries.CHUNK_GENERATOR,Omnira.MOD_ID);
    public static final java.util.function.Supplier<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> CORRIDOR_GENERATOR=
            GENERATORS.register("spacetime_corridor",()->com.mcmagic.omnira.spacetime.CorridorGenerator.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES=
            net.neoforged.neoforge.registries.DeferredRegister.create(Registries.BIOME_SOURCE,Omnira.MOD_ID);
    public static final java.util.function.Supplier<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> STRATA_BIOMES=
            BIOME_SOURCES.register("spacetime_strata",()->com.mcmagic.omnira.spacetime.StrataBiomeSource.CODEC);
    public static final ResourceKey<Level> SPACETIME_CORRIDOR=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"spacetime_corridor"));
    public static final ResourceKey<Level> MAGIC_REALM = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "magic_realm")
    );

    public static final ResourceKey<Level> DREAM_REALM=ResourceKey.create(
            Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"dream_realm"));
    private ModDimensions() {
    }
}

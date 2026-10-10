package com.mcmagic.omnira.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.grower.TreeGrower.class)
public interface CatalysisTreeGrowerAccessor {
    @org.spongepowered.asm.mixin.gen.Invoker("getConfiguredFeature") ResourceKey<ConfiguredFeature<?,?>> omnira$tree(RandomSource random,boolean flowers);
    @org.spongepowered.asm.mixin.gen.Invoker("getConfiguredMegaFeature") ResourceKey<ConfiguredFeature<?,?>> omnira$megaTree(RandomSource random);
}

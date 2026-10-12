package com.mcmagic.omnira.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.grower.TreeGrower.class)
public interface CatalysisTreeGrowerAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("tree") java.util.Optional<ResourceKey<ConfiguredFeature<?,?>>> omnira$primaryTree();
    @org.spongepowered.asm.mixin.gen.Accessor("secondaryTree") java.util.Optional<ResourceKey<ConfiguredFeature<?,?>>> omnira$secondaryTree();
    @org.spongepowered.asm.mixin.gen.Accessor("megaTree") java.util.Optional<ResourceKey<ConfiguredFeature<?,?>>> omnira$primaryMegaTree();
    @org.spongepowered.asm.mixin.gen.Accessor("secondaryMegaTree") java.util.Optional<ResourceKey<ConfiguredFeature<?,?>>> omnira$secondaryMegaTree();
    @org.spongepowered.asm.mixin.gen.Accessor("flowers") java.util.Optional<ResourceKey<ConfiguredFeature<?,?>>> omnira$flowerTree();
    @org.spongepowered.asm.mixin.gen.Accessor("secondaryFlowers") java.util.Optional<ResourceKey<ConfiguredFeature<?,?>>> omnira$secondaryFlowerTree();
    @org.spongepowered.asm.mixin.gen.Invoker("getConfiguredFeature") ResourceKey<ConfiguredFeature<?,?>> omnira$tree(RandomSource random,boolean flowers);
    @org.spongepowered.asm.mixin.gen.Invoker("getConfiguredMegaFeature") ResourceKey<ConfiguredFeature<?,?>> omnira$megaTree(RandomSource random);
}

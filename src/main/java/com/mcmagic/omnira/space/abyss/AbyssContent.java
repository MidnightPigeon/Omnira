package com.mcmagic.omnira.space.abyss;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class AbyssContent {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:inverted_abyss"));
    public static final DeferredBlock<Block> MASS=ModBlocks.BLOCKS.register("dense_void_mass",()->new DenseVoidMassBlock(
            BlockBehaviour.Properties.of().strength(2).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()));
    static {ModItems.ITEMS.registerSimpleBlockItem(MASS);}
    public static void init(){}
    private AbyssContent(){}
}

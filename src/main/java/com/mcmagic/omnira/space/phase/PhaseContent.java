package com.mcmagic.omnira.space.phase;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class PhaseContent {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:phase_crystal_grove"));
    public static final DeferredBlock<PhaseBlock> BLOCK=ModBlocks.BLOCKS.register("phase_crystal_block",()->new PhaseBlock(
            BlockBehaviour.Properties.ofFullCopy(DreamContent.SPATIAL_CRYSTAL.get()).mapColor(s->s.getValue(PhaseBlock.PHASE)>=8?net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE:net.minecraft.world.level.material.MapColor.COLOR_BLUE).lightLevel(s->12+Math.round(s.getValue(PhaseBlock.PHASE)*3F/16))));
    public static final DeferredItem<BlockItem> BLOCK_ITEM=ModItems.ITEMS.registerSimpleBlockItem(BLOCK);
    public static final DeferredBlock<PhaseClusterBlock> CLUSTER=ModBlocks.BLOCKS.register("phase_crystal_cluster",()->new PhaseClusterBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLUE).noOcclusion().lightLevel(s->12)));
    public static final DeferredItem<BlockItem> CLUSTER_ITEM=ModItems.ITEMS.registerSimpleBlockItem(CLUSTER);
    public static void init(){}
    private PhaseContent(){}
}

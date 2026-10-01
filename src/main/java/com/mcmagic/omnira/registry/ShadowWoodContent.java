package com.mcmagic.omnira.registry;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.registries.DeferredBlock;
import java.util.function.Supplier;

public final class ShadowWoodContent {
    public static final DeferredBlock<Block> WOOD=block("shadow_wood",()->new RotatedPillarBlock(copy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<Block> STRIPPED_LOG=block("stripped_shadow_log",()->new RotatedPillarBlock(copy(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredBlock<Block> STRIPPED_WOOD=block("stripped_shadow_wood",()->new RotatedPillarBlock(copy(Blocks.STRIPPED_OAK_WOOD)));
    public static final DeferredBlock<Block> STAIRS=block("shadow_stairs",()->new com.mcmagic.omnira.block.DecorativeStairBlock(DreamContent.SHADOW_PLANKS.get().defaultBlockState(),copy(Blocks.OAK_STAIRS)));
    public static final DeferredBlock<Block> SLAB=block("shadow_slab",()->new SlabBlock(copy(Blocks.OAK_SLAB)));
    public static final DeferredBlock<Block> FENCE=block("shadow_fence",()->new FenceBlock(copy(Blocks.OAK_FENCE)));
    public static final DeferredBlock<Block> GATE=block("shadow_fence_gate",()->new FenceGateBlock(WoodType.OAK,copy(Blocks.OAK_FENCE_GATE)));
    public static final DeferredBlock<Block> DOOR=block("shadow_door",()->new DoorBlock(BlockSetType.OAK,copy(Blocks.OAK_DOOR)));
    public static final DeferredBlock<Block> TRAPDOOR=block("shadow_trapdoor",()->new TrapDoorBlock(BlockSetType.OAK,copy(Blocks.OAK_TRAPDOOR)));
    public static final DeferredBlock<Block> BUTTON=block("shadow_button",()->new ButtonBlock(BlockSetType.OAK,30,copy(Blocks.OAK_BUTTON)));
    public static final DeferredBlock<Block> PLATE=block("shadow_pressure_plate",()->new PressurePlateBlock(BlockSetType.OAK,copy(Blocks.OAK_PRESSURE_PLATE)));
    public static final DeferredBlock<Block> SAPLING=block("shadow_sapling",()->new com.mcmagic.omnira.block.ShadowSaplingBlock(copy(Blocks.JUNGLE_SAPLING)));
    private static BlockBehaviour.Properties copy(Block block) {return BlockBehaviour.Properties.ofFullCopy(block);}
    private static DeferredBlock<Block> block(String name,Supplier<Block> supplier) {
        var result=ModBlocks.BLOCKS.register(name,supplier);DreamContent.ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(result));return result;
    }
    public static void init() {}
    public static void flammability() {
        var fire=(FireBlock)Blocks.FIRE;
        for(var b:new Block[]{DreamContent.SHADOW_LOG.get(),WOOD.get(),STRIPPED_LOG.get(),STRIPPED_WOOD.get()}) fire.setFlammable(b,5,5);
        for(var b:new Block[]{DreamContent.SHADOW_PLANKS.get(),STAIRS.get(),SLAB.get(),FENCE.get(),GATE.get()}) fire.setFlammable(b,5,20);
        fire.setFlammable(DreamContent.SHADOW_LEAVES.get(),30,60);
    }
}

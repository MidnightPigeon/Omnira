package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.time.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.registries.DeferredBlock;
import java.util.function.Supplier;

public final class TimeNatureContent {
    public static final DeferredBlock<Block> LOG=block("time_log",()->new RotatedPillarBlock(copy(Blocks.OAK_LOG)));
    public static final DeferredBlock<Block> WOOD=block("time_wood",()->new RotatedPillarBlock(copy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<Block> STRIPPED_LOG=block("stripped_time_log",()->new RotatedPillarBlock(copy(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredBlock<Block> STRIPPED_WOOD=block("stripped_time_wood",()->new RotatedPillarBlock(copy(Blocks.STRIPPED_OAK_WOOD)));
    public static final DeferredBlock<Block> PLANKS=block("time_planks",()->new Block(copy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<Block> LEAVES=block("time_leaves",()->new TimeLeavesBlock(copy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<Block> SAPLING=block("time_sapling",()->new TimeSaplingBlock(copy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<Block> STAIRS=block("time_stairs",()->new com.mcmagic.omnira.block.DecorativeStairBlock(PLANKS.get().defaultBlockState(),copy(Blocks.OAK_STAIRS)));
    public static final DeferredBlock<Block> SLAB=block("time_slab",()->new SlabBlock(copy(Blocks.OAK_SLAB)));
    public static final DeferredBlock<Block> FENCE=block("time_fence",()->new FenceBlock(copy(Blocks.OAK_FENCE)));
    public static final DeferredBlock<Block> GATE=block("time_fence_gate",()->new FenceGateBlock(WoodType.OAK,copy(Blocks.OAK_FENCE_GATE)));
    public static final DeferredBlock<Block> DOOR=block("time_door",()->new DoorBlock(BlockSetType.OAK,copy(Blocks.OAK_DOOR)));
    public static final DeferredBlock<Block> TRAPDOOR=block("time_trapdoor",()->new TrapDoorBlock(BlockSetType.OAK,copy(Blocks.OAK_TRAPDOOR)));
    public static final DeferredBlock<Block> BUTTON=block("time_button",()->new ButtonBlock(BlockSetType.OAK,30,copy(Blocks.OAK_BUTTON)));
    public static final DeferredBlock<Block> PLATE=block("time_pressure_plate",()->new PressurePlateBlock(BlockSetType.OAK,copy(Blocks.OAK_PRESSURE_PLATE)));
    public static final DeferredBlock<Block> SILT=block("sedimented_temporal_silt",()->new com.mcmagic.omnira.mire.DecayingTemporalSiltBlock(BlockBehaviour.Properties.of().strength(.8F).sound(SoundType.GRAVEL).randomTicks()));
    public static final DeferredBlock<Block> LIVING_SILT=block("sedimented_living_temporal_silt",()->new com.mcmagic.omnira.mire.DecayingTemporalSiltBlock(BlockBehaviour.Properties.of().strength(.8F).sound(SoundType.GRAVEL).randomTicks()));
    public static final DeferredBlock<Block> GRASS=block("time_grass",()->new TimePlantBlock(false,copy(Blocks.SHORT_GRASS).noCollission().noOcclusion().offsetType(BlockBehaviour.OffsetType.NONE)));
    public static final DeferredBlock<Block> FLOWER=block("time_flower",()->new TimePlantBlock(true,copy(Blocks.ALLIUM).noCollission().noOcclusion().offsetType(BlockBehaviour.OffsetType.NONE)));
    public static final DeferredBlock<Block> SPATIAL_MATRIX=block("spatial_crystal_matrix",()->new SpatialMatrixBlock(copy(Blocks.BUDDING_AMETHYST).lightLevel(s->10).randomTicks()));
    public static final DeferredBlock<Block> SMALL_SPATIAL_BUD=bud("small_spatial_bud",3,6);
    public static final DeferredBlock<Block> MEDIUM_SPATIAL_BUD=bud("medium_spatial_bud",5,5);
    public static final DeferredBlock<Block> LARGE_SPATIAL_BUD=bud("large_spatial_bud",7,3);
    public static final DeferredBlock<Block> SPATIAL_CLUSTER=bud("spatial_crystal_cluster",12,2);
    public static final net.neoforged.neoforge.registries.DeferredItem<Item> TIME_SEED=ModItems.ITEMS.registerItem("time_seed",TimeSeedItem::new,new Item.Properties());
    public static final net.neoforged.neoforge.registries.DeferredItem<Item> SPATIAL_SHARD=ModItems.ITEMS.registerItem("spatial_crystal_shard",SpatialCrystalItem::new,new Item.Properties());
    private static DeferredBlock<Block> bud(String name,int height,int inset){return block(name,()->new SpatialBudBlock(height,inset,copy(Blocks.AMETHYST_CLUSTER).lightLevel(s->7)));}
    private static BlockBehaviour.Properties copy(Block block){return BlockBehaviour.Properties.ofFullCopy(block);}
    private static DeferredBlock<Block> block(String name,Supplier<Block> factory){
        var block=ModBlocks.BLOCKS.register(name,factory);DreamContent.ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(block));return block;
    }
    public static void init(){}
    public static void flammability(){
        var fire=(FireBlock)Blocks.FIRE;
        for(var b:new Block[]{LOG.get(),WOOD.get(),STRIPPED_LOG.get(),STRIPPED_WOOD.get()})fire.setFlammable(b,5,5);
        for(var b:new Block[]{PLANKS.get(),STAIRS.get(),SLAB.get(),FENCE.get(),GATE.get()})fire.setFlammable(b,5,20);
        fire.setFlammable(LEAVES.get(),30,60);
    }
    private TimeNatureContent(){}
}

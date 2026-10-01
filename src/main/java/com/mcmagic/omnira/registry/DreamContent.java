package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.block.ShadowBerryBlock;
import com.mcmagic.omnira.block.DecorativeStairBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Dream building materials are ordinary blocks, never per-block entities. */
public final class DreamContent {
    public static final List<DeferredItem<BlockItem>> ITEMS=new ArrayList<>();
    public static final DeferredBlock<Block> LIGHT_CONDENSATE=block("light_condensate",()->glass(7));
    public static final DeferredBlock<Block> LIGHT_SOURCE_CRYSTAL=block("light_source_crystal",()->glass(10));
    public static final DeferredBlock<Block> LIGHT_CONDENSATE_BRICKS=block("light_condensate_bricks",()->glass(7));
    public static final DeferredBlock<Block> LIGHT_SOURCE_BRICKS=block("light_source_bricks",()->glass(10));
    public static final DeferredBlock<Block> SPIRITUAL_CRYSTAL_BLOCK=block("spiritual_crystal_block",()->new com.mcmagic.omnira.block.InfusableCrystalBlock(
            stone().noOcclusion().sound(SoundType.AMETHYST).lightLevel(s->10)
                    .isRedstoneConductor((s,l,p)->false).isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
    public static final DeferredBlock<Block> SPIRITUAL_CRYSTAL_STAIRS=block("spiritual_crystal_stairs",()->new DecorativeStairBlock(SPIRITUAL_CRYSTAL_BLOCK.get().defaultBlockState(),BlockBehaviour.Properties.ofFullCopy(SPIRITUAL_CRYSTAL_BLOCK.get())));
    public static final DeferredBlock<Block> DREAM_CRYSTAL_BLOCK=block("dream_crystal_block",()->glass(10));
    public static final DeferredBlock<Block> CRYSTAL_COLUMN=block("crystal_column",()->new com.mcmagic.omnira.block.CrystalColumnBlock(stone().noOcclusion().sound(SoundType.AMETHYST).lightLevel(s->10),0));
    public static final DeferredBlock<Block> CRYSTAL_COLUMN_BASE=block("crystal_column_base",()->new com.mcmagic.omnira.block.CrystalColumnBlock(stone().noOcclusion().sound(SoundType.AMETHYST).lightLevel(s->10),1));
    public static final DeferredBlock<Block> CRYSTAL_COLUMN_CAPITAL=block("crystal_column_capital",()->new com.mcmagic.omnira.block.CrystalColumnBlock(stone().noOcclusion().sound(SoundType.AMETHYST).lightLevel(s->10),2));
    public static final DeferredBlock<Block> LIGHT_CRYSTAL_TORCH=block("light_crystal_torch",()->new com.mcmagic.omnira.block.LightCrystalTorchBlock(
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s->15)));
    public static final DeferredBlock<Block> LIGHT_CRYSTAL_CORE=block("light_crystal_core",()->new com.mcmagic.omnira.block.LightCrystalCoreBlock(stone().randomTicks().lightLevel(s->15).sound(SoundType.AMETHYST)));
    public static final DeferredBlock<Block> SOLIDIFIED_LIGHT_CRYSTAL_CORE=block("solidified_light_crystal_core",()->new Block(stone().lightLevel(s->15).sound(SoundType.AMETHYST)));
    public static final DeferredBlock<Block> SHADOW_ROCK=block("shadow_rock",()->new Block(stone()));
    public static final DeferredBlock<Block> SHADOW_LANTERN=block("shadow_lantern",()->new com.mcmagic.omnira.block.ShadowLanternBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).sound(SoundType.AMETHYST).lightLevel(s->12)));
    public static final DeferredBlock<Block> SHADOW_ROCK_BRICKS=block("shadow_rock_bricks",()->new Block(stone()));
    public static final DeferredBlock<Block> CHISELED_SHADOW_ROCK=block("chiseled_shadow_rock",()->new Block(stone()));
    public static final DeferredBlock<Block> SHADOW_BOOKSHELF=block("shadow_bookshelf",()->new com.mcmagic.omnira.block.ShadowBookshelfBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF)));
    public static final DeferredBlock<Block> SHADOW_ROCK_SLAB=block("shadow_rock_slab",()->new SlabBlock(stone()));
    public static final DeferredBlock<Block> SHADOW_ROCK_STAIRS=block("shadow_rock_stairs",()->new DecorativeStairBlock(SHADOW_ROCK.get().defaultBlockState(),stone()));
    public static final DeferredBlock<Block> MOSSY_SHADOW_ROCK=block("mossy_shadow_rock",()->new Block(stone().lightLevel(s->4)));
    public static final DeferredBlock<Block> SHADOW_LOG=block("shadow_log",()->new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG)));
    public static final DeferredBlock<Block> SHADOW_PLANKS=block("shadow_planks",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_PLANKS)));
    public static final DeferredBlock<Block> SHADOW_LEAVES=block("shadow_leaves",()->new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES)));
    public static final DeferredBlock<Block> SHADOW_BERRIES=block("shadow_berries",()->new ShadowBerryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COCOA)));
    public static final DeferredBlock<Block> MIRROR_ROCK=block("mirror_rock",()->new Block(stone().sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> ENGRAVED_MIRROR_ROCK=block("engraved_mirror_rock",()->new Block(stone().sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> VOID_MIRROR_ROCK=block("void_mirror_rock",()->new Block(stone().sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> VOID_ENGRAVED_MIRROR_ROCK=block("void_engraved_mirror_rock",()->new Block(stone().sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> ENGRAVED_MIRROR_ROCK_STAIRS=block("engraved_mirror_rock_stairs",()->new DecorativeStairBlock(ENGRAVED_MIRROR_ROCK.get().defaultBlockState(),stone().sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> ENGRAVED_MIRROR_ROCK_SLAB=block("engraved_mirror_rock_slab",()->new SlabBlock(stone().sound(SoundType.GLASS)));
    public static final DeferredBlock<Block> TEMPORAL_SILT=block("temporal_silt",()->new com.mcmagic.omnira.time.TemporalSiltBlock(false,BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.GRAVEL).randomTicks()));
    public static final DeferredBlock<Block> LIVING_TEMPORAL_SILT=block("living_temporal_silt",()->new com.mcmagic.omnira.time.TemporalSiltBlock(true,BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.GRAVEL).randomTicks()));
    public static final DeferredBlock<Block> SPATIAL_CRYSTAL=block("rough_spatial_crystal_block",()->new com.mcmagic.omnira.spacetime.RoughSpatialCrystalBlock(stone().noOcclusion()
            .sound(SoundType.AMETHYST).lightLevel(s->5).isRedstoneConductor((s,l,p)->false)
            .isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
    public static final DeferredBlock<Block> EXCITED_SPATIAL_CRYSTAL=block("excited_rough_spatial_crystal_block",()->new com.mcmagic.omnira.spacetime.ExcitedSpatialCrystalBlock(
            stone().noOcclusion().sound(SoundType.AMETHYST).lightLevel(s->15).isRedstoneConductor((s,l,p)->false)
                    .isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
    public static final DeferredBlock<Block> DREAM_CRYSTAL_BEDROCK=block("dream_crystal_bedrock",()->new com.mcmagic.omnira.block.DreamCrystalMatrixBlock(stone().sound(SoundType.AMETHYST).lightLevel(s->10).randomTicks()));
    public static final DeferredBlock<Block> SMALL_DREAM_BUD=bud("small_dream_bud",3,6,10);
    public static final DeferredBlock<Block> MEDIUM_DREAM_BUD=bud("medium_dream_bud",5,5,10);
    public static final DeferredBlock<Block> LARGE_DREAM_BUD=bud("large_dream_bud",7,3,10);
    public static final DeferredBlock<Block> DREAM_CRYSTAL=block("dream_crystal",()->new AmethystClusterBlock(12,2,
            BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).requiresCorrectToolForDrops().lightLevel(s->10)));
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"omnira");
    public static final DeferredHolder<Feature<?>,com.mcmagic.omnira.time.TimeTreeFeature> TIME_TREE_FEATURE=FEATURES.register("time_tree",com.mcmagic.omnira.time.TimeTreeFeature::new);
    public static final DeferredHolder<Feature<?>,com.mcmagic.omnira.time.FleetingVegetationFeature> FLEETING_VEGETATION=FEATURES.register("fleeting_vegetation",com.mcmagic.omnira.time.FleetingVegetationFeature::new);
    public static final DeferredHolder<Feature<?>,com.mcmagic.omnira.time.RecurrenceVegetationFeature> RECURRENCE_VEGETATION=FEATURES.register("recurrence_vegetation",com.mcmagic.omnira.time.RecurrenceVegetationFeature::new);
    public static final DeferredHolder<Feature<?>,com.mcmagic.omnira.time.EventideRemnantsFeature> EVENTIDE_REMNANTS=FEATURES.register("eventide_remnants",com.mcmagic.omnira.time.EventideRemnantsFeature::new);
    public static final DeferredHolder<Feature<?>,com.mcmagic.omnira.spacetime.TimeWarpFeature> TIME_WARP_FEATURE=
            FEATURES.register("time_warp_point",com.mcmagic.omnira.spacetime.TimeWarpFeature::new);
    public static final DeferredHolder<Feature<?>,com.mcmagic.omnira.world.dimension.DreamLandscapeFeature> LANDSCAPE=
            FEATURES.register("dream_landscape",com.mcmagic.omnira.world.dimension.DreamLandscapeFeature::new);
    private static BlockBehaviour.Properties stone() {return BlockBehaviour.Properties.of().strength(2.5F,6).requiresCorrectToolForDrops().sound(SoundType.STONE);}
    private static DeferredBlock<Block> bud(String name,int height,int inset,int light) {
        return block(name,()->new AmethystClusterBlock(height,inset,BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).requiresCorrectToolForDrops().lightLevel(s->light)));
    }
    private static Block glass(int light) {return new TransparentBlock(stone().noOcclusion().sound(SoundType.AMETHYST)
            .lightLevel(s->light).isRedstoneConductor((s,l,p)->false).isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false));}
    private static DeferredBlock<Block> block(String name,Supplier<Block> factory) {
        var block=ModBlocks.BLOCKS.register(name,factory);
        var item=ModItems.ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));
        if(!name.equals("crystal_column_capital"))ITEMS.add(item);
        return block;
    }
    public static void register(IEventBus bus) {FEATURES.register(bus);}
    private DreamContent() {}
}

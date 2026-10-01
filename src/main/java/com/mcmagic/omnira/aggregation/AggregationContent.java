package com.mcmagic.omnira.aggregation;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.*;

public final class AggregationContent {
    public static final DeferredBlock<AggregationRingBlock> BLOCK=ModBlocks.BLOCKS.register("advanced_crystal_aggregation_ring",
            ()->new AggregationRingBlock(BlockBehaviour.Properties.of().strength(-1,3600000).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST).lightLevel(s->10)));
    public static final DeferredItem<AggregationRingItem> ITEM=ModItems.ITEMS.register("advanced_crystal_aggregation_ring",
            ()->new AggregationRingItem(BLOCK.get(),new Item.Properties()));
    public static final DeferredItem<Item> CRYSTAL=ModItems.ITEMS.registerItem("high_tier_magic_crystal",
            com.mcmagic.omnira.item.LowTierMagicCrystalItem::new,new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SPATIAL_BLOCK=ModBlocks.BLOCKS.register("spatial_crystal_block",
            ()->new net.minecraft.world.level.block.TransparentBlock(spatialProperties()));
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> SPATIAL_STAIRS=ModBlocks.BLOCKS.register("spatial_crystal_stairs",
            ()->new com.mcmagic.omnira.block.DecorativeStairBlock(SPATIAL_BLOCK.get().defaultBlockState(),spatialProperties()));
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> SPATIAL_SLAB=ModBlocks.BLOCKS.register("spatial_crystal_slab",
            ()->new net.minecraft.world.level.block.SlabBlock(spatialProperties()));
    public static final DeferredItem<BlockItem> SPATIAL_ITEM=ModItems.ITEMS.registerSimpleBlockItem(SPATIAL_BLOCK);
    public static final DeferredItem<BlockItem> SPATIAL_STAIR_ITEM=ModItems.ITEMS.registerSimpleBlockItem(SPATIAL_STAIRS);
    public static final DeferredItem<BlockItem> SPATIAL_SLAB_ITEM=ModItems.ITEMS.registerSimpleBlockItem(SPATIAL_SLAB);
    private static BlockBehaviour.Properties spatialProperties(){return BlockBehaviour.Properties.of().strength(2.5F,6)
            .requiresCorrectToolForDrops().noOcclusion().sound(SoundType.AMETHYST).lightLevel(s->5)
            .isRedstoneConductor((s,l,p)->false).isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false);}
    public static final java.util.function.Supplier<BlockEntityType<AggregationRingBlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES
            .register("advanced_crystal_aggregation_ring",()->BlockEntityType.Builder.of(AggregationRingBlockEntity::new,BLOCK.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<AggregationRingMenu>> MENU=ModMenuTypes.MENU_TYPES
            .register("advanced_crystal_aggregation_ring",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(AggregationRingMenu::new));
    public static void init(){}
    private AggregationContent(){}
}

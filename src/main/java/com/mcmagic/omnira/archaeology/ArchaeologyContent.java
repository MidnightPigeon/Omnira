package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ArchaeologyContent {
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>,net.minecraft.world.entity.EntityType<ChronalBlindfish>> BLINDFISH=ModEntityTypes.ENTITY_TYPES.register("chronal_blindfish",()->net.minecraft.world.entity.EntityType.Builder.of(ChronalBlindfish::new,net.minecraft.world.entity.MobCategory.WATER_AMBIENT).sized(.45F,.25F).clientTrackingRange(8).build("omnira:chronal_blindfish"));
    public static final DeferredItem<BlindfishItem> BLINDFISH_ITEM=ModItems.ITEMS.register("chronal_blindfish",()->new BlindfishItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(.1F).build())));
    public static final DeferredItem<MobBucketItem> BLINDFISH_BUCKET=ModItems.ITEMS.register("chronal_blindfish_bucket",()->new MobBucketItem(BLINDFISH.get(),com.mcmagic.omnira.mire.MireContent.SOURCE.get(),SoundEvents.BUCKET_EMPTY_FISH,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> BLINDFISH_EGG=ModItems.ITEMS.register("chronal_blindfish_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(BLINDFISH,0xD9ECD8,0x7299A1,new Item.Properties()));
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>,EpochalGrassFeature> GRASS_FEATURE=DreamContent.FEATURES.register("epochal_grass",EpochalGrassFeature::new);
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<MovingDrill>> MOVING_RIG=ModEntityTypes.ENTITY_TYPES.register("moving_drill",()->net.minecraft.world.entity.EntityType.Builder.<MovingDrill>of(MovingDrill::new,net.minecraft.world.entity.MobCategory.MISC).sized(3,2).clientTrackingRange(10).updateInterval(1).build("moving_drill"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<DrillSeat>> SEAT=ModEntityTypes.ENTITY_TYPES.register("drill_seat",()->net.minecraft.world.entity.EntityType.Builder.<DrillSeat>of(DrillSeat::new,net.minecraft.world.entity.MobCategory.MISC).sized(.1F,.1F).noSave().clientTrackingRange(8).build("drill_seat"));
    public static final DeferredBlock<ColoredFallingBlock> SAND=ModBlocks.BLOCKS.register("time_sand",()->new ColoredFallingBlock(new ColorRGBA(0xCCD8B7),BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));
    public static final DeferredBlock<TimeSandBlock> SUSPICIOUS=ModBlocks.BLOCKS.register("suspicious_time_sand",()->new TimeSandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SUSPICIOUS_SAND)));
    public static final DeferredBlock<Block> POTTERY=block("ancient_pottery_rubble",Blocks.TERRACOTTA);
    public static final DeferredBlock<Block> PHANTOM=block("phantom_emblem_rubble",Blocks.DEEPSLATE_TILES);
    public static final DeferredBlock<Block> UMBRELLA=block("umbrella_laboratory_rubble",Blocks.QUARTZ_BLOCK);
    public static final DeferredBlock<Block> CONTAINMENT=block("containment_facility_rubble",Blocks.GRAY_CONCRETE);
    public static final DeferredBlock<Block> FOSSIL=block("primordial_fossil_rock",Blocks.TUFF);
    public static final java.util.List<DeferredItem<EraAggregateItem>> AGGREGATES=java.util.stream.IntStream.range(0,5).mapToObj(i->ModItems.ITEMS.register(
            new String[]{"ancient_archaeology_aggregate","phantom_thieves_aggregate","umbrella_aggregate","foundation_aggregate","primordial_earth_aggregate"}[i],()->new EraAggregateItem(new Item.Properties(),i))).toList();
    public static final DeferredItem<Item> CALLING_CARD=curio("phantom_calling_card",ArchaeologyCurio.Kind.CALLING_CARD);
    public static final DeferredItem<Item> ENHANCED_CARD=curio("enhanced_phantom_calling_card",ArchaeologyCurio.Kind.ENHANCED_CARD);
    public static final DeferredItem<Item> D_CLASS_PASS=curio("d_class_credentials",ArchaeologyCurio.Kind.D);
    public static final DeferredItem<Item> C_CLASS_PASS=curio("c_class_credentials",ArchaeologyCurio.Kind.C);
    public static final DeferredItem<Item> B_CLASS_PASS=curio("b_class_credentials",ArchaeologyCurio.Kind.B);
    public static final DeferredItem<Item> A_CLASS_PASS=curio("a_class_credentials",ArchaeologyCurio.Kind.A);
    public static final DeferredItem<Item> JADE_RING=curio("scp_714",ArchaeologyCurio.Kind.JADE_RING);
    public static final DeferredItem<Item> LUST_RING=curio("ring_of_lust",ArchaeologyCurio.Kind.LUST_RING);
    public static final DeferredItem<Item> GREED_RING=curio("ring_of_greed",ArchaeologyCurio.Kind.GREED_RING);
    public static final DeferredItem<Item> DESIRE_CRYSTAL=ModItems.ITEMS.registerSimpleItem("desire_crystal",new Item.Properties().rarity(Rarity.UNCOMMON));
    private static DeferredItem<Item> curio(String id,ArchaeologyCurio.Kind kind){return ModItems.ITEMS.register(id,()->new ArchaeologyCurio(new Item.Properties().rarity(Rarity.UNCOMMON),kind));}
    public static final DeferredItem<Item> UNKNOWN_FOSSIL=ModItems.ITEMS.registerSimpleItem("unknown_organism_fossil");
    public static final DeferredItem<Item> UNKNOWN_EGG=ModItems.ITEMS.registerSimpleItem("unknown_egg");
    public static final DeferredItem<Item> DAMAGED_FACILITY=ModItems.ITEMS.registerSimpleItem("damaged_research_facility");
    public static final DeferredItem<SpacetimeBrushItem> BRUSH=ModItems.ITEMS.register("spacetime_archaeology_brush",()->new SpacetimeBrushItem(new Item.Properties().durability(128)));
    public static final DeferredBlock<HyperDrillBlock> DRILL=ModBlocks.BLOCKS.register("hyperdimensional_drill",()->new HyperDrillBlock(BlockBehaviour.Properties.of().strength(4,20).noOcclusion().sound(SoundType.METAL)));
    public static final DeferredItem<HyperDrillItem> DRILL_ITEM=ModItems.ITEMS.register("hyperdimensional_drill",()->new HyperDrillItem(DRILL.get(),new Item.Properties().stacksTo(1)));
    public static final java.util.function.Supplier<BlockEntityType<HyperDrillBlockEntity>> DRILL_ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("hyperdimensional_drill",()->BlockEntityType.Builder.of(HyperDrillBlockEntity::new,DRILL.get()).build(null));
    static {for(var b:java.util.List.of(SAND,SUSPICIOUS,POTTERY,PHANTOM,UMBRELLA,CONTAINMENT,FOSSIL))ModItems.ITEMS.registerSimpleBlockItem(b);}
    private static DeferredBlock<Block> block(String name,Block base){return ModBlocks.BLOCKS.register(name,()->new Block(BlockBehaviour.Properties.ofFullCopy(base)));}
    public static Block relic(int era){return switch(era){case 0->POTTERY.get();case 1->PHANTOM.get();case 2->UMBRELLA.get();case 3->CONTAINMENT.get();default->FOSSIL.get();};}
    public static void register(IEventBus bus){
        bus.addListener((net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event)->event.modify(BlockEntityType.BRUSHABLE_BLOCK,SUSPICIOUS.get()));
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(BLINDFISH.get(),ChronalBlindfish.createAttributes().build()));
    }
    private ArchaeologyContent(){}
}

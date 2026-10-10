package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.registries.*;

public final class VoidSeaContent {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:void_starsea"));
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES,"omnira");
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"omnira");
    public static final DeferredHolder<FluidType,FluidType> TYPE=TYPES.register("void_starflow",()->new FluidType(FluidType.Properties.create()
            .canSwim(true).canDrown(true).canExtinguish(true).supportsBoating(true).canConvertToSource(true).lightLevel(5)
            .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL)
            .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)){});
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> SOURCE=FLUIDS.register("void_starflow",()->new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> FLOWING=FLUIDS.register("flowing_void_starflow",()->new BaseFlowingFluid.Flowing(properties()));
    public static final DeferredBlock<LiquidBlock> LIQUID=ModBlocks.BLOCKS.register("void_starflow",()->new LiquidBlock(SOURCE.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(s->5)));
    public static final DeferredItem<BucketItem> BUCKET=ModItems.ITEMS.register("void_starflow_bucket",()->new BucketItem(SOURCE.get(),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final DeferredBlock<VoidFissureBlock> FISSURE=ModBlocks.BLOCKS.register("void_crevice",VoidFissureBlock::new);
    public static final DeferredItem<BlockItem> FISSURE_ITEM=ModItems.ITEMS.registerSimpleBlockItem(FISSURE);
    public static final DeferredHolder<EntityType<?>,EntityType<StarJellyfish>> JELLYFISH=ModEntityTypes.ENTITY_TYPES.register("star_jellyfish",()->EntityType.Builder.of(StarJellyfish::new,MobCategory.WATER_CREATURE).sized(.8F,1F).clientTrackingRange(10).build("omnira:star_jellyfish"));
    public static final DeferredHolder<EntityType<?>,EntityType<VoidManta>> MANTA=ModEntityTypes.ENTITY_TYPES.register("void_manta",()->EntityType.Builder.of(VoidManta::new,MobCategory.WATER_CREATURE).sized(2.8F,.65F).clientTrackingRange(12).build("omnira:void_manta"));
    public static final DeferredItem<Item> JELLY_ITEM=ModItems.ITEMS.register("star_jellyfish",()->new ChorusFruitItem(new Item.Properties()
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(.3F).alwaysEdible().build())));
    public static final DeferredItem<MobBucketItem> JELLY_BUCKET=ModItems.ITEMS.register("star_jellyfish_bucket",()->new MobBucketItem(JELLYFISH.get(),SOURCE.get(),SoundEvents.BUCKET_EMPTY_FISH,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> JELLY_EGG=ModItems.ITEMS.register("star_jellyfish_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(JELLYFISH,0x697AAA,0xC9FFFF,new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> MANTA_EGG=ModItems.ITEMS.register("void_manta_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(MANTA,0x343959,0x8DDFE8,new Item.Properties()));
    private static BaseFlowingFluid.Properties properties(){return new BaseFlowingFluid.Properties(TYPE,SOURCE,FLOWING).block(LIQUID).bucket(BUCKET).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);}
    public static boolean fluid(FluidState state){return state.getType()==SOURCE.get()||state.getType()==FLOWING.get();}
    public static void register(IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->{
        e.put(JELLYFISH.get(),StarJellyfish.attributes().build());e.put(MANTA.get(),VoidManta.attributes().build());
    });}
    private VoidSeaContent(){}
}

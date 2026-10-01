package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.registries.*;

public final class MireContent {
    public static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES,"omnira");
    public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"omnira");
    public static final DeferredHolder<FluidType,FluidType> TYPE=TYPES.register("decayed_timeflow",()->new FluidType(FluidType.Properties.create()
            .canSwim(true).canDrown(true).canExtinguish(true).supportsBoating(true).canConvertToSource(true)
            .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL)
            .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)){});
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> SOURCE=FLUIDS.register("decayed_timeflow",()->new BaseFlowingFluid.Source(fluidProperties()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> FLOWING=FLUIDS.register("flowing_decayed_timeflow",()->new BaseFlowingFluid.Flowing(fluidProperties()));
    public static final DeferredBlock<LiquidBlock> LIQUID=ModBlocks.BLOCKS.register("decayed_timeflow",()->new LiquidBlock(SOURCE.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final DeferredItem<BucketItem> BUCKET=ModItems.ITEMS.register("decayed_timeflow_bucket",()->new BucketItem(SOURCE.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredBlock<Block> SILT=ModBlocks.BLOCKS.register("rotted_temporal_silt",()->new Block(BlockBehaviour.Properties.of().strength(.8F).sound(SoundType.MUD)));
    public static final DeferredItem<BlockItem> SILT_ITEM=ModItems.ITEMS.registerSimpleBlockItem(SILT);
    public static final DeferredBlock<MireLilyBlock> DECAYED=ModBlocks.BLOCKS.register("decayed_rottenleaf_lily",()->new MireLilyBlock(false));
    public static final DeferredBlock<MireLilyBlock> REBORN=ModBlocks.BLOCKS.register("reborn_rottenleaf_lily",()->new MireLilyBlock(true));
    public static final DeferredItem<PlaceOnWaterBlockItem> DECAYED_ITEM=ModItems.ITEMS.register("decayed_rottenleaf_lily",()->new PlaceOnWaterBlockItem(DECAYED.get(),new Item.Properties()));
    public static final DeferredItem<PlaceOnWaterBlockItem> REBORN_ITEM=ModItems.ITEMS.register("reborn_rottenleaf_lily",()->new PlaceOnWaterBlockItem(REBORN.get(),new Item.Properties()));
    public static final DeferredHolder<EntityType<?>,EntityType<TimeflowEel>> EEL=ModEntityTypes.ENTITY_TYPES.register("timeflow_eel",()->EntityType.Builder.of(TimeflowEel::new,MobCategory.WATER_AMBIENT).sized(.45F,.25F).clientTrackingRange(8).build("omnira:timeflow_eel"));
    public static final DeferredItem<Item> EEL_ITEM=ModItems.ITEMS.registerSimpleItem("timeflow_eel",new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(.1F).effect(()->new net.minecraft.world.effect.MobEffectInstance(ModEffects.TIME_PASSAGE,200),1F).build()));
    public static final DeferredItem<MobBucketItem> EEL_BUCKET=ModItems.ITEMS.register("timeflow_eel_bucket",()->new MobBucketItem(EEL.get(),SOURCE.get(),SoundEvents.BUCKET_EMPTY_FISH,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> EGG=ModItems.ITEMS.register("timeflow_eel_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(EEL,0x54644C,0xBED6A6,new Item.Properties()));
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>,MireVegetationFeature> VEGETATION=DreamContent.FEATURES.register("mire_vegetation",MireVegetationFeature::new);
    private static BaseFlowingFluid.Properties fluidProperties(){return new BaseFlowingFluid.Properties(TYPE,SOURCE,FLOWING).bucket(BUCKET).block(LIQUID).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);}
    public static boolean fluid(net.minecraft.world.level.material.FluidState state){return state.getType()==SOURCE.get()||state.getType()==FLOWING.get();}
    public static void register(IEventBus bus){
        TYPES.register(bus);FLUIDS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->e.put(EEL.get(),TimeflowEel.createAttributes().build()));
    }
}

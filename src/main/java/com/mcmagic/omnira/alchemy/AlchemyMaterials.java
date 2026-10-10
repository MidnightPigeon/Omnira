package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.registries.*;

public final class AlchemyMaterials {
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES,"omnira");
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"omnira");
    public static final Liquid DILUTE=new Liquid("dilute_spacetime",true);
    public static final Liquid REFINED=new Liquid("refined_spacetime",false);
    public static final DeferredBlock<EnhancedAlchemicalEnergyCoreBlock> ENHANCED_CORE=ModBlocks.BLOCKS.register("enhanced_alchemical_energy_core",()->
            new EnhancedAlchemicalEnergyCoreBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.ROUGH_ALCHEMICAL_ENERGY_CORE.get())));
    public static final DeferredItem<BlockItem> ENHANCED_CORE_ITEM=ModItems.ITEMS.registerSimpleBlockItem(ENHANCED_CORE);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<EnhancedAlchemicalEnergyCoreBlock.Core>> CORE_ENTITY=
            ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("enhanced_alchemical_energy_core",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    EnhancedAlchemicalEnergyCoreBlock.Core::new,ENHANCED_CORE.get()).build(null));
    public static final DeferredItem<AlchemicalWasteItem> WASTE=ModItems.ITEMS.register("alchemical_waste",()->new AlchemicalWasteItem(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>,EntityType<ThrownAlchemicalWaste>> THROWN_WASTE=ModEntityTypes.ENTITY_TYPES.register("alchemical_waste",()->
            EntityType.Builder.<ThrownAlchemicalWaste>of(ThrownAlchemicalWaste::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(4).updateInterval(10).build("omnira:alchemical_waste"));

    public static final class Liquid {
        public final String id;
        public final DeferredHolder<FluidType,FluidType> type;
        public final DeferredHolder<Fluid,BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> flowing;
        public final DeferredBlock<LiquidBlock> block;
        public final DeferredItem<BucketItem> bucket;
        private Liquid(String id,boolean waterlike){
            this.id=id;
            type=TYPES.register(id,()->new FluidType(FluidType.Properties.create().canSwim(true).canDrown(true)
                    .canExtinguish(waterlike).supportsBoating(waterlike).canConvertToSource(waterlike).lightLevel(5)
                    .sound(SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)){});
            source=FLUIDS.register(id,()->new BaseFlowingFluid.Source(properties()));
            flowing=FLUIDS.register("flowing_"+id,()->new BaseFlowingFluid.Flowing(properties()));
            block=ModBlocks.BLOCKS.register(id,()->new LiquidBlock(source.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(s->5)));
            bucket=ModItems.ITEMS.register(id+"_bucket",()->new BucketItem(source.get(),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        }
        private BaseFlowingFluid.Properties properties(){return new BaseFlowingFluid.Properties(type,source,flowing)
                .block(block).bucket(bucket).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);}
    }
    public static void register(IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);}
    private AlchemyMaterials(){}
}

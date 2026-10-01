package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity;
import com.mcmagic.omnira.block.entity.AnalysisArtisanTableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Omnira.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.time.WonderlandPokerStandBlockEntity>> WONDERLAND_POKER_STAND=
            BLOCK_ENTITY_TYPES.register("wonderland_poker_stand",()->BlockEntityType.Builder.of(com.mcmagic.omnira.time.WonderlandPokerStandBlockEntity::new,
                    ModBlocks.WONDERLAND_POKER_STAND.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.mire.MemoryCubeBlockEntity>> MEMORY_CUBE=
            BLOCK_ENTITY_TYPES.register("memory_cube",()->BlockEntityType.Builder.of(com.mcmagic.omnira.mire.MemoryCubeBlockEntity::new,ModBlocks.MEMORY_CUBE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.time.TimePlantBlockEntity>> TIME_PLANT=
            BLOCK_ENTITY_TYPES.register("time_plant",()->BlockEntityType.Builder.of(com.mcmagic.omnira.time.TimePlantBlockEntity::new,
                    TimeNatureContent.GRASS.get(),TimeNatureContent.FLOWER.get(),com.mcmagic.omnira.mire.MireContent.DECAYED.get(),com.mcmagic.omnira.mire.MireContent.REBORN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.shop.MarisaOrbBlockEntity>> MARISA_ORB=
            BLOCK_ENTITY_TYPES.register("marisa_crystal_ball",()->BlockEntityType.Builder.of(com.mcmagic.omnira.shop.MarisaOrbBlockEntity::new,
                    com.mcmagic.omnira.shop.KirisameContent.ORB.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.spacetime.TimeWarpPointBlockEntity>> TIME_WARP_POINT=
            BLOCK_ENTITY_TYPES.register("time_warp_point",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.spacetime.TimeWarpPointBlockEntity::new,ModBlocks.TIME_WARP_POINT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.spacetime.CorridorGatewayBlockEntity>> CORRIDOR_GATEWAY=
            BLOCK_ENTITY_TYPES.register("corridor_gateway",()->BlockEntityType.Builder.of(com.mcmagic.omnira.spacetime.CorridorGatewayBlockEntity::new,ModBlocks.CORRIDOR_GATEWAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.forging.AdvancedForgeBlockEntity>> ADVANCED_FORGE=
            BLOCK_ENTITY_TYPES.register("advanced_assembly_table",()->BlockEntityType.Builder.of(com.mcmagic.omnira.forging.AdvancedForgeBlockEntity::new,ModBlocks.ADVANCED_FORGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity>> RESONANCE_CORE=
            BLOCK_ENTITY_TYPES.register("resonance_core",()->BlockEntityType.Builder.of(com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity::new,ModBlocks.RESONANCE_CORE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity>> LIQUID_CRYSTAL_BALL=
            BLOCK_ENTITY_TYPES.register("liquid_crystal_ball",()->BlockEntityType.Builder.of(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity::new,ModBlocks.LIQUID_CRYSTAL_BALL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.SpellCoreBlockEntity>> SPELL_CORE=
            BLOCK_ENTITY_TYPES.register("spell_core",()->BlockEntityType.Builder.of(com.mcmagic.omnira.block.entity.SpellCoreBlockEntity::new,
                    ModBlocks.TEST_SPELL_CORE.get(),ModBlocks.DREAM_SPELL_CORE.get(),ModBlocks.LIGHT_DARK_SPELL_CORE.get(),ModBlocks.SPACETIME_SPELL_CORE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.NightHeronStatueBlockEntity>> NIGHT_HERON_STATUE=
            BLOCK_ENTITY_TYPES.register("night_heron_statue",()->BlockEntityType.Builder.of(com.mcmagic.omnira.block.entity.NightHeronStatueBlockEntity::new,ModBlocks.NIGHT_HERON_STATUE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.PureVesselBlockEntity>> PURE_VESSEL=
            BLOCK_ENTITY_TYPES.register("pure_vessel",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.PureVesselBlockEntity::new,ModBlocks.PURE_VESSEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.CrystalBallBlockEntity>> CRYSTAL_BALL=
            BLOCK_ENTITY_TYPES.register("crystal_ball",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.CrystalBallBlockEntity::new,ModBlocks.CRYSTAL_BALL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.AdvancedCondensationTableBlockEntity>> ADVANCED_CONDENSATION_TABLE=
            BLOCK_ENTITY_TYPES.register("advanced_condensation_table",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.AdvancedCondensationTableBlockEntity::new,ModBlocks.ADVANCED_CONDENSATION_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.WaymarkBlockEntity>> WAYMARK=
            BLOCK_ENTITY_TYPES.register("spacetime_waymark",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.WaymarkBlockEntity::new,ModBlocks.WAYMARK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.RitualEnergyCoreBlockEntity>> RITUAL_ENERGY_CORE=
            BLOCK_ENTITY_TYPES.register("ritual_energy_core",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.RitualEnergyCoreBlockEntity::new,ModBlocks.RITUAL_ENERGY_CORE.get(),ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalProcessingTableBlockEntity>> CRYSTAL_PROCESSING_TABLE =
            BLOCK_ENTITY_TYPES.register("crystal_processing_table", () -> BlockEntityType.Builder.of(
                    CrystalProcessingTableBlockEntity::new,
                    ModBlocks.CRYSTAL_PROCESSING_TABLE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity>> SIMPLE_CONDENSATION_TABLE=
            BLOCK_ENTITY_TYPES.register("simple_condensation_table",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity::new,ModBlocks.SIMPLE_CONDENSATION_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity>> ARCANE_ASSEMBLY_TABLE=
            BLOCK_ENTITY_TYPES.register("arcane_assembly_table",()->BlockEntityType.Builder.of(
                    (net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state)->
                            net.neoforged.fml.ModList.get().isLoaded("create")
                            ?com.mcmagic.omnira.compat.CreateAssemblyFactory.entity(pos,state)
                            :new com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity(pos,state),ModBlocks.ARCANE_ASSEMBLY_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity>> VOID_CRYSTAL=
            BLOCK_ENTITY_TYPES.register("void_crystal",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity::new,ModBlocks.VOID_CRYSTAL.get(),ModBlocks.REINFORCED_VOID_CRYSTAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity>> CRYSTAL_PEDESTAL=
            BLOCK_ENTITY_TYPES.register("crystal_pedestal",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity::new,ModBlocks.CRYSTAL_PEDESTAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.mcmagic.omnira.block.entity.DreamPortalBlockEntity>> DREAM_PORTAL=
            BLOCK_ENTITY_TYPES.register("dream_portal",()->BlockEntityType.Builder.of(
                    com.mcmagic.omnira.block.entity.DreamPortalBlockEntity::new,ModBlocks.DREAM_PORTAL.get()).build(null));
    private ModBlockEntityTypes() {
    }

    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity>> MANA_ENGINE=
            BLOCK_ENTITY_TYPES.register("mana_engine",()->BlockEntityType.Builder.of((net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state)->
                    net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.CreateEngineFactory.entity(pos,state):
                    new com.mcmagic.omnira.block.entity.DecorativeManaEngineBlockEntity(pos,state),ModBlocks.MANA_ENGINE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnalysisArtisanTableBlockEntity>> ANALYSIS_ARTISAN_TABLE =
            BLOCK_ENTITY_TYPES.register("analysis_artisan_table", () -> BlockEntityType.Builder.of(
                    AnalysisArtisanTableBlockEntity::new, ModBlocks.ANALYSIS_ARTISAN_TABLE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
        eventBus.addListener(ModBlockEntityTypes::registerCapabilities);
    }
    private static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,LIQUID_CRYSTAL_BALL.get(),(ball,side)->ball.fluidHandler);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,CRYSTAL_BALL.get(),(ball,side)->ball.itemHandler);
    }
}

package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.block.CrystalProcessingTableBlock;
import com.mcmagic.omnira.block.AnalysisArtisanTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final net.minecraft.resources.ResourceLocation RESONANCE_CORE_ID=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","resonance_core");
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Omnira.MOD_ID);
    public static final DeferredBlock<Block> WONDERLAND_POKER_STAND=BLOCKS.register("wonderland_poker_stand",()->new com.mcmagic.omnira.time.WonderlandPokerStandBlock(
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(state->15)));
    public static final DeferredBlock<Block> MEMORY_CUBE=BLOCKS.register("memory_cube",()->new com.mcmagic.omnira.block.MemoryCubeBlock(
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST)
                    .lightLevel(s->s.getValue(com.mcmagic.omnira.block.MemoryCubeBlock.PEACEFUL)?6:3).noOcclusion().noLootTable()));
    public static final DeferredBlock<Block> TIME_WARP_POINT=BLOCKS.register("time_warp_point",()->new com.mcmagic.omnira.spacetime.TimeWarpPointBlock(
            BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).lightLevel(s->8)));
    public static final DeferredBlock<Block> TEMPORAL_MOTE=BLOCKS.register("temporal_mote",()->new com.mcmagic.omnira.spacetime.TemporalMoteBlock(
            BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).lightLevel(s->3)));
    public static final DeferredBlock<Block> GOLDEN_THRONE=BLOCKS.register("golden_throne",()->new com.mcmagic.omnira.block.GoldenThroneBlock(
            BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Block> GOLDEN_TOILET=BLOCKS.register("golden_toilet",()->new com.mcmagic.omnira.block.GoldenToiletBlock(
            BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Block> PURE_SOLIDIFIED_SPACETIME=BLOCKS.register("pure_solidified_spacetime",()->new Block(
            BlockBehaviour.Properties.of().strength(-1,3600000).noLootTable().sound(SoundType.AMETHYST)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static final DeferredBlock<Block> CORRIDOR_GATEWAY=BLOCKS.register("corridor_gateway",()->new com.mcmagic.omnira.spacetime.CorridorGatewayBlock(
            BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).lightLevel(s->12)));
    public static final DeferredBlock<Block> SPACETIME_RIFT=BLOCKS.register("spacetime_rift",()->new com.mcmagic.omnira.spacetime.SpacetimeRiftBlock(
            BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable().lightLevel(s->12)));
    public static final DeferredBlock<Block> ADVANCED_FORGE=BLOCKS.register("advanced_assembly_table",()->new com.mcmagic.omnira.forging.AdvancedForgeBlock(
            BlockBehaviour.Properties.of().strength(-1F,3600000F).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                    .sound(SoundType.AMETHYST).noOcclusion().noLootTable().lightLevel(s->10)));
    public static final DeferredBlock<Block> LIQUID_CRYSTAL_BALL=BLOCKS.register("liquid_crystal_ball",()->new com.mcmagic.omnira.block.CrystalBallBlock(
            BlockBehaviour.Properties.of().strength(2.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->12).noOcclusion()));
    public static final DeferredBlock<Block> TEST_SPELL_CORE=BLOCKS.register("primordial_spell_core",()->new com.mcmagic.omnira.block.SpellCoreBlock(
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> LIGHT_DARK_SPELL_CORE=BLOCKS.register("light_dark_spell_core",()->new com.mcmagic.omnira.block.SpellCoreBlock(
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> DREAM_SPELL_CORE=BLOCKS.register("dream_spell_core",()->new com.mcmagic.omnira.block.SpellCoreBlock(
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> SPACETIME_SPELL_CORE=BLOCKS.register("spacetime_spell_core",()->new com.mcmagic.omnira.block.SpellCoreBlock(
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST).lightLevel(s->12).noOcclusion()));
    public static final DeferredBlock<Block> NIGHT_HERON_STATUE=BLOCKS.register("night_heron_statue",()->new com.mcmagic.omnira.block.NightHeronStatueBlock(
            BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> PURE_VESSEL=BLOCKS.register("pure_vessel",()->new com.mcmagic.omnira.block.PureVesselBlock(
            BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> RESONANCE_CORE=BLOCKS.register("resonance_core",()->new com.mcmagic.omnira.block.ResonanceCoreBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s->12)));
    public static final DeferredBlock<Block> ADVANCED_CONDENSATION_TABLE=BLOCKS.register("advanced_condensation_table",()->
            new com.mcmagic.omnira.block.AdvancedCondensationTableBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> CRYSTAL_BALL=BLOCKS.register("crystal_ball",()->
            new com.mcmagic.omnira.block.CrystalBallBlock(BlockBehaviour.Properties.of().strength(2.5F).requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST).lightLevel(s->12).noOcclusion().isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false)));
    public static final DeferredBlock<Block> WAYMARK=BLOCKS.register("spacetime_waymark",()->
            new com.mcmagic.omnira.block.WaymarkBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> ADVANCED_RITUAL_ENERGY_CORE=BLOCKS.register("advanced_ritual_energy_core",()->
            new com.mcmagic.omnira.block.RitualEnergyCoreBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> RITUAL_ENERGY_CORE=BLOCKS.register("ritual_energy_core",()->
            new com.mcmagic.omnira.block.RitualEnergyCoreBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> CRYSTAL_PEDESTAL=BLOCKS.register("crystal_pedestal",()->
            new com.mcmagic.omnira.block.CrystalPedestalBlock(BlockBehaviour.Properties.of().strength(2.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> DREAM_PORTAL=BLOCKS.register("dream_portal",()->
            new com.mcmagic.omnira.block.DreamPortalBlock(BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable().lightLevel(state->9)));
    public static final DeferredBlock<Block> PERMANENT_VOID_CRYSTAL=BLOCKS.register("permanent_void_crystal",()->
            new com.mcmagic.omnira.block.PermanentVoidCrystalBlock(BlockBehaviour.Properties.of().strength(3,6).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> PERMANENT_REINFORCED_VOID_CRYSTAL=BLOCKS.register("permanent_reinforced_void_crystal",()->
            new com.mcmagic.omnira.block.PermanentVoidCrystalBlock(BlockBehaviour.Properties.of().strength(5,1200).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion()));
    public static final DeferredBlock<Block> CRYSTAL_CASING=BLOCKS.register("crystal_casing",()->
            new com.mcmagic.omnira.block.CrystalCasingBlock(BlockBehaviour.Properties.of().strength(1.5F).requiresCorrectToolForDrops().sound(SoundType.GLASS).lightLevel(s->10)
                    .noOcclusion().isRedstoneConductor((state,level,pos)->false).isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos)->false)));
    public static final DeferredBlock<Block> INFUSED_CRYSTAL_CASING=BLOCKS.register("infused_crystal_casing",()->
            new com.mcmagic.omnira.block.CrystalCasingBlock(BlockBehaviour.Properties.ofFullCopy(CRYSTAL_CASING.get())));

    public static final DeferredBlock<Block> MANA_ENGINE=BLOCKS.register("mana_engine",()->{
        var properties=BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(state->10).noOcclusion();
        return net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.CreateEngineFactory.block(properties):
                new com.mcmagic.omnira.block.DecorativeManaEngineBlock(properties);
    });

    public static final DeferredBlock<Block> CRYSTAL_PROCESSING_TABLE = BLOCKS.register(
            "crystal_processing_table",
            () -> new CrystalProcessingTableBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> CrystalProcessingTableBlock.LIGHT_LEVEL)
                    .noOcclusion())
    );

    public static final DeferredBlock<Block> SIMPLE_CONDENSATION_TABLE=BLOCKS.register("simple_condensation_table",
            ()->new com.mcmagic.omnira.block.SimpleCondensationTableBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<Block> ARCANE_ASSEMBLY_TABLE=BLOCKS.register("arcane_assembly_table",
            ()->{
                var properties=BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                        .lightLevel(state->CrystalProcessingTableBlock.LIGHT_LEVEL).noOcclusion();
                return net.neoforged.fml.ModList.get().isLoaded("create")
                        ?com.mcmagic.omnira.compat.CreateAssemblyFactory.block(properties)
                        :new com.mcmagic.omnira.block.ArcaneAssemblyTableBlock(properties);
            });

    public static final DeferredBlock<Block> VOID_CRYSTAL=BLOCKS.register("void_crystal",
            ()->new com.mcmagic.omnira.block.VoidCrystalBlock(BlockBehaviour.Properties.of().strength(3,6).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion().isSuffocating((s,l,p)->false)));
    public static final DeferredBlock<Block> REINFORCED_VOID_CRYSTAL=BLOCKS.register("reinforced_void_crystal",
            ()->new com.mcmagic.omnira.block.VoidCrystalBlock(BlockBehaviour.Properties.of().strength(5,1200).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s->10).noOcclusion().isSuffocating((s,l,p)->false)));
    private ModBlocks() {
    }

    public static final DeferredBlock<Block> ANALYSIS_ARTISAN_TABLE = BLOCKS.register("analysis_artisan_table",
            () -> new AnalysisArtisanTableBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST).lightLevel(state -> CrystalProcessingTableBlock.LIGHT_LEVEL).noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

package com.mcmagic.omnira;

import com.mojang.logging.LogUtils;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mcmagic.omnira.registry.ModCreativeModeTabs;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModRecipes;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(Omnira.MOD_ID)
public final class Omnira {
    public static final String MOD_ID = "omnira";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Omnira(IEventBus modEventBus, ModContainer modContainer) {
        net.neoforged.neoforge.common.NeoForgeMod.enableMilkFluid();
        modEventBus.addListener(this::commonSetup);
        if(net.neoforged.fml.ModList.get().isLoaded("create"))
            com.mcmagic.omnira.compat.CrystalEncasing.register(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,com.mcmagic.omnira.config.OmniraConfig.SPEC,"omnira-common.toml");

        com.mcmagic.omnira.registry.ModAttributes.TYPES.register(modEventBus);
        com.mcmagic.omnira.registry.DreamContent.register(modEventBus);
        com.mcmagic.omnira.world.dimension.ModDimensions.GENERATORS.register(modEventBus);
        com.mcmagic.omnira.world.dimension.ModDimensions.BIOME_SOURCES.register(modEventBus);
        com.mcmagic.omnira.world.structure.RuinsStructure.register(modEventBus);
        com.mcmagic.omnira.registry.ShadowWoodContent.init();
        com.mcmagic.omnira.registry.TimeNatureContent.init();
        com.mcmagic.omnira.time.EventideMasonry.init();
        com.mcmagic.omnira.time.SundialContent.init();
        com.mcmagic.omnira.aggregation.AggregationContent.init();
        com.mcmagic.omnira.mire.MireContent.register(modEventBus);
        com.mcmagic.omnira.archaeology.ArchaeologyContent.register(modEventBus);
        com.mcmagic.omnira.reversal.ReversalContent.register(modEventBus);
        com.mcmagic.omnira.shop.KirisameContent.init();
        com.mcmagic.omnira.registry.DecorationVariants.init();
        com.mcmagic.omnira.registry.ModRegistryAliases.install();
        ModBlocks.register(modEventBus);
        com.mcmagic.omnira.registry.ModSounds.TYPES.register(modEventBus);
        com.mcmagic.omnira.registry.ModEffects.EFFECTS.register(modEventBus);
        com.mcmagic.omnira.mire.TimePassage.POTIONS.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        com.mcmagic.omnira.registry.ArmorContent.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        com.mcmagic.omnira.registry.ModParticles.TYPES.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if(net.neoforged.fml.ModList.get().isLoaded("create"))event.enqueueWork(com.mcmagic.omnira.compat.ForgeMovement::register);
        event.enqueueWork(com.mcmagic.omnira.registry.ShadowWoodContent::flammability);
        event.enqueueWork(com.mcmagic.omnira.registry.TimeNatureContent::flammability);
        LOGGER.info("Omnira common setup complete.");
    }
}

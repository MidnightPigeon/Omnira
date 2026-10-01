package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import com.mcmagic.omnira.menu.AnalysisArtisanTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Omnira.MOD_ID);
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.forging.AdvancedForgeMenu>> ADVANCED_FORGE=
            MENU_TYPES.register("advanced_assembly_table",()->IMenuTypeExtension.create(com.mcmagic.omnira.forging.AdvancedForgeMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.vehicle.CruiseOrbMenu>> CRUISE_ORB=
            MENU_TYPES.register("cruise_orb",()->IMenuTypeExtension.create(com.mcmagic.omnira.vehicle.CruiseOrbMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.ResonanceMenu>> RESONANCE=
            MENU_TYPES.register("resonance",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.ResonanceMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.OrbUpgradeMenu>> ORB_UPGRADE=
            MENU_TYPES.register("orb_upgrade",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.OrbUpgradeMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.ArquebusMenu>> ARQUEBUS=
            MENU_TYPES.register("arquebus",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.ArquebusMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.ArquebusMenu>> SWORD_SPELL=
            MENU_TYPES.register("sword_spell",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.ArquebusMenu::sword));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.CrystalBallMenu>> CRYSTAL_BALL=
            MENU_TYPES.register("crystal_ball",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.CrystalBallMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.WaymarkMenu>> WAYMARK=
            MENU_TYPES.register("waymark",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.WaymarkMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrystalProcessingTableMenu>> CRYSTAL_PROCESSING_TABLE =
            MENU_TYPES.register("crystal_processing_table", () -> IMenuTypeExtension.create(CrystalProcessingTableMenu::new));

    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.CrystalGridMenu>> CRYSTAL_GRID =
            MENU_TYPES.register("crystal_grid",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.CrystalGridMenu::new));

    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.SimpleCondensationTableMenu>> SIMPLE_CONDENSATION_TABLE=
            MENU_TYPES.register("simple_condensation_table",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.SimpleCondensationTableMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.SimpleCondensationTableMenu>> ADVANCED_CONDENSATION_TABLE=
            MENU_TYPES.register("advanced_condensation_table",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.SimpleCondensationTableMenu::advanced));
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu>> ARCANE_ASSEMBLY_TABLE=
            MENU_TYPES.register("arcane_assembly_table",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu::new));
    private ModMenuTypes() {
    }
    public static final DeferredHolder<MenuType<?>,MenuType<com.mcmagic.omnira.menu.ManaEngineMenu>> MANA_ENGINE=
            MENU_TYPES.register("mana_engine",()->IMenuTypeExtension.create(com.mcmagic.omnira.menu.ManaEngineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AnalysisArtisanTableMenu>> ANALYSIS_ARTISAN_TABLE =
            MENU_TYPES.register("analysis_artisan_table", () -> IMenuTypeExtension.create(AnalysisArtisanTableMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}

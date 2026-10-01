package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.item.LowTierMagicCrystalItem;
import com.mcmagic.omnira.item.PrimaryMicrocoreItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Omnira.MOD_ID);
    public static final DeferredItem<BlockItem> WONDERLAND_POKER_STAND=ITEMS.registerSimpleBlockItem(ModBlocks.WONDERLAND_POKER_STAND);
    public static final DeferredItem<Item> DREAM_RABBIT_REMAINS=ITEMS.register("dream_rabbit_remains",()->new com.mcmagic.omnira.dream.DreamRabbitItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),false));
    public static final DeferredItem<Item> ALICE_DREAM_RABBIT_SIGIL=ITEMS.register("alice_dream_rabbit_sigil",()->new com.mcmagic.omnira.dream.DreamRabbitItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE),true));
    public static final DeferredItem<Item> SPACETIME_ANCHORING_SIGIL=ITEMS.registerItem("spacetime_anchoring_sigil",com.mcmagic.omnira.spacetime.AnchoringSigilItem::new);
    public static final DeferredItem<BlockItem> MEMORY_CUBE=ITEMS.register("memory_cube",()->new com.mcmagic.omnira.item.MemoryCubeBlockItem(ModBlocks.MEMORY_CUBE.get(),new Item.Properties()));
    public static final DeferredItem<Item> PEACEFUL_MEMORY=ITEMS.register("peaceful_memory_cube",()->new com.mcmagic.omnira.item.MemoryCubeItem(new Item.Properties(),true));
    public static final DeferredItem<Item> CORRUPTED_MEMORY=ITEMS.register("corrupted_memory_cube",()->new com.mcmagic.omnira.item.MemoryCubeItem(new Item.Properties(),false));
    public static final DeferredItem<Item> TIME_WARP_POINT=ITEMS.registerSimpleItem("time_warp_point",new Item.Properties().stacksTo(16));
    public static final DeferredItem<BlockItem> GOLDEN_THRONE=ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_THRONE);
    public static final DeferredItem<BlockItem> GOLDEN_TOILET=ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_TOILET);
    public static final DeferredItem<BlockItem> PURE_SOLIDIFIED_SPACETIME=ITEMS.registerSimpleBlockItem(ModBlocks.PURE_SOLIDIFIED_SPACETIME);
    public static final DeferredItem<Item> UNSTABLE_SPACETIME_AGGREGATE=ITEMS.registerItem("unstable_spacetime_aggregate",com.mcmagic.omnira.spacetime.UnstableAggregateItem::new,new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> ADVANCED_FORGE=ITEMS.register("advanced_assembly_table",()->new com.mcmagic.omnira.forging.AdvancedForgeItem(new Item.Properties()));
    public static final DeferredItem<BlockItem> RESONANCE_CORE=ITEMS.registerSimpleBlockItem(ModBlocks.RESONANCE_CORE);
    public static final DeferredItem<Item> RESONANCE_TERMINAL=ITEMS.registerItem("resonance_terminal",com.mcmagic.omnira.item.ResonanceTerminalItem::new);
    public static final DeferredItem<Item> ENHANCED_RESONANCE_TERMINAL=ITEMS.registerItem("enhanced_resonance_terminal",p->new com.mcmagic.omnira.item.ResonanceTerminalItem(p,6));
    public static final DeferredItem<Item> CRUISE_ORB=ITEMS.registerItem("cruise_orb",com.mcmagic.omnira.vehicle.CruiseOrbItem::new,new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> CRYSTAL_BROOM=ITEMS.registerItem("crystal_broom",com.mcmagic.omnira.vehicle.CrystalBroomItem::new);
    public static final DeferredItem<BlockItem> LIQUID_CRYSTAL_BALL=ITEMS.register("liquid_crystal_ball",()->new com.mcmagic.omnira.item.PortableOrbBlockItem(ModBlocks.LIQUID_CRYSTAL_BALL.get(),new Item.Properties()));
    public static final DeferredItem<Item> BASIC_STACKING_UPGRADE=upgrade("basic_stacking_upgrade",com.mcmagic.omnira.item.OrbUpgradeItem.Kind.STACKING);
    public static final DeferredItem<Item> INFUSED_STACKING_UPGRADE=upgrade("infused_stacking_upgrade",com.mcmagic.omnira.item.OrbUpgradeItem.Kind.INFUSED_STACKING);
    public static final DeferredItem<Item> SPACETIME_STABILIZATION_UPGRADE=upgrade("spacetime_stabilization_upgrade",com.mcmagic.omnira.item.OrbUpgradeItem.Kind.STABILIZATION);
    public static final DeferredItem<Item> LAVA_PRODUCTION_UPGRADE=upgrade("lava_production_upgrade",com.mcmagic.omnira.item.OrbUpgradeItem.Kind.LAVA);
    public static final DeferredItem<Item> INTAKE_UPGRADE=upgrade("intake_upgrade",com.mcmagic.omnira.item.OrbUpgradeItem.Kind.INTAKE);
    public static final DeferredItem<Item> OUTPUT_UPGRADE=upgrade("output_upgrade",com.mcmagic.omnira.item.OrbUpgradeItem.Kind.OUTPUT);
    public static final DeferredItem<Item> SPEED_UPGRADE=upgrade("speed_upgrade",com.mcmagic.omnira.item.OrbUpgradeItem.Kind.SPEED);
    private static DeferredItem<Item> upgrade(String name,com.mcmagic.omnira.item.OrbUpgradeItem.Kind kind) {
        return ITEMS.register(name,()->new com.mcmagic.omnira.item.OrbUpgradeItem(new Item.Properties(),kind));
    }
    public static final DeferredItem<BlockItem> NIGHT_HERON_STATUE=ITEMS.registerSimpleBlockItem(ModBlocks.NIGHT_HERON_STATUE);
    public static final DeferredItem<Item> LIFE_ENDER=ITEMS.registerItem("life_ender",com.mcmagic.omnira.item.LifeEnderItem::new);
    public static final DeferredItem<Item> CRYSTALLIZED_NAIL=ITEMS.register("crystallized_bone_nail",()->new com.mcmagic.omnira.item.RitualSwordItem(new Item.Properties(),com.mcmagic.omnira.item.RitualSwordItem.Kind.NAIL));
    public static final DeferredItem<Item> ARCANE_NEEDLE=ITEMS.register("arcane_needle",()->new com.mcmagic.omnira.item.RitualSwordItem(new Item.Properties(),com.mcmagic.omnira.item.RitualSwordItem.Kind.NEEDLE));
    public static final DeferredItem<BlockItem> ADVANCED_CONDENSATION_TABLE=ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_CONDENSATION_TABLE);
    public static final DeferredItem<BlockItem> PURE_VESSEL=ITEMS.registerSimpleBlockItem(ModBlocks.PURE_VESSEL);
    public static final DeferredItem<Item> PARADOX_DUST=ITEMS.registerSimpleItem("paradox_dust");
    public static final DeferredItem<Item> ARCANE_ARQUEBUS=ITEMS.registerItem("arcane_arquebus",com.mcmagic.omnira.item.ArcaneArquebusItem::new,new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> ANCESTOR_LAUNCHER=ITEMS.registerSimpleItem("ancestor_launcher",new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> KINGS_NEW_CLOTHES=ITEMS.registerSimpleItem("kings_new_clothes",new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> DREAM_MIRROR_SPAWN_EGG=ITEMS.register("dream_mirror_spawn_egg",()->
            new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntityTypes.DREAM_MIRROR,0x749DCF,0xB498D8,new Item.Properties()));
    public static final DeferredItem<Item> SHADOW_GHOST_SPAWN_EGG = ITEMS.register("shadow_ghost_spawn_egg", () ->
            new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntityTypes.SHADOW_GHOST,0xBCA4DC,0x685086,new Item.Properties()));
    public static final DeferredItem<Item> LIGHT_SPIRIT_SPAWN_EGG = ITEMS.register("light_spirit_spawn_egg", () ->
            new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntityTypes.LIGHT_SPIRIT, 0xF4E8A6, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<Item> CRYSTAL_PICKAXE=ITEMS.register("crystal_pickaxe",()->new com.mcmagic.omnira.item.CrystalPickaxeItem(new Item.Properties(),false));
    public static final DeferredItem<Item> INFUSED_CRYSTAL_PICKAXE=ITEMS.register("infused_crystal_pickaxe",()->new com.mcmagic.omnira.item.CrystalPickaxeItem(new Item.Properties().rarity(Rarity.UNCOMMON),true));
    public static final DeferredItem<BlockItem> WAYMARK=ITEMS.registerSimpleBlockItem(ModBlocks.WAYMARK);
    public static final DeferredItem<Item> RECALL_CRYSTAL=ITEMS.register("recall_crystal",()->new com.mcmagic.omnira.item.RecallCrystalItem(new Item.Properties(),false));
    public static final DeferredItem<Item> ENHANCED_RECALL_CRYSTAL=ITEMS.register("enhanced_recall_crystal",()->new com.mcmagic.omnira.item.RecallCrystalItem(new Item.Properties().rarity(Rarity.UNCOMMON),true));
    public static final DeferredItem<Item> DREAM_CRYSTAL_SHARD=ITEMS.registerSimpleItem("dream_crystal_shard");
    public static final DeferredItem<Item> POCKET_MAGIC_BOTTLE=ITEMS.register("pocket_magic_bottle",()->
            new com.mcmagic.omnira.item.bottle.PocketBottleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> RITUAL_ENERGY_CORE=ITEMS.registerSimpleBlockItem(ModBlocks.RITUAL_ENERGY_CORE);
    public static final DeferredItem<BlockItem> ADVANCED_RITUAL_ENERGY_CORE=ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE);
    // Display-only ritual results; neither item is obtainable or listed in a creative tab.
    public static final DeferredItem<BlockItem> DREAM_PORTAL=ITEMS.registerSimpleBlockItem(ModBlocks.DREAM_PORTAL);
    public static final DeferredItem<BlockItem> CORRIDOR_GATEWAY=ITEMS.registerSimpleBlockItem(ModBlocks.CORRIDOR_GATEWAY);
    public static final DeferredItem<com.mcmagic.omnira.item.GuideBookItem> GUIDE_BOOK=
            ITEMS.registerItem("guide_book",com.mcmagic.omnira.item.GuideBookItem::new,new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> INFUSED_GRIMOIRE=grimoire("infused_grimoire",com.mcmagic.omnira.item.InfusedGrimoireItem.Kind.INFUSED);
    public static final DeferredItem<Item> SANCTIFIED_GRIMOIRE=grimoire("sanctified_grimoire",com.mcmagic.omnira.item.InfusedGrimoireItem.Kind.SANCTIFIED);
    public static final DeferredItem<Item> CORRUPTED_GRIMOIRE=grimoire("corrupted_grimoire",com.mcmagic.omnira.item.InfusedGrimoireItem.Kind.CORRUPTED);
    private static DeferredItem<Item> grimoire(String id,com.mcmagic.omnira.item.InfusedGrimoireItem.Kind kind) {
        return ITEMS.registerItem(id,p->new com.mcmagic.omnira.item.InfusedGrimoireItem(p,kind));
    }
    public static final DeferredItem<BlockItem> CRYSTAL_PEDESTAL=ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_PEDESTAL);
    public static final DeferredItem<BlockItem> CRYSTAL_BALL=ITEMS.register("crystal_ball",()->new com.mcmagic.omnira.item.PortableOrbBlockItem(ModBlocks.CRYSTAL_BALL.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> PERMANENT_VOID_CRYSTAL=ITEMS.registerSimpleBlockItem(ModBlocks.PERMANENT_VOID_CRYSTAL);
    public static final DeferredItem<BlockItem> MANA_ENGINE=ITEMS.registerSimpleBlockItem(ModBlocks.MANA_ENGINE);
    public static final DeferredItem<BlockItem> CRYSTAL_CASING=ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_CASING);
    public static final DeferredItem<BlockItem> INFUSED_CRYSTAL_CASING=ITEMS.registerSimpleBlockItem(ModBlocks.INFUSED_CRYSTAL_CASING);

    public static final DeferredItem<BlockItem> SIMPLE_CONDENSATION_TABLE=ITEMS.registerSimpleBlockItem(ModBlocks.SIMPLE_CONDENSATION_TABLE);
    public static final DeferredItem<Item> SPIRITUAL_CRYSTAL=ITEMS.registerSimpleItem("spiritual_crystal",new Item.Properties());
    public static final DeferredItem<Item> INFUSED_SPIRITUAL_CRYSTAL=ITEMS.registerSimpleItem("infused_spiritual_crystal",new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> RESONANCE_CRYSTAL=ITEMS.registerSimpleItem("resonance_crystal",new Item.Properties());
    public static final DeferredItem<Item> ARCANE_DUST =
            ITEMS.registerSimpleItem("arcane_dust", new Item.Properties());

    public static final DeferredItem<BlockItem> CRYSTAL_PROCESSING_TABLE =
            ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTAL_PROCESSING_TABLE);
    public static final DeferredItem<BlockItem> ANALYSIS_ARTISAN_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ANALYSIS_ARTISAN_TABLE);

    public static final DeferredItem<Item> SHADOW_MANUSCRIPT = ITEMS.registerSimpleItem("shadow_manuscript");
    public static final DeferredItem<Item> TRAVELER_MANUSCRIPT = ITEMS.registerItem("traveler_manuscript",com.mcmagic.omnira.item.TravelerManuscriptItem::new,new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PSYKER_TALENT=fate("psyker_talent",com.mcmagic.omnira.item.FateCurioItem.Kind.PSYKER);
    public static final DeferredItem<Item> KHORNE_BLESSING=fate("khorne_blessing",com.mcmagic.omnira.item.FateCurioItem.Kind.KHORNE);
    public static final DeferredItem<Item> TZEENTCH_BLESSING=fate("tzeentch_blessing",com.mcmagic.omnira.item.FateCurioItem.Kind.TZEENTCH);
    public static final DeferredItem<Item> NURGLE_BLESSING=fate("nurgle_blessing",com.mcmagic.omnira.item.FateCurioItem.Kind.NURGLE);
    public static final DeferredItem<Item> SLAANESH_BLESSING=fate("slaanesh_blessing",com.mcmagic.omnira.item.FateCurioItem.Kind.SLAANESH);
    public static final java.util.List<DeferredItem<Item>> BLESSINGS=java.util.List.of(KHORNE_BLESSING,TZEENTCH_BLESSING,NURGLE_BLESSING,SLAANESH_BLESSING);
    private static DeferredItem<Item> fate(String id,com.mcmagic.omnira.item.FateCurioItem.Kind kind) {
        return ITEMS.registerItem(id,p->new com.mcmagic.omnira.item.FateCurioItem(p,kind));
    }
    public static final DeferredItem<Item> SHADOW_MIST = ITEMS.register("shadow_mist",com.mcmagic.omnira.item.ShadowMistItem::new);
    public static final DeferredItem<Item> SPACETIME_KNOT = ITEMS.registerSimpleItem("spacetime_knot",new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<Item> SPELL_INK =
            ITEMS.registerSimpleItem("spell_ink", new Item.Properties().durability(128));

    public static final DeferredItem<Item> LOW_TIER_MAGIC_CRYSTAL =
            ITEMS.registerItem("low_tier_magic_crystal", LowTierMagicCrystalItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> EARTH_MICROCORE = registerPrimaryMicrocore("earth_microcore");
    public static final DeferredItem<Item> WATER_MICROCORE = registerPrimaryMicrocore("water_microcore");
    public static final DeferredItem<Item> FIRE_MICROCORE = registerPrimaryMicrocore("fire_microcore");
    public static final DeferredItem<Item> AIR_MICROCORE = registerPrimaryMicrocore("air_microcore");

    public static final DeferredItem<Item> LIGHT_MICROCORE = registerMicrocore("light_microcore", Rarity.UNCOMMON);
    public static final DeferredItem<Item> DARK_MICROCORE = registerMicrocore("dark_microcore", Rarity.UNCOMMON);

    public static final DeferredItem<Item> SPACE_MICROCORE = ITEMS.registerItem("space_microcore",com.mcmagic.omnira.item.CompositeMicrocoreItem::new,new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> TIME_MICROCORE = ITEMS.registerItem("time_microcore",com.mcmagic.omnira.item.CompositeMicrocoreItem::new,new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<Item> OMNI_MICROCORE = registerMicrocore("omni_microcore", Rarity.EPIC);

    public static final DeferredItem<Item> TEST_SPELL_CORE=ITEMS.registerItem("primordial_spell_core",com.mcmagic.omnira.item.SpellCoreItem::new,new Item.Properties());
    public static final DeferredItem<Item> DREAM_SPELL_CORE=ITEMS.registerItem("dream_spell_core",p->new com.mcmagic.omnira.item.SpellCoreItem(p,true),new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LIGHT_DARK_SPELL_CORE=ITEMS.registerItem("light_dark_spell_core",p->new com.mcmagic.omnira.item.SpellCoreItem(p,com.mcmagic.omnira.item.SpellCoreItem.Kind.LIGHT_DARK),new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SPACETIME_SPELL_CORE=ITEMS.registerItem("spacetime_spell_core",p->new com.mcmagic.omnira.item.SpellCoreItem(p,com.mcmagic.omnira.item.SpellCoreItem.Kind.SPACETIME),new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> SHARP_BREATH=ITEMS.registerSimpleItem("sharp_breath",new Item.Properties());
    public static final DeferredItem<Item> HEALING_DEW=ITEMS.registerSimpleItem("healing_dew",new Item.Properties());
    public static final DeferredItem<Item> TEST_DAMAGE_ELEMENT=ITEMS.registerSimpleItem("test_damage_element",new Item.Properties());
    public static final DeferredItem<Item> GRID_FRAME=ITEMS.registerSimpleItem("grid_frame");
    public static final DeferredItem<Item> CRUDE_LIGHT_CORE=ITEMS.registerSimpleItem("crude_light_core");
    public static final DeferredItem<Item> ANALYSIS_CRYSTAL=ITEMS.register("analysis_crystal",com.mcmagic.omnira.item.AnalysisCrystalItem::new);
    public static final DeferredItem<Item> BASIC_CRYSTAL_GRID=grid("basic_crystal_grid",3,50,0,0,0,0);
    public static final DeferredItem<Item> ELEMENTAL_CRYSTAL_GRID=grid("elemental_crystal_grid",5,100,.1,10,.05,0);
    public static final DeferredItem<Item> ARCANE_CRYSTAL_GRID=grid("arcane_crystal_grid",8,300,.2,10,.1,.1);
    public static final DeferredItem<Item> OMNI_CRYSTAL_GRID=grid("omni_crystal_grid",12,500,.5,10,.2,.1);
    public static final DeferredItem<Item> MODULAR_STAFF=ITEMS.registerItem("modular_staff",com.mcmagic.omnira.item.StaffItem::new,new Item.Properties());
    private static DeferredItem<Item> grid(String id,int capacity,double mana,double regen,double reduction,double power,double cooldownReduction) {
        return ITEMS.registerItem(id,p->new com.mcmagic.omnira.item.CrystalGridItem(p,capacity,mana,regen,reduction,power,cooldownReduction),new Item.Properties());
    }

    public static final DeferredItem<BlockItem> ARCANE_ASSEMBLY_TABLE=ITEMS.registerSimpleBlockItem(ModBlocks.ARCANE_ASSEMBLY_TABLE);
    public static final DeferredItem<Item> WOODEN_STAFF_SHAFT=staffPart("wooden_staff_shaft",com.mcmagic.omnira.item.staff.StaffPart.Role.SHAFT,0,0,0,0);
    public static final DeferredItem<Item> SHADOW_STAFF_SHAFT=staffPart("shadow_staff_shaft",com.mcmagic.omnira.item.staff.StaffPart.Role.SHAFT,.1,0,0,0,2);
    public static final DeferredItem<Item> BLAZE_STAFF_SHAFT=staffPart("blaze_staff_shaft",com.mcmagic.omnira.item.staff.StaffPart.Role.SHAFT,.1,0,0,0);
    public static final DeferredItem<Item> IRON_REINFORCEMENT=staffPart("iron_reinforcement",com.mcmagic.omnira.item.staff.StaffPart.Role.REINFORCEMENT,0,0,0,0);
    public static final DeferredItem<Item> DIAMOND_REINFORCEMENT=staffPart("diamond_reinforcement",com.mcmagic.omnira.item.staff.StaffPart.Role.REINFORCEMENT,0,5,0,0);
    public static final DeferredItem<Item> NETHERITE_REINFORCEMENT=staffPart("netherite_reinforcement",com.mcmagic.omnira.item.staff.StaffPart.Role.REINFORCEMENT,0,10,0,0);
    public static final DeferredItem<Item> LIGHT_CORE_REINFORCEMENT=staffPart("light_core_reinforcement",com.mcmagic.omnira.item.staff.StaffPart.Role.REINFORCEMENT,.1,5,0,0);
    public static final DeferredItem<Item> SPIRITUAL_CRYSTAL_TIP=staffPart("spiritual_crystal_tip",com.mcmagic.omnira.item.staff.StaffPart.Role.TIP,0,0,1,60);
    public static final DeferredItem<Item> MULTI_ARCANE_TIP=staffPart("multi_arcane_tip",com.mcmagic.omnira.item.staff.StaffPart.Role.TIP,0,0,2,60,2);
    public static final DeferredItem<Item> ELEMENTAL_FUSION_TIP=staffPart("elemental_fusion_tip",com.mcmagic.omnira.item.staff.StaffPart.Role.TIP,0,0,1,20,2);
    public static final DeferredItem<Item> SWIFTNESS_RUNE=ITEMS.registerItem("swiftness_rune",p ->
            new com.mcmagic.omnira.item.staff.StaffPartItem(p,new com.mcmagic.omnira.item.staff.StaffPart(
                    com.mcmagic.omnira.item.staff.StaffPart.Role.UPGRADE,0,0,0,0,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","item/staff_parts/swiftness_rune"),
                    1,0,0,com.mcmagic.omnira.item.staff.StaffPart.SlotKind.RUNE,.5,.2)),new Item.Properties());
    public static final DeferredItem<Item> WIDE_AREA_RUNE=ITEMS.registerItem("wide_area_rune",p ->
            new com.mcmagic.omnira.item.staff.StaffPartItem(p,new com.mcmagic.omnira.item.staff.StaffPart(
                    com.mcmagic.omnira.item.staff.StaffPart.Role.UPGRADE,0,0,0,0,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","item/staff_parts/wide_area_rune"),
                    1,0,0,com.mcmagic.omnira.item.staff.StaffPart.SlotKind.RUNE,0,0,.5)),new Item.Properties());
    public static final DeferredItem<Item> AMPLIFICATION_RUNE=ITEMS.registerItem("amplification_rune",p ->
            new com.mcmagic.omnira.item.staff.StaffPartItem(p,new com.mcmagic.omnira.item.staff.StaffPart(
                    com.mcmagic.omnira.item.staff.StaffPart.Role.UPGRADE,.5,0,0,0,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","item/staff_parts/amplification_rune"),
                    1,0,0,com.mcmagic.omnira.item.staff.StaffPart.SlotKind.RUNE)),new Item.Properties());
    private static DeferredItem<Item> staffPart(String id,com.mcmagic.omnira.item.staff.StaffPart.Role role,double power,double reduction,int shots,int cooldown) {
        return staffPart(id,role,power,reduction,shots,cooldown,1);
    }
    private static DeferredItem<Item> staffPart(String id,com.mcmagic.omnira.item.staff.StaffPart.Role role,double power,double reduction,int shots,int cooldown,int slots) {
        return ITEMS.registerItem(id,p->new com.mcmagic.omnira.item.staff.StaffPartItem(p,
                com.mcmagic.omnira.item.staff.StaffPart.component(id,role,power,reduction,shots,cooldown)
                        .withSlots(role==com.mcmagic.omnira.item.staff.StaffPart.Role.SHAFT?slots:0,
                                role==com.mcmagic.omnira.item.staff.StaffPart.Role.TIP?slots:0)),new Item.Properties());
    }

    public static final DeferredItem<Item> DISSOCIATION_THREAD=ITEMS.registerSimpleItem("dissociation_thread",new Item.Properties());
    public static final DeferredItem<Item> CONSTRUCTION_MATRIX=ITEMS.registerSimpleItem("construction_matrix",new Item.Properties());
    public static final DeferredItem<Item> MILK_SUSPENSION=ITEMS.registerSimpleItem("milk_suspension",new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> VOID_CRYSTAL=ITEMS.register("void_crystal",()->new BlockItem(ModBlocks.PERMANENT_VOID_CRYSTAL.get(),new Item.Properties()));
    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static DeferredItem<Item> registerMicrocore(String name, Rarity rarity) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().rarity(rarity));
    }

    private static DeferredItem<Item> registerPrimaryMicrocore(String name) {
        return ITEMS.registerItem(name, PrimaryMicrocoreItem::new, new Item.Properties());
    }
}

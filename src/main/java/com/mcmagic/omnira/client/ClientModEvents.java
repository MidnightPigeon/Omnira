package com.mcmagic.omnira.client;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.client.hud.ManaOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import com.mcmagic.omnira.client.screen.CrystalProcessingTableScreen;
import com.mcmagic.omnira.client.screen.AnalysisArtisanTableScreen;
import com.mcmagic.omnira.client.renderer.CrystalProcessingTableRenderer;
import com.mcmagic.omnira.client.renderer.SpellEntityRenderer;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Omnira.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }
    @SubscribeEvent
    public static void dimensionEffects(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"dream_realm"),new DreamSkyEffects());
        event.register(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"spacetime_corridor"),new SpacetimeSkyEffects());
    }

    @SubscribeEvent
    public static void setupCurios(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(()->{
            net.minecraft.client.renderer.item.ItemProperties.register(com.mcmagic.omnira.registry.ModItems.MEMORY_CUBE.get(),
                    ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"peaceful"),(stack,level,entity,seed)->
                            com.mcmagic.omnira.item.MemoryCubeBlockItem.peaceful(stack)?1F:0F);
            net.minecraft.client.renderer.item.ItemProperties.register(com.mcmagic.omnira.registry.ModItems.ARCANE_NEEDLE.get(),
                    ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"needle_out"),(stack,level,entity,seed)->
                            entity instanceof net.minecraft.world.entity.player.Player player && player.getMainHandItem()==stack
                                    && com.mcmagic.omnira.item.FlyingNeedle.isOut(player)?1F:0F);
            net.minecraft.client.renderer.item.ItemProperties.register(com.mcmagic.omnira.registry.ModItems.POCKET_MAGIC_BOTTLE.get(),
                    ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"filled"),(stack,level,entity,seed)->
                            com.mcmagic.omnira.item.bottle.PocketBottleItem.filled(stack)?1F:0F);
            for(var item:new net.minecraft.world.item.Item[]{
                    com.mcmagic.omnira.registry.ModItems.BASIC_CRYSTAL_GRID.get(),
                    com.mcmagic.omnira.registry.ModItems.ELEMENTAL_CRYSTAL_GRID.get(),
                    com.mcmagic.omnira.registry.ModItems.ARCANE_CRYSTAL_GRID.get(),
                    com.mcmagic.omnira.registry.ModItems.OMNI_CRYSTAL_GRID.get(),
                    com.mcmagic.omnira.registry.ModItems.TEST_SPELL_CORE.get(),
                    com.mcmagic.omnira.registry.ModItems.DREAM_SPELL_CORE.get(),
                    com.mcmagic.omnira.registry.ModItems.LIGHT_DARK_SPELL_CORE.get(),
                    com.mcmagic.omnira.registry.ModItems.SPACETIME_SPELL_CORE.get()})
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(item,
                        com.mcmagic.omnira.client.renderer.EquippedCrystalRenderer::new);
        });
    }

    @SubscribeEvent
    public static void additionalModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(com.mcmagic.omnira.client.renderer.MemoryCubeRenderer.model(false));
        event.register(com.mcmagic.omnira.client.renderer.MemoryCubeRenderer.model(true));
        for(String part:com.mcmagic.omnira.client.renderer.AdvancedForgeRenderer.PARTS)event.register(com.mcmagic.omnira.client.renderer.AdvancedForgeRenderer.model(part));
        event.register(com.mcmagic.omnira.client.renderer.CrystalGridRenderer.part("infused_spiritual_crystal","body"));
        event.register(com.mcmagic.omnira.client.renderer.CrystalGridRenderer.part("infused_spiritual_crystal","mote"));
        event.register(com.mcmagic.omnira.client.renderer.FlyingNeedleRenderer.MODEL);
        for(String part:new String[]{"body","mote"})
            event.register(com.mcmagic.omnira.client.renderer.ResonanceCrystalRenderer.model(part));
        for(String part:com.mcmagic.omnira.client.renderer.ArquebusRenderer.PARTS)
            event.register(com.mcmagic.omnira.client.renderer.ArquebusRenderer.model(part));
        for(String part:com.mcmagic.omnira.client.renderer.CrystalBallRenderer.PARTS)
            event.register(com.mcmagic.omnira.client.renderer.CrystalBallRenderer.model(part));
        // Resource discovery keeps future part and upgrade overlays independent of Java variant lists.
        for(var resource:net.minecraft.client.Minecraft.getInstance().getResourceManager()
                .listResources("models/item/staff_parts",id->id.getPath().endsWith(".json")).keySet()) {
            String path=resource.getPath();
            event.register(com.mcmagic.omnira.client.renderer.ModularStaffRenderer.part(
                    ResourceLocation.fromNamespaceAndPath(resource.getNamespace(),path.substring(7,path.length()-5))));
        }
        event.register(com.mcmagic.omnira.client.renderer.MilkSuspensionRenderer.model());
        event.register(com.mcmagic.omnira.client.renderer.ArcaneMaterialRenderer.model(false));
        event.register(com.mcmagic.omnira.client.renderer.ArcaneMaterialRenderer.model(true));
        event.register(com.mcmagic.omnira.client.renderer.ArcaneMaterialRenderer.spark());
        for(String part:com.mcmagic.omnira.client.renderer.AnalysisCrystalRenderer.PARTS)
            event.register(com.mcmagic.omnira.client.renderer.AnalysisCrystalRenderer.model(part));
        event.register(com.mcmagic.omnira.client.renderer.WaymarkRenderer.model());
        event.register(com.mcmagic.omnira.client.renderer.RitualEnergyCoreRenderer.model());
        event.register(com.mcmagic.omnira.client.renderer.RitualEnergyCoreRenderer.advancedModel());
        event.register(com.mcmagic.omnira.client.renderer.AssemblyRenderer.shaft());
        event.register(CrystalProcessingTableRenderer.hammerModel());
        for(String part:new String[]{"core","ripple"})event.register(com.mcmagic.omnira.client.renderer.AdvancedCondensationVisuals.model(part));
        for(String part:new String[]{"body","halo","grain"})event.register(com.mcmagic.omnira.client.renderer.UnstableAggregateRenderer.model(part));
        for(String part:new String[]{"body","grain"})event.register(com.mcmagic.omnira.client.renderer.TimeWarpPointItemRenderer.model(part));
        for(String part:new String[]{"head","hat","eyes","grain"})event.register(com.mcmagic.omnira.client.renderer.DreamRabbitRenderer.model(part));
        event.register(com.mcmagic.omnira.client.renderer.TimePlantItemRenderer.model(false));
        event.register(com.mcmagic.omnira.client.renderer.TimePlantItemRenderer.model(true));
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","block/wonderland_poker_stand")));
        for(String suit:new String[]{"heart","spade","diamond","club"}){
            event.register(com.mcmagic.omnira.client.renderer.WonderlandPokerStandRenderer.cardModel(suit));
            event.register(com.mcmagic.omnira.client.renderer.WonderlandPokerStandRenderer.symbolModel(suit));
        }
        for(String part:new String[]{"base","shell"})event.register(com.mcmagic.omnira.client.renderer.MarisaOrbRenderer.model(part));
        for(String part:new String[]{"closed","open"})
            event.register(com.mcmagic.omnira.client.renderer.GuideBookRenderer.model(part));
        for(String kind:new String[]{"infused","sanctified","corrupted"})
            event.register(com.mcmagic.omnira.client.renderer.GuideBookRenderer.grimoire(kind));
        for(String part:new String[]{"body","mote"}) {
            event.register(com.mcmagic.omnira.client.renderer.SpellCoreRenderer.model(part));
            event.register(com.mcmagic.omnira.client.renderer.SpellCoreRenderer.dreamModel(part));
            event.register(com.mcmagic.omnira.client.renderer.SpellCoreRenderer.lightDarkModel(part));
        }
        for(String part:new String[]{"body","star","mote_blue","mote_green"})
            event.register(com.mcmagic.omnira.client.renderer.SpellCoreRenderer.spacetimeModel(part));
        event.register(com.mcmagic.omnira.client.renderer.ManaEngineRenderer.model("rotor"));
        event.register(com.mcmagic.omnira.client.renderer.ManaEngineRenderer.model("shaft"));
        for(String tier:new String[]{"basic","elemental","arcane","omni"})
            for(String part:new String[]{"frame","orbit"})
                event.register(com.mcmagic.omnira.client.renderer.CrystalGridRenderer.part(tier+"_crystal_grid",part));
    }

    @SubscribeEvent
    public static void ghostColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint)->{
            if(tint<1 || tint>com.mcmagic.omnira.item.ResonanceTerminalItem.capacity(stack))return 0xFFFFFFFF;
            int kind=com.mcmagic.omnira.item.ResonanceTerminalItem.data(stack).getCompound("Link"+(tint-1)).getInt("Kind");
            return kind==4?0xDDFFE58A:kind==3?0xDD85C5FF:kind==2?0xDDA3EBB6:0xDDF3A1B2;
        },com.mcmagic.omnira.registry.ModItems.RESONANCE_TERMINAL.get(),com.mcmagic.omnira.registry.ModItems.ENHANCED_RESONANCE_TERMINAL.get());
        event.register((stack,tint)->{
            double phase=.5+.5*Math.sin(net.minecraft.Util.getMillis()/850.0);
            int r=(int)(120+55*phase),g=(int)(175-55*phase),b=255;
            return 0xFF000000 | r<<16 | g<<8 | b;
        },com.mcmagic.omnira.registry.ModItems.DREAM_SPELL_CORE.get());
        event.register((stack,tint)->{
            if(tint<0)return 0xFFFFFFFF;
            double phase=.5+.5*Math.sin(net.minecraft.Util.getMillis()/850.0);
            int shade=(int)(24+231*(tint==1?1-phase:phase));
            return 0xFF000000 | shade<<16 | shade<<8 | shade;
        },com.mcmagic.omnira.registry.ModItems.LIGHT_DARK_SPELL_CORE.get());
        event.register((stack,tint)->switch(tint) {case 1->0xFF8EAFC5;case 2->0xFFD5F8FF;default->0xFFA6DDF7;},com.mcmagic.omnira.registry.ModItems.CRYSTALLIZED_NAIL.get());
        event.register((stack,tint)->switch(tint) {case 1->0xFF9482B8;case 2->0xFFE7DEF5;default->0xFFC7B5E5;},com.mcmagic.omnira.registry.ModItems.ARCANE_NEEDLE.get());
        event.register((stack,tint)->{
            double phase=net.minecraft.Util.getMillis()/180.0+tint*1.7;
            int shade=(int)(22+233*(.5+.5*Math.sin(phase)));
            return 0xFF000000 | shade<<16 | shade<<8 | shade;
        },com.mcmagic.omnira.registry.ModItems.PARADOX_DUST.get());
        event.register((stack,tint)->{
            if(tint==2)return 0xEE8DDEFF;
            if(tint==3)return 0xFF211523;
            if(tint==4)return 0xFFF34363;
            if(tint==5)return 0xDDD8B6F4;
            if(tint==6)return 0xEEA88BC9;
            if(tint==7)return 0xEEDDC4FF;
            double time=net.minecraft.Util.getMillis()/1000.0;
            int alpha=(int)(130+55*Math.sin(time*1.4)+20*Math.sin(time*2.3));
            return alpha<<24 | (tint==1?0xBDDFFF:0x72B9EF);
        },com.mcmagic.omnira.registry.ModItems.ARCANE_ARQUEBUS.get(),com.mcmagic.omnira.registry.ModItems.KINGS_NEW_CLOTHES.get());
    }

    @SubscribeEvent
    public static void itemRenderers(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private com.mcmagic.omnira.client.renderer.WonderlandPokerStandRenderer.ItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.WonderlandPokerStandRenderer.ItemRenderer();return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.WONDERLAND_POKER_STAND.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private com.mcmagic.omnira.client.renderer.TimePlantItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.TimePlantItemRenderer();return renderer;
            }
        },com.mcmagic.omnira.registry.TimeNatureContent.GRASS.get().asItem(),com.mcmagic.omnira.registry.TimeNatureContent.FLOWER.get().asItem());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private com.mcmagic.omnira.client.renderer.MarisaOrbRenderer.ItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.MarisaOrbRenderer.ItemRenderer();return renderer;
            }
        },com.mcmagic.omnira.shop.KirisameContent.ORB.get().asItem());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private final com.mcmagic.omnira.client.renderer.TimeWarpPointItemRenderer renderer=new com.mcmagic.omnira.client.renderer.TimeWarpPointItemRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },com.mcmagic.omnira.registry.ModItems.TIME_WARP_POINT.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private final com.mcmagic.omnira.client.renderer.DreamRabbitRenderer renderer=new com.mcmagic.omnira.client.renderer.DreamRabbitRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },com.mcmagic.omnira.registry.ModItems.DREAM_RABBIT_REMAINS.get(),com.mcmagic.omnira.registry.ModItems.ALICE_DREAM_RABBIT_SIGIL.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private final com.mcmagic.omnira.client.renderer.UnstableAggregateRenderer renderer=new com.mcmagic.omnira.client.renderer.UnstableAggregateRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private final com.mcmagic.omnira.client.renderer.AdvancedForgeItemRenderer renderer=new com.mcmagic.omnira.client.renderer.AdvancedForgeItemRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },com.mcmagic.omnira.registry.ModItems.ADVANCED_FORGE.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private final com.mcmagic.omnira.client.renderer.InfusedCrystalRenderer renderer=new com.mcmagic.omnira.client.renderer.InfusedCrystalRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get());
        CrystalDressRendering.register(event);
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.CrystalBroomRenderer.ItemRenderer();return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.CRYSTAL_BROOM.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.CruiseOrbRenderer.ItemRenderer();return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.CRUISE_ORB.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.NightHeronStatueRenderer.ItemRenderer();return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.NIGHT_HERON_STATUE.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.ResonanceCrystalRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.RESONANCE_CRYSTAL.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.ArquebusRenderer();
                return renderer;
            }
            @Override public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player,net.minecraft.world.entity.HumanoidArm arm,
                    net.minecraft.world.item.ItemStack stack,float partialTick,float equip,float swing) {
                int side=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
                pose.translate(side*.46,-.42-equip*.6,-.65+Math.sin(swing*Math.PI)*.07);
                return true;
            }
        },com.mcmagic.omnira.registry.ModItems.ARCANE_ARQUEBUS.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.CrystalBallRenderer.ItemRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.CRYSTAL_BALL.get(),com.mcmagic.omnira.registry.ModItems.LIQUID_CRYSTAL_BALL.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new com.mcmagic.omnira.client.renderer.AnalysisCrystalRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.ANALYSIS_CRYSTAL.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new com.mcmagic.omnira.client.renderer.ArcaneMaterialRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.SHADOW_MIST.get(),com.mcmagic.omnira.registry.ModItems.SPACETIME_KNOT.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new com.mcmagic.omnira.client.renderer.ModularStaffRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.MODULAR_STAFF.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new com.mcmagic.omnira.client.renderer.MilkSuspensionRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.MILK_SUSPENSION.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new com.mcmagic.omnira.client.renderer.SpellCoreRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.TEST_SPELL_CORE.get(),com.mcmagic.omnira.registry.ModItems.DREAM_SPELL_CORE.get(),com.mcmagic.omnira.registry.ModItems.LIGHT_DARK_SPELL_CORE.get(),com.mcmagic.omnira.registry.ModItems.SPACETIME_SPELL_CORE.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new com.mcmagic.omnira.client.renderer.GuideBookRenderer();
                return renderer;
            }
        },com.mcmagic.omnira.registry.ModItems.GUIDE_BOOK.get(),com.mcmagic.omnira.registry.ModItems.INFUSED_GRIMOIRE.get(),
                com.mcmagic.omnira.registry.ModItems.SANCTIFIED_GRIMOIRE.get(),com.mcmagic.omnira.registry.ModItems.CORRUPTED_GRIMOIRE.get());
        var extension=new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new com.mcmagic.omnira.client.renderer.CrystalGridRenderer();
                return renderer;
            }
        };
        event.registerItem(extension,com.mcmagic.omnira.registry.ModItems.BASIC_CRYSTAL_GRID.get(),
                com.mcmagic.omnira.registry.ModItems.ELEMENTAL_CRYSTAL_GRID.get(),
                com.mcmagic.omnira.registry.ModItems.ARCANE_CRYSTAL_GRID.get(),
                com.mcmagic.omnira.registry.ModItems.OMNI_CRYSTAL_GRID.get());
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"recall"),com.mcmagic.omnira.client.hud.RecallOverlay::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"spell_chain"),com.mcmagic.omnira.client.hud.LatticeOverlay::render);
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL,
                ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "mana"), ManaOverlay::render);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CRUISE_ORB.get(),com.mcmagic.omnira.client.screen.CruiseOrbScreen::new);
        event.register(ModMenuTypes.ARQUEBUS.get(),com.mcmagic.omnira.client.screen.ArquebusScreen::new);
        event.register(ModMenuTypes.SWORD_SPELL.get(),com.mcmagic.omnira.client.screen.ArquebusScreen::new);
        event.register(ModMenuTypes.CRYSTAL_BALL.get(),com.mcmagic.omnira.client.screen.CrystalBallScreen::new);
        event.register(ModMenuTypes.ADVANCED_FORGE.get(),com.mcmagic.omnira.client.screen.AdvancedForgeScreen::new);
        event.register(ModMenuTypes.ORB_UPGRADE.get(),com.mcmagic.omnira.client.screen.OrbUpgradeScreen::new);
        event.register(ModMenuTypes.RESONANCE.get(),com.mcmagic.omnira.client.screen.ResonanceScreen::new);
        event.register(ModMenuTypes.WAYMARK.get(),com.mcmagic.omnira.client.screen.WaymarkScreen::new);
        event.register(ModMenuTypes.MANA_ENGINE.get(),com.mcmagic.omnira.client.screen.ManaEngineScreen::new);
        event.register(ModMenuTypes.ARCANE_ASSEMBLY_TABLE.get(),com.mcmagic.omnira.client.screen.ArcaneAssemblyTableScreen::new);
        event.register(ModMenuTypes.SIMPLE_CONDENSATION_TABLE.get(),com.mcmagic.omnira.client.screen.SimpleCondensationTableScreen::new);
        event.register(ModMenuTypes.ADVANCED_CONDENSATION_TABLE.get(),com.mcmagic.omnira.client.screen.SimpleCondensationTableScreen::new);
        event.register(ModMenuTypes.CRYSTAL_GRID.get(),com.mcmagic.omnira.client.screen.CrystalGridScreen::new);
        event.register(ModMenuTypes.ANALYSIS_ARTISAN_TABLE.get(), AnalysisArtisanTableScreen::new);
        event.register(ModMenuTypes.CRYSTAL_PROCESSING_TABLE.get(), CrystalProcessingTableScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.WONDERLAND_POKER_STAND.get(),com.mcmagic.omnira.client.renderer.WonderlandPokerStandRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.MEMORY_CUBE.get(),com.mcmagic.omnira.client.renderer.MemoryCubeRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.GOLDEN_THRONE_SEAT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.ADVANCED_FORGE.get(),com.mcmagic.omnira.client.renderer.AdvancedForgeRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CRUISE_ORB.get(),com.mcmagic.omnira.client.renderer.CruiseOrbRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CRYSTAL_BROOM.get(),com.mcmagic.omnira.client.renderer.CrystalBroomRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CRYSTAL_BALL.get(),com.mcmagic.omnira.client.renderer.CrystalBallRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.MARISA_ORB.get(),com.mcmagic.omnira.client.renderer.MarisaOrbRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.TIME_PLANT.get(),com.mcmagic.omnira.client.renderer.TimePlantRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.LIQUID_CRYSTAL_BALL.get(),context->new com.mcmagic.omnira.client.renderer.CrystalBallRenderer(context)::render);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PURE_VESSEL.get(),com.mcmagic.omnira.client.renderer.PureVesselRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SPELL_CORE.get(),com.mcmagic.omnira.client.renderer.PlacedSpellCoreRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.NIGHT_HERON_STATUE.get(),com.mcmagic.omnira.client.renderer.NightHeronStatueRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FLYING_NEEDLE.get(),com.mcmagic.omnira.client.renderer.FlyingNeedleRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SHADOW_MIST.get(),com.mcmagic.omnira.client.renderer.ShadowMistRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SHADOW_GHOST.get(),com.mcmagic.omnira.client.renderer.ShadowGhostRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DREAM_MIRROR.get(),com.mcmagic.omnira.client.renderer.DreamMirrorRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.LIGHT_SPIRIT.get(), com.mcmagic.omnira.client.renderer.LightSpiritRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ARCHAEOPTERYX_SPIRIT.get(),context->new com.mcmagic.omnira.client.renderer.AncientCompanionRenderer<>(context,true));
        event.registerEntityRenderer(ModEntityTypes.VELOCIRAPTOR.get(),context->new com.mcmagic.omnira.client.renderer.AncientCompanionRenderer<>(context,false));
        event.registerBlockEntityRenderer(ModBlockEntityTypes.WAYMARK.get(),com.mcmagic.omnira.client.renderer.WaymarkRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.POCKET_BOTTLE.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.UNSTABLE_AGGREGATE.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TEMPORAL_AMBER.get(),com.mcmagic.omnira.client.renderer.TemporalAmberRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.RITUAL_ENERGY_CORE.get(),com.mcmagic.omnira.client.renderer.RitualEnergyCoreRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CRYSTAL_PEDESTAL.get(),com.mcmagic.omnira.client.renderer.CrystalPedestalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.DREAM_PORTAL.get(),com.mcmagic.omnira.client.renderer.DreamPortalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CORRIDOR_GATEWAY.get(),com.mcmagic.omnira.client.renderer.CorridorGatewayRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.MANA_ENGINE.get(),com.mcmagic.omnira.client.renderer.ManaEngineRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.ARCANE_ASSEMBLY_TABLE.get(),com.mcmagic.omnira.client.renderer.AssemblyRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SIMPLE_CONDENSATION_TABLE.get(),CrystalProcessingTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.ADVANCED_CONDENSATION_TABLE.get(),CrystalProcessingTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.ANALYSIS_ARTISAN_TABLE.get(), CrystalProcessingTableRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SPELL.get(), SpellEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CRYSTAL_PROCESSING_TABLE.get(), CrystalProcessingTableRenderer::new);
    }
}

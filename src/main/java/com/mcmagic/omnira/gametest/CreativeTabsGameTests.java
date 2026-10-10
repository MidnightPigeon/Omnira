package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_creative_tabs")
@PrefixGameTestTemplate(false)
public final class CreativeTabsGameTests {
    @GameTest(template="spell_arena") public static void functionalFamiliesStayTogether(GameTestHelper h){
        var expected=Map.of(
                ModCreativeModeTabs.Category.BUILDING,List.of("spatial_stability_anchor","dormant_spatial_stability_anchor","star_source","void_crevice"),
                ModCreativeModeTabs.Category.RESEARCH,List.of("rough_alchemy_cauldron","primitive_reaction_cauldron","large_alchemical_reaction_pool","marisa_crystal_ball","spacetime_reverser","split_time_sundial"),
                ModCreativeModeTabs.Category.STORAGE,List.of("pocket_magic_bottle","dream_rabbit_remains","alice_dream_rabbit_sigil","spacetime_waymark","recall_crystal","enhanced_recall_crystal"),
                ModCreativeModeTabs.Category.MISC,List.of("rough_alchemical_energy_core","enhanced_alchemical_energy_core","alchemical_waste","stardust","decayed_timeflow_bucket","void_starflow_bucket","dilute_spacetime_bucket","refined_spacetime_bucket"));
        expected.forEach((category,ids)->h.assertTrue(ModCreativeModeTabs.declaredIds(category).containsAll(ids),"Functional category drift: "+category));
        var building=ModCreativeModeTabs.declaredIds(ModCreativeModeTabs.Category.BUILDING);
        for(var family:List.of("spiritual_crystal","infused_crystal","dream_crystal","spatial_crystal")){
            int index=building.indexOf(family+"_block");
            h.assertTrue(index>=0&&building.subList(index,index+3).equals(List.of(family+"_block",family+"_stairs",family+"_slab")),"Split crystal family: "+family);
        }
        adjacent(h,ModCreativeModeTabs.Category.BUILDING,List.of("spatial_stability_anchor","dormant_spatial_stability_anchor"));
        adjacent(h,ModCreativeModeTabs.Category.RESEARCH,List.of("rough_alchemy_cauldron","primitive_reaction_cauldron","large_alchemical_reaction_pool"));
        adjacent(h,ModCreativeModeTabs.Category.MISC,List.of("rough_alchemical_energy_core","enhanced_alchemical_energy_core","alchemical_waste"));
        adjacent(h,ModCreativeModeTabs.Category.MISC,List.of("decayed_timeflow_bucket","void_starflow_bucket","dilute_spacetime_bucket","refined_spacetime_bucket"));
        adjacent(h,ModCreativeModeTabs.Category.STORAGE,List.of("dream_rabbit_remains","alice_dream_rabbit_sigil"));
        h.succeed();
    }
    private static void adjacent(GameTestHelper h,ModCreativeModeTabs.Category category,List<String> family){
        var ids=ModCreativeModeTabs.declaredIds(category);int at=ids.indexOf(family.getFirst());
        h.assertTrue(at>=0&&at+family.size()<=ids.size()&&ids.subList(at,at+family.size()).equals(family),"Separated progression: "+family);
    }
    @GameTest(template="spell_arena") public static void completeExclusiveCatalog(GameTestHelper h) {
        var assigned=new HashMap<Item,ModCreativeModeTabs.Category>();
        var declared=new HashSet<String>();var invalid=new ArrayList<String>();
        for(var category:ModCreativeModeTabs.Category.values())for(String id:ModCreativeModeTabs.declaredIds(category)){
            h.assertTrue(declared.add(id),"Duplicate catalog ID: "+id);
            if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse("omnira:"+id)))invalid.add(id);
        }
        h.assertTrue(invalid.isEmpty(),"Unknown catalog IDs: "+invalid);
        h.assertTrue(ModCreativeModeTabs.uncategorized().isEmpty(),"Uncategorized items: "+ModCreativeModeTabs.uncategorized());
        for(var category:ModCreativeModeTabs.Category.values()) {
            var stacks=ModCreativeModeTabs.contents(category);
            var variants=new ArrayList<ItemStack>();
            h.assertTrue(!stacks.isEmpty(),"Empty creative tab");
            for(var stack:stacks) {
                h.assertTrue(!stack.isEmpty() && stack.getCount()==1,"Invalid creative stack");
                var previous=assigned.putIfAbsent(stack.getItem(),category);
                h.assertTrue(previous==null||previous==category,"Item appears in multiple tabs: "+stack);
                h.assertTrue(variants.stream().noneMatch(old->ItemStack.isSameItemSameComponents(old,stack)),"Duplicate creative variant: "+stack);
                variants.add(stack);
                h.assertTrue(!ModCreativeModeTabs.hidden(stack.getItem()),"Legacy/test item exposed");
                if(stack.is(ModItems.MODULAR_STAFF.get()))
                    h.assertTrue(stack.has(ModDataComponents.STAFF_ASSEMBLY.get()) && com.mcmagic.omnira.item.staff.StaffAssembly.of(stack).valid(),"Creative staff has no usable assembly");
            }
        }
        for(var item:ModItems.ITEMS.getEntries())
            h.assertTrue(assigned.containsKey(item.get())!=ModCreativeModeTabs.hidden(item.get()),"Missing or hidden item: "+item.getId());
        h.assertTrue(assigned.get(ModItems.ARCANE_ARQUEBUS.get())==ModCreativeModeTabs.Category.SPELLS,"Weapon misplaced");
        h.assertTrue(assigned.get(ModItems.CRYSTAL_BALL.get())==ModCreativeModeTabs.Category.STORAGE,"Storage misplaced");
        for(var item:List.of(ModItems.WAYMARK.get(),ModItems.RECALL_CRYSTAL.get(),ModItems.ENHANCED_RECALL_CRYSTAL.get(),
                ModItems.FOLD_GATE_FRAME.get(),ModItems.STABLE_FOLD_GATE.get(),ModItems.UNSTABLE_FOLD_GATE.get()))
            h.assertTrue(assigned.get(item)==ModCreativeModeTabs.Category.STORAGE,"Player transport misplaced: "+item);
        h.assertTrue(assigned.get(DreamContent.SHADOW_LANTERN.get().asItem())==ModCreativeModeTabs.Category.BUILDING,"Decoration misplaced");
        h.assertTrue(assigned.get(ModItems.SHADOW_MIST.get())==ModCreativeModeTabs.Category.MISC,"Shadow mist misplaced");
        h.assertTrue(assigned.get(DreamContent.DREAM_CRYSTAL.get().asItem())==ModCreativeModeTabs.Category.MISC,"Natural crystal block misplaced");
        h.assertTrue(assigned.get(ModItems.CRUDE_LIGHT_CORE.get())==ModCreativeModeTabs.Category.MISC,"Intermediate product misplaced");
        var parameters=new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(),true,h.getLevel().registryAccess());
        h.assertTrue(ModCreativeModeTabs.contents(ModCreativeModeTabs.Category.BUILDING).stream().filter(s->s.is(ModItems.MEMORY_CUBE.get())).count()==2,"Memory cube states missing");
        var equipment=ModCreativeModeTabs.declaredIds(ModCreativeModeTabs.Category.SPELLS);
        int d=equipment.indexOf("d_class_credentials");
        h.assertTrue(equipment.subList(d,d+4).equals(List.of("d_class_credentials","c_class_credentials","b_class_credentials","a_class_credentials")),"Credential progression scrambled");
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream().filter(id->id.getNamespace().equals("omnira")).count()==5,"Expected exactly five Omnira tabs");
        for(var holder:List.of(ModCreativeModeTabs.MAGIC_MOD_TAB,ModCreativeModeTabs.RESEARCH_TAB,ModCreativeModeTabs.STORAGE_TAB,ModCreativeModeTabs.BUILDING_TAB,ModCreativeModeTabs.MISC_TAB)) {
            holder.get().buildContents(parameters);
            h.assertTrue(!holder.get().getDisplayItems().isEmpty(),"Registered tab did not populate");
        }
        var ordered=net.neoforged.neoforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs();
        var own=List.of(ModCreativeModeTabs.MAGIC_MOD_TAB.get(),ModCreativeModeTabs.RESEARCH_TAB.get(),ModCreativeModeTabs.STORAGE_TAB.get(),ModCreativeModeTabs.BUILDING_TAB.get(),ModCreativeModeTabs.MISC_TAB.get());
        int start=ordered.indexOf(own.getFirst());
        h.assertTrue(start>=0&&start+own.size()<=ordered.size()&&ordered.subList(start,start+own.size()).equals(own),"Omnira tabs are not contiguous");
        var outsiders=ordered.stream().filter(tab->!own.contains(tab)).toList();
        var scrambled=new ArrayList<>(outsiders);
        for(int i=0;i<own.size();i++)scrambled.add(Math.min(i*2,scrambled.size()),own.get(i));
        var grouped=ModCreativeModeTabs.groupTabs(scrambled);
        h.assertTrue(grouped.stream().filter(tab->!own.contains(tab)).toList().equals(outsiders),"Other mods' tab order changed");
        h.assertTrue(ModCreativeModeTabs.groupTabs(grouped).equals(grouped),"Tab grouping is not idempotent");
        for(var category:ModCreativeModeTabs.Category.values())com.mcmagic.omnira.Omnira.LOGGER.info("Creative category {}: {} stacks",category,ModCreativeModeTabs.contents(category).size());
        com.mcmagic.omnira.Omnira.LOGGER.info("Creative tabs cover {} unique items",assigned.size());
        h.succeed();
    }
}

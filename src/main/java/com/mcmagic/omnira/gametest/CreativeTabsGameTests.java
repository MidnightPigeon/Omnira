package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_creative_tabs")
@PrefixGameTestTemplate(false)
public final class CreativeTabsGameTests {
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
        h.assertTrue(assigned.get(ModItems.ARCANE_ARQUEBUS.get())==ModCreativeModeTabs.Category.EQUIPMENT,"Weapon misplaced");
        h.assertTrue(assigned.get(ModItems.CRYSTAL_BALL.get())==ModCreativeModeTabs.Category.RESEARCH,"Storage misplaced");
        h.assertTrue(assigned.get(DreamContent.SHADOW_LANTERN.get().asItem())==ModCreativeModeTabs.Category.BUILDING,"Decoration misplaced");
        h.assertTrue(assigned.get(ModItems.SHADOW_MIST.get())==ModCreativeModeTabs.Category.MISC,"Shadow mist misplaced");
        for(var stack:ModCreativeModeTabs.contents(ModCreativeModeTabs.Category.MISC))
            h.assertTrue(!(stack.getItem() instanceof BlockItem),"Placeable material in miscellaneous");
        h.assertTrue(assigned.get(DreamContent.DREAM_CRYSTAL.get().asItem())==ModCreativeModeTabs.Category.NATURE,"Natural crystal block misplaced");
        h.assertTrue(assigned.get(ModItems.CRUDE_LIGHT_CORE.get())==ModCreativeModeTabs.Category.MISC,"Intermediate product misplaced");
        var parameters=new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(),true,h.getLevel().registryAccess());
        h.assertTrue(ModCreativeModeTabs.contents(ModCreativeModeTabs.Category.BUILDING).stream().filter(s->s.is(ModItems.MEMORY_CUBE.get())).count()==2,"Memory cube states missing");
        var equipment=ModCreativeModeTabs.declaredIds(ModCreativeModeTabs.Category.EQUIPMENT);
        int d=equipment.indexOf("d_class_credentials");
        h.assertTrue(equipment.subList(d,d+4).equals(List.of("d_class_credentials","c_class_credentials","b_class_credentials","a_class_credentials")),"Credential progression scrambled");
        for(var holder:List.of(ModCreativeModeTabs.MAGIC_MOD_TAB,ModCreativeModeTabs.EQUIPMENT_TAB,ModCreativeModeTabs.RESEARCH_TAB,ModCreativeModeTabs.MISC_TAB,ModCreativeModeTabs.BUILDING_TAB,ModCreativeModeTabs.NATURE_TAB)) {
            holder.get().buildContents(parameters);
            h.assertTrue(!holder.get().getDisplayItems().isEmpty(),"Registered tab did not populate");
        }
        for(var category:ModCreativeModeTabs.Category.values())com.mcmagic.omnira.Omnira.LOGGER.info("Creative category {}: {} stacks",category,ModCreativeModeTabs.contents(category).size());
        com.mcmagic.omnira.Omnira.LOGGER.info("Creative tabs cover {} unique items",assigned.size());
        h.succeed();
    }
}

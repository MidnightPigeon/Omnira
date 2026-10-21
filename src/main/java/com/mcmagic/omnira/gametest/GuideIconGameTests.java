package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.guide.GuideContents;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guide_icons")
@PrefixGameTestTemplate(false)
public final class GuideIconGameTests {
    @GameTest(template="spell_arena") public static void allDisplayedIconsAreRegistered(GameTestHelper h) throws Exception {
        for(var language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuidanceGameTests.guide(language));
            for(var chapter:book.chapters())registered(h,chapter);
        }
        h.succeed();
    }
    private static void registered(GameTestHelper h,GuideContents.Chapter chapter){
        var seen=new HashSet<String>();
        for(var row:chapter.directory()){
            var id=ResourceLocation.parse(row.icon());
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(id)&&BuiltInRegistries.ITEM.get(id)!=Items.AIR,"Missing directory icon: "+row.title()+" / "+id);
            h.assertTrue(seen.add(row.icon()),"Duplicate sibling icon in "+chapter.id()+": "+row.icon());
            if(row.entry()!=null){
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(row.entry().item())),"Missing item binding: "+row.entry().id());
                h.assertTrue(row.icon().equals(row.entry().icon()),"Directory ignored entry override");
            }else registered(h,row.chapter());
        }
    }
    @GameTest(template="spell_arena") public static void iconDoesNotChangeRecipesOrItemLookup(GameTestHelper h) throws Exception {
        for(var language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuidanceGameTests.guide(language));
            var assembly=book.destination("assemble_staff").orElseThrow().entry();
            h.assertTrue(assembly.icon().equals("omnira:arcane_assembly_table")&&assembly.item().equals("omnira:modular_staff")&&assembly.recipes(),"Display override changed recipe lookup");
            var temporal=book.destination("temporal_energy").orElseThrow().entry();
            h.assertTrue(temporal.icon().equals("omnira:time_seed")&&temporal.item().equals("omnira:spacetime_reverser"),"Energy material page lost its binding");
            h.assertTrue(book.itemDestination("omnira:pulse_accumulator_orb").orElseThrow().entry().id().equals("magic_pulse"),"Item navigation now follows display icons");
            var ball=book.destination("crystal_ball").orElseThrow().entry();
            h.assertTrue(ball.icon().equals(ball.item()),"Ordinary physical item no longer uses its own icon");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void oldResourcesAndConstructorKeepDefaultIcon(GameTestHelper h) throws Exception {
        var json=GuidanceGameTests.guide("zh_cn");var source=GuidanceGameTests.find(json,"assemble_staff");source.remove("icon");
        var entry=GuideContents.fromJson(json).destination("assemble_staff").orElseThrow().entry();
        h.assertTrue(entry.icon().equals(entry.item())&&entry.item().equals("omnira:modular_staff"),"Resource without icon lost fallback");
        var legacy=new GuideContents.Entry("legacy","minecraft:stone","Legacy",List.of(),false,"",List.of(),List.of());
        h.assertTrue(legacy.icon().equals("minecraft:stone"),"Existing constructor lost compatibility");h.succeed();
    }
    private GuideIconGameTests(){}
}

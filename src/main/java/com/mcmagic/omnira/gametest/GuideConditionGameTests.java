package com.mcmagic.omnira.gametest;
import com.google.gson.*;
import com.mcmagic.omnira.client.guide.*;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

@GameTestHolder("omnira_guide_conditions") @PrefixGameTestTemplate(false)
public class GuideConditionGameTests {
    @GameTest(template="spell_arena") public static void cataclysmVisibilityAndIndexes(GameTestHelper h) throws IOException {
        for(String locale:new String[]{"zh_cn","en_us"}) {
            var root=GuideResourceTree.load(path->{
                try(var in=GuideConditionGameTests.class.getResourceAsStream("/assets/omnira/guide/"+locale+"/"+path)) {
                    if(in==null)throw new IOException(path);
                    var data=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
                    if(path.endsWith("introduction.json"))
                        h.assertFalse(data.has("links"),"Internal links cannot be rendered on an introduction: "+path);
                    return data;
                }
            });
            var absent=GuideContents.fromJson(root,id->false);
            h.assertTrue(absent.destination("cataclysm_power").isEmpty()&&absent.destination("cataclysm_staff_tips").isEmpty(),"Hidden chapter or entry leaked");
            h.assertTrue(new GuideReferenceIndex(absent).item(new ItemStack(ModItems.STORM_TIP.get())).isEmpty(),"Hidden item quick reference leaked");
            h.assertTrue(new GuideTextSearch(absent).find(locale.equals("zh_cn")?"风暴杖端":"Storm Staff Tip").isEmpty(),"Hidden entry searchable");
            var present=GuideContents.fromJson(root,id->id.equals("cataclysm"));
            h.assertTrue(absent.destination("industrial_hand").isEmpty()&&absent.destination("industrial_wheel").isEmpty(),"Create guide leaked");
            var noCreate=absent.destination("create_integration").orElseThrow().entry();
            h.assertTrue(noCreate.links().stream().noneMatch(l->l.target().equals("industrial_hand")),"Conditional reference leaked");
            h.assertTrue(noCreate.text().stream().anyMatch(t->t.contains("工业之轮")||t.contains("Industrial Wheel")),"Permanent integration summary missing");
            var withCreate=GuideContents.fromJson(root,id->true);
            h.assertTrue(withCreate.destination("industrial_wheel").isPresent()&&withCreate.destination("create_integration").orElseThrow().entry().links().stream().anyMatch(l->l.target().equals("industrial_hand")),"Create guide missing");
            var children=withCreate.destination("cross_worlds").orElseThrow().chapter().chapters();
            h.assertTrue(children.getLast().id().equals("industrial_hand")&&children.get(children.size()-2).id().equals("cataclysm_power"),"Conditional chapter order");
            var dest=present.destination("cataclysm_power").orElseThrow();
            h.assertTrue(dest.path().size()==2&&dest.path().getFirst().id().equals("cross_worlds")&&dest.chapter().entries().size()==8,"Wrong hierarchy or missing parts");
            h.assertTrue(present.parentDirectory("cataclysm_staff_tips").chapter().id().equals("cataclysm_power"),"Entry back navigation not migrated");
            h.assertTrue(new GuideReferenceIndex(present).item(new ItemStack(ModItems.STORM_TIP.get())).orElseThrow().equals("cataclysm_staff_tips"),"Item lookup lost stable ID");
            var summaries=absent.destination("mod_integrations").orElseThrow().chapter().entries();
            h.assertTrue(summaries.stream().map(GuideContents.Entry::id).toList().equals(java.util.List.of(
                    "create_integration","aeronautics_integration","lootr_integration","cataclysm_integration")),"Missing integration summary");
            h.assertTrue(absent.destination("cataclysm_integration").orElseThrow().entry().links().isEmpty()
                    &&present.destination("cataclysm_integration").orElseThrow().entry().links().stream().anyMatch(l->l.target().equals("cataclysm_power")),"Incorrect Cataclysm detail link");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void mixedDirectoryRemovesOnlyHiddenPriority(GameTestHelper h){
        var root=JsonParser.parseString("{\"title\":\"Book\",\"chapters\":[{\"id\":\"parent\",\"title\":\"Parent\",\"icon\":\"minecraft:book\",\"entries\":[{\"id\":\"entry\",\"item\":\"minecraft:stone\",\"title\":\"Entry\",\"text\":[]}],\"chapters\":[{\"id\":\"optional\",\"title\":\"Optional\",\"icon\":\"minecraft:paper\",\"required_mod\":\"cataclysm\",\"entries\":[],\"chapters\":[]}],\"directory_order\":[\"optional\",\"entry\"]}]}").getAsJsonObject();
        var absent=GuideContents.fromJson(root,id->false);var present=GuideContents.fromJson(root,id->true);
        h.assertTrue(absent.chapters().getFirst().directory().size()==1&&absent.chapters().getFirst().directory().getFirst().entry().id().equals("entry"),"Hidden priority removed wrong row");
        h.assertTrue(present.chapters().getFirst().directory().size()==2,"Filtering mutated original resource");
        h.assertTrue(present.chapters().getFirst().directory().getFirst().entry().id().equals("entry"),"Optional chapter shifted permanent entry");h.succeed();
    }
}

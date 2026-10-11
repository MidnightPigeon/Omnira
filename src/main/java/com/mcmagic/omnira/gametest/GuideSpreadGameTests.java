package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.guide.GuideSpread;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guide_spread")
@PrefixGameTestTemplate(false)
public final class GuideSpreadGameTests {
    @GameTest(template="spell_arena") public static void constructionScenesAndUnspoiledRisk(GameTestHelper h) throws Exception {
        for(var language:java.util.List.of("zh_cn","en_us")){
            var book=GuidanceGameTests.guide(language);
            for(var id:java.util.List.of("advanced_assembly_table","advanced_crystal_aggregation_ring","large_alchemical_reaction_pool")){
                {
                    var entry=GuidanceGameTests.find(book,id);
                    h.assertTrue(entry!=null,"Missing construction guide: "+id);
                    h.assertTrue(entry.has("scene")&&!entry.get("scene").getAsString().isEmpty(),"Missing 3D construction scene: "+id);
                }
            }
            {
                var text=GuidanceGameTests.find(book,"void_crystal_ball").toString();
                h.assertTrue(text.contains(language.equals("zh_cn")?"风险":"risky"),"Missing shattering risk warning");
                h.assertTrue(!text.contains("时空聚合体")&&!text.contains("Spacetime Aggregate")&&!text.contains("spacetime storm"),"Guide spoiled shattering outcome");
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void entrySpreads(GameTestHelper h){
        for(int total=1;total<=15;total++){
            int seen=0,first=0;
            do{
                for(int side=0;side<2;side++){
                    int leaf=GuideSpread.leaf(first,side);
                    if(leaf<total){h.assertTrue(leaf==seen++,"Entry leaf skipped or repeated");}
                }
                if(!GuideSpread.hasNext(first,total))break;
                int next=GuideSpread.turn(first,true);
                h.assertTrue(GuideSpread.turn(next,false)==first,"Backtracking changed spread");first=next;
            }while(true);
            h.assertTrue(seen==total,"Last odd leaf lost");
        }
        h.assertTrue(GuideSpread.turn(0,false)==0,"Backtracking below zero");h.succeed();
    }
    @GameTest(template="spell_arena") public static void directorySpreads(GameTestHelper h){
        h.assertTrue(GuideSpread.directoryTotal(0,1)==1,"Empty directory created a phantom right page");
        h.assertTrue(GuideSpread.directoryLeaf(0,0)==0&&GuideSpread.directoryLeaf(1,0)==1,"Empty introductions reserved a sidebar");
        h.assertTrue(GuideSpread.directoryStart(1)==1&&GuideSpread.directoryStart(3)==3,"Introduction inserted a blank leaf");
        h.assertTrue(GuideSpread.directoryLeaf(1,1)==0,"Opening spread lost its right-hand directory");
        for(int intro=0;intro<5;intro++)for(int entries=0;entries<=40;entries++){
            int seen=0,total=GuideSpread.directoryTotal(entries,intro);
            for(int first=0;first<total;first+=2)for(int side=0;side<2;side++){
                int leaf=GuideSpread.leaf(first,side);
                if(leaf>=total)continue;
                int directory=GuideSpread.directoryLeaf(leaf,intro);
                if(directory<0)continue;
                for(int row=0;row<6&&directory*6+row<entries;row++)
                    h.assertTrue(directory*6+row==seen++,"Directory entry skipped or repeated");
            }
            h.assertTrue(seen==entries,"Directory entries lost behind introduction");
        }
        h.succeed();
    }
}

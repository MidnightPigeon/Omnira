package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.guide.GuideSpread;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guide_spread")
@PrefixGameTestTemplate(false)
public final class GuideSpreadGameTests {
    @GameTest(template="spell_arena") public static void backAlwaysUsesDirectoryHierarchy(GameTestHelper h)throws Exception {
        for(var language:java.util.List.of("zh_cn","en_us")) {
            var book=com.mcmagic.omnira.client.guide.GuideContents.fromJson(GuidanceGameTests.guide(language));
            for(int i=0;i<book.chapters().size();i++)checkParent(h,book,book.chapters().get(i),null,i);
        }
        for(int intro=0;intro<7;intro++)for(int row=0;row<70;row++) {
            int spread=GuideSpread.directorySpread(row,intro),leaf=intro+row/6;
            h.assertTrue((spread&1)==0&&leaf>=spread&&leaf<spread+2,"Parent directory page hidden or misaligned");
        }
        h.succeed();
    }
    private static void checkParent(GameTestHelper h,com.mcmagic.omnira.client.guide.GuideContents book,
            com.mcmagic.omnira.client.guide.GuideContents.Chapter chapter,com.mcmagic.omnira.client.guide.GuideContents.Chapter expected,int index){
        var parent=book.parentDirectory(chapter.id());
        h.assertTrue(parent.chapter()==expected&&parent.row()==index,"Chapter returned to history instead of parent: "+chapter.id());
        for(int i=0;i<chapter.directory().size();i++) {
            var row=chapter.directory().get(i);
            if(row.chapter()!=null)checkParent(h,book,row.chapter(),chapter,i);
            else {
                var entryParent=book.parentDirectory(row.entry().id());
                h.assertTrue(entryParent.chapter()==chapter&&entryParent.row()==i,"Entry returned to wrong directory: "+row.entry().id());
            }
        }
    }
    @GameTest(template="spell_arena") public static void captionWithoutImageIsProse(GameTestHelper h){
        var text=java.util.List.of("Introduction");
        var plain=new com.mcmagic.omnira.client.guide.GuideContents.Introduction("",112,88,"Caption",text,java.util.List.of());
        h.assertTrue(!plain.hasArtwork()&&plain.paragraphs().equals(java.util.List.of("Caption","Introduction")),"Caption reserved a blank image or disappeared");
        var art=new com.mcmagic.omnira.client.guide.GuideContents.Introduction("omnira:textures/gui/guide/chapters/beginning.png",112,88,"Caption",text,java.util.List.of());
        h.assertTrue(art.hasArtwork()&&art.paragraphs().equals(text),"Formal chapter duplicated its illustration caption");
        var empty=new com.mcmagic.omnira.client.guide.GuideContents.Introduction("",112,88,"",java.util.List.of(),java.util.List.of());
        h.assertTrue(!empty.hasArtwork()&&empty.paragraphs().isEmpty(),"Empty introduction created a page");
        for(int width:new int[]{88,124,160}){
            var pages=com.mcmagic.omnira.client.guide.GuideTextLayout.paginate(plain.paragraphs(),width,12,String::length);
            h.assertTrue(pages.size()==1&&GuideSpread.directoryLeaf(1,pages.size())==0,"Caption pushed the contents beyond the opening spread");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void diagramsDoNotShiftRecipesOrLinks(GameTestHelper h) {
        for(int text=1;text<=5;text++)for(int scenes=0;scenes<=1;scenes++)for(int figures=0;figures<=4;figures++)for(int recipes=0;recipes<=3;recipes++) {
            var layout=new com.mcmagic.omnira.client.guide.GuideEntryPages(text,scenes,figures,recipes);
            h.assertTrue(layout.figureStart()==text+scenes&&layout.recipeStart()==text+scenes+figures&&layout.linkStart()==text+scenes+figures+recipes,"Guide section offset mismatch");
            int seen=0;
            for(int first=0;first<layout.linkStart();first+=2)for(int side=0;side<2;side++) {
                int leaf=GuideSpread.leaf(first,side);if(leaf>=layout.linkStart())continue;
                if(leaf>=layout.recipeStart())h.assertTrue(leaf-layout.recipeStart()<recipes,"Recipe hit-test index mismatch");
                else if(leaf>=layout.figureStart())h.assertTrue(leaf-layout.figureStart()<figures,"Figure hit-test index mismatch");
                seen++;
            }
            h.assertTrue(seen==layout.linkStart(),"Page omitted or repeated");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void figureBoundsAndText(GameTestHelper h) throws Exception {
        for(var language:java.util.List.of("zh_cn","en_us"))checkFigures(h,GuidanceGameTests.guide(language));
        h.succeed();
    }
    private static void checkFigures(GameTestHelper h,com.google.gson.JsonObject node) {
        if(node.has("entries"))for(var value:node.getAsJsonArray("entries")) {
            var e=value.getAsJsonObject();
            for(var figure:com.mcmagic.omnira.client.guide.GuideFigure.read(e.getAsJsonArray("figures"))) {
                for(var icon:figure.icons())h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse(icon.item())),"Missing figure icon: "+icon.item());
                for(var label:figure.labels()) {
                    java.util.function.ToIntFunction<String> measure=s->s.codePoints().map(c->Character.isIdeographic(c)||c>0x3000?9:c==' '?4:6).sum();
                    var lines=com.mcmagic.omnira.client.guide.GuideTextLayout.wrap(label.text(),label.width(),measure);
                    h.assertTrue((lines.size()-1)*11+9<=label.height(),"Figure label overflows: "+e.get("id")+" / "+label.text());
                }
            }
        }
        if(node.has("chapters"))for(var child:node.getAsJsonArray("chapters"))checkFigures(h,child.getAsJsonObject());
    }
    @GameTest(template="spell_arena") public static void sourceCatalogueBounds(GameTestHelper h) throws Exception {
        for(var language:java.util.List.of("zh_cn","en_us")) {
            var book=GuidanceGameTests.guide(language);
            for(var id:java.util.List.of("item_sources","structure_sources")) {
                var sources=new java.util.ArrayList<com.mcmagic.omnira.client.guide.GuideContents.Source>();
                for(var value:GuidanceGameTests.find(book,id).getAsJsonObject("introduction").getAsJsonArray("sources")) {
                    var row=value.getAsJsonObject();
                    sources.add(new com.mcmagic.omnira.client.guide.GuideContents.Source(
                            row.has("icon")?row.get("icon").getAsString():"",row.get("name").getAsString(),row.get("origin").getAsString()));
                }
                sources.add(new com.mcmagic.omnira.client.guide.GuideContents.Source("omnira:guide_book","Long resource-pack name ".repeat(30),"Test"));
                for(int glyphWidth:new int[]{6,9,12}) {
                    java.util.function.ToIntFunction<String> measure=s->s.codePointCount(0,s.length())*glyphWidth;
                    var pages=com.mcmagic.omnira.client.guide.GuideSourceLayout.pages(sources,measure);
                    var restored=new java.util.LinkedHashMap<com.mcmagic.omnira.client.guide.GuideContents.Source,StringBuilder>();
                    for(var page:pages) {
                        h.assertTrue(page.stream().mapToInt(com.mcmagic.omnira.client.guide.GuideSourceLayout.Row::height).sum()<=132,"Catalogue leaf overflow");
                        for(var row:page)for(var line:row.lines()) {
                            h.assertTrue(measure.applyAsInt(line)<=(row.source().icon().isBlank()?124:104),"Catalogue line overflow");
                            restored.computeIfAbsent(row.source(),ignored->new StringBuilder()).append(line);
                        }
                    }
                    for(var source:sources)h.assertTrue(restored.get(source).toString().replaceAll("\\s","").equals(
                            (source.name()+"\u2014\u2014"+source.origin()).replaceAll("\\s","")),"Catalogue lost attribution text");
                }
            }
        }
        h.succeed();
    }
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

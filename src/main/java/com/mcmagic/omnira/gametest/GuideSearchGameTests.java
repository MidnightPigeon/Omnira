package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.guide.*;
import com.mcmagic.omnira.client.screen.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guide_search") @PrefixGameTestTemplate(false)
public final class GuideSearchGameTests {
    private static GuideContents.Introduction intro(String caption,List<String> text,List<GuideContents.Source> sources){return new GuideContents.Introduction("",112,88,caption,text,sources);}
    private static GuideContents fixture(){
        var empty=intro("",List.of(),List.of());
        var childEntry=new GuideContents.Entry("child_entry","minecraft:stone","Child Entry",List.of("Deep needle text"),false,"",List.of(),List.of());
        var child=new GuideContents.Chapter("child","Child Chapter","minecraft:stone",empty,List.of(childEntry),List.of(),List.of(new GuideContents.DirectoryRow(childEntry,null)));
        var diagram=new GuideFigure("Diagram Name",List.of(),List.of(),List.of(),List.of(new GuideFigure.Label("Diagram needle",0,0,100,20,0)));
        var entry=new GuideContents.Entry("entry","minecraft:paper","Entry Name",List.of("ABC 123 needle", "A second needle","{dream}Colorful passage"),false,"private_scene_identifier",List.of(),List.of(diagram));
        var chapter=new GuideContents.Chapter("chapter","Parent Chapter","minecraft:book",intro("Introduction caption",List.of("Chapter prose"),List.of(new GuideContents.Source("","Source object","Source IP"))),List.of(entry),List.of(child),List.of(new GuideContents.DirectoryRow(entry,null),new GuideContents.DirectoryRow(null,child)));
        return new GuideContents("Test Book",intro("Opening prose",List.of(),List.of()),List.of(chapter),Map.of());
    }
    @GameTest(template="spell_arena") public static void textMatchesItsOwnContainerWithoutAncestorDuplication(GameTestHelper h){
        var index=new GuideTextSearch(fixture());
        h.assertTrue(index.find("needle").stream().map(GuideTextSearch.Hit::id).toList().equals(List.of("entry","child_entry")),"Matched ancestor directories or duplicated a repeated match");
        for(String query:List.of("Diagram Name","Diagram needle","ＡＢＣ　１２３"))h.assertTrue(index.find(query).getFirst().id().equals("entry"),"Visible figure or normalized text ignored");
        for(String query:List.of("Introduction caption","Chapter prose","source ip","Source object\u2014\u2014Source IP"))h.assertTrue(index.find(query).getFirst().id().equals("chapter"),"Introduction/source text has no chapter result");
        h.assertTrue(index.find("Opening prose").getFirst().id().isEmpty()&&index.find("private_scene_identifier").isEmpty(),"Opening text or hidden identifier indexing incorrect");
        h.assertTrue(index.find("{dream}").isEmpty()&&index.find("dream").isEmpty()&&index.find("Colorful passage").getFirst().id().equals("entry"),"Invisible color marker became searchable");
        h.assertTrue(index.find(" ").isEmpty()&&index.find("unmatched phrase").isEmpty(),"Blank or absent text matched");h.succeed();
    }
    @GameTest(template="spell_arena") public static void resultsRootBackIsDistinctFromChildDirectoryBack(GameTestHelper h){
        var session=new GuideSearchSession(fixture());var before=new GuideSearchSession.Position("entry",4);
        h.assertTrue(!session.start("　 ",before)&&!session.active(),"Blank query entered search mode");
        h.assertTrue(session.start("chapter",before)&&session.active()&&session.results(),"Search did not create independent results");
        var parent=session.hits().stream().filter(hit->hit.id().equals("chapter")).findFirst().orElseThrow();session.open(parent);
        h.assertTrue(session.returnsToResults("chapter")&&!session.returnsToResults("child")&&!session.back("child_entry"),"Child back incorrectly jumped to search");
        h.assertTrue(!session.results()&&session.back("chapter")&&session.results(),"Direct result did not return to search");
        var origin=session.stop();h.assertTrue(before.equals(origin)&&!session.active()&&!session.results()&&session.query().isEmpty()&&session.hits().isEmpty()&&session.page()==0,"Stop did not restore origin and clear state");
        session.start("Opening prose",before);session.open(session.hits().getFirst());h.assertTrue(session.back(""),"Opening-page result has no return to search");session.stop();
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void bilingualResultsResolveAndPaginationIsComplete(GameTestHelper h)throws Exception{
        for(var language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuidanceGameTests.guide(language));var session=new GuideSearchSession(book);
            session.start(language.equals("zh_cn")?"水晶":"crystal",new GuideSearchSession.Position("",0));
            h.assertTrue(session.hits().size()>12,"Expected broad query to span several spreads");
            int seen=0;
            do {
                for(int side=0;side<2;side++)for(int row=0;row<6;row++){
                    int index=(session.page()+side)*6+row;if(index>=session.hits().size())continue;
                    var hit=session.hits().get(index);h.assertTrue(index==seen++,"Search pagination skipped a result");
                    if(!hit.id().isEmpty())h.assertTrue(book.destination(hit.id()).isPresent(),"Search links to missing destination");
                }
                if(!GuideSpread.hasNext(session.page(),session.totalPages()))break;session.turn(true);
            }while(true);
            int saved=session.page();var hit=session.hits().getFirst();session.open(hit);session.back(hit.id());
            h.assertTrue(seen==session.hits().size()&&session.page()==saved,"Returning from a result lost the results spread");
            session.stop();session.start("unique-absent-search-text",new GuideSearchSession.Position("",0));
            session.turn(true);session.turn(false);h.assertTrue(session.hits().isEmpty()&&session.results()&&session.totalPages()==2&&session.page()==0,"No-results spread is missing or can turn to phantom pages");
            session.stop();h.assertTrue(session.query().isEmpty()&&!session.active(),"Home/stop failed to clear search");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void detachedSearchControlsFitAllViewports(GameTestHelper h){
        for(int width:new int[]{240,320,480,854,1920})for(int height:new int[]{160,224,256,480,1080}){
            var viewport=UiViewport.layout(width,height,GuideBookLayout.WIDTH,GuideBookLayout.HEIGHT);
            var layout=GuideBookLayout.centered(viewport.width(),viewport.height());
            h.assertTrue(layout.left()>=0&&layout.top()>=0&&layout.left()+320<=viewport.width()&&layout.top()+250<=viewport.height(),"Book/search escapes viewport");
            h.assertTrue(GuideBookLayout.SEARCH_Y>GuideBookLayout.BOOK_HEIGHT&&GuideBookLayout.INPUT_X+GuideBookLayout.INPUT_WIDTH<GuideBookLayout.FIND_X&&GuideBookLayout.FIND_X+GuideBookLayout.FIND_WIDTH<=320,"Search controls overlap book or each other");
        }h.succeed();
    }
}

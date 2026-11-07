package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.client.guide.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@GameTestHolder("omnira_guide_tree") @PrefixGameTestTemplate(false)
public final class GuideTreeGameTests {
    private static JsonObject resource(String path)throws IOException{
        try(var in=GuideTreeGameTests.class.getResourceAsStream("/assets/omnira/guide/"+path)){
            if(in==null)throw new IOException(path);return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @GameTest(template="spell_arena") public static void bilingualTreeKeepsFormalSearchSeparate(GameTestHelper h)throws Exception{
        Set<String> first=null;
        for(String language:List.of("zh_cn","en_us")){
            var book=GuideContents.fromJson(GuideResourceTree.load(path->resource(language+"/"+path)),id->false);
            var index=new GuideTextSearch(book);var tree=GuideTree.read(resource("tree.json"),resource(language+"/tree_pages.json"),book);
            var ids=new HashSet<String>();int summaries=0,rings=0;
            var outside=tree.nodes().stream().filter(n->n.id().equals("cross_worlds")).findFirst().orElseThrow();
            h.assertTrue(outside.ring() && outside.x()==0 && outside.y()==tree.radius() && outside.overview()
                    && book.destination(outside.target()).orElseThrow().chapter()!=null,"Missing bottom-ring chapter shortcut");
            for(var n:tree.nodes()){
                h.assertTrue(ids.add(n.id()),"Duplicate map node");
                if(n.ring()){rings++;h.assertTrue(Math.abs(Math.hypot(n.x(),n.y())-tree.radius())<1e-8,"Outer node is not on a true circle");}
                if(n.mapPage()!=null){
                    summaries++;h.assertTrue(book.destination(n.target()).isEmpty(),"Map summary entered the book directory");
                    h.assertTrue(index.find(n.title()).stream().noneMatch(hit->hit.id().equals(n.target())),"Map summary entered search");
                    h.assertTrue(!n.mapPage().recipes()&&n.mapPage().scene().isEmpty(),"Summary acquired independent recipes or scenes");
                    for(var link:n.mapPage().links())h.assertTrue(book.destination(link.target()).isPresent(),"Summary links to invisible/missing page");
                }else h.assertTrue(book.destination(n.target()).isPresent(),"Formal destination missing");
            }
            h.assertTrue(summaries>0&&rings>0,"Expected summary pages and outer-ring entries");
            h.assertTrue(tree.nodes().stream().anyMatch(n->n.target().equals("spacetime_engine")&&n.overview()&&n.mapPage()==null),"Missing prominent direct Spacetime Engine entry");
            for(var n:tree.nodes())for(var parent:n.parents())h.assertTrue(ids.contains(parent),"Edge points to removed node");
            if(first==null)first=ids;else h.assertTrue(first.equals(ids),"Localized node identities differ");
            h.assertTrue(index.find(language.equals("zh_cn")?"水晶":"crystal").size()>10,"Formal search was damaged");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void conditionalTargetsAndSummaryLinksFollowBookVisibility(GameTestHelper h)throws Exception{
        var root=GuideResourceTree.load(path->resource("en_us/"+path));
        var layout=JsonParser.parseString("{\"radius\":720,\"nodes\":[{\"id\":\"optional\",\"target\":\"industrial_wheel\",\"stage\":1,\"kind\":\"branch\",\"x\":0,\"y\":0},{\"id\":\"summary\",\"target\":\"map:optional\",\"stage\":1,\"kind\":\"branch\",\"x\":100,\"y\":0,\"parent\":\"optional\"}]}").getAsJsonObject();
        var pages=JsonParser.parseString("{\"pages\":[{\"id\":\"map:optional\",\"title\":\"Optional\",\"icon\":\"omnira:guide_book\",\"text\":[\"Map only\"],\"links\":[\"industrial_wheel\"]}]}").getAsJsonObject();
        h.assertTrue(GuideTree.read(layout,pages,GuideContents.fromJson(root,id->false)).nodes().isEmpty(),"Hidden integration leaked into map");
        var visible=GuideTree.read(layout,pages,GuideContents.fromJson(root,id->id.equals("create")));
        h.assertTrue(visible.nodes().size()==2&&visible.nodes().getLast().mapPage().links().size()==1,"Visible integration missing");h.succeed();
    }
    @GameTest(template="spell_arena") public static void circleCameraZoomAnchorAndRecenter(GameTestHelper h){
        var view=new GuideTreeView();
        for(int width:new int[]{240,360,854,1920})for(int height:new int[]{180,480,1080}){
            view.fit(width,height,720);double cx=width/2.0,cy=height/2.0;
            double horizontal=view.screenX(720,cx)-view.screenX(-720,cx),vertical=view.screenY(720,cy)-view.screenY(-720,cy);
            h.assertTrue(Math.abs(horizontal-vertical)<1e-8,"Viewport stretched the circle");
            double px=cx+17,py=cy-21,wx=view.worldX(px,cx),wy=view.worldY(py,cy);
            view.zoomAt(1.3,px,py,cx,cy);
            h.assertTrue(Math.abs(wx-view.worldX(px,cx))<1e-8&&Math.abs(wy-view.worldY(py,cy))<1e-8,"Zoom moved cursor's map position");
            view.pan(35,-12);double zoom=view.zoom();view.center(2);
            h.assertTrue(view.zoom()==zoom&&view.x()==0&&view.y()==GuideTreeView.stageY(2),"Recenter changed zoom or stage");
            // Returning from reading does not reconstruct or reset this camera.
            double x=view.x(),y=view.y();view.screenX(50,cx);view.screenY(50,cy);
            h.assertTrue(x==view.x()&&y==view.y()&&zoom==view.zoom(),"Projection mutated camera");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void dependenciesAreAcyclicAndDoNotInventProgression(GameTestHelper h)throws Exception{
        var book=GuideContents.fromJson(GuideResourceTree.load(path->resource("zh_cn/"+path)),id->true);
        var tree=GuideTree.read(resource("tree.json"),resource("zh_cn/tree_pages.json"),book);
        var nodes=new HashMap<String,GuideTree.Node>();tree.nodes().forEach(n->nodes.put(n.id(),n));
        for(var n:tree.nodes())visit(n,nodes,new HashSet<>());
        var star=nodes.get("fulfillment_star");
        h.assertTrue(star!=null&&star.stage()==1&&star.parents().getFirst().equals("advanced_condensation_table"),"Fulfillment Star should be reachable after dream condensation");
        h.assertTrue(star.mapPage()==null&&star.target().equals("fulfillment_star"),"Star must open its existing formal entry");
        for(var n:tree.nodes())if(n!=star)h.assertTrue(Math.hypot(star.x()-n.x(),star.y()-n.y())>=40,"Star overlaps another node");
        h.assertTrue(nodes.get("map:aggregation_materials").parents().equals(List.of("map:spacetime_resources")),"Naturally obtainable ring materials must branch directly from spacetime exploration");
        h.assertTrue(nodes.get("world_rotation").parents().contains("myriad_aggregation")&&!nodes.get("world_rotation").parents().contains("omni_crystal_grid"),"World Rotation incorrectly requires the grid");
        h.assertTrue(!nodes.get("map:first_cores").parents().contains("spacetime_reverser"),"Bootstrap microcores depend on their own consumer");
        h.assertTrue(nodes.get("crystal_ball").ring()&&nodes.get("liquid_crystal_ball").ring()&&Math.hypot(nodes.get("crystal_ball").x()-nodes.get("liquid_crystal_ball").x(),nodes.get("crystal_ball").y()-nodes.get("liquid_crystal_ball").y())<100,"Storage family split apart");
        h.succeed();
    }
    private static void visit(GuideTree.Node node,Map<String,GuideTree.Node> nodes,Set<String> path){
        if(!path.add(node.id()))throw new IllegalStateException("Cyclic map route: "+node.id());
        for(String id:node.parents())visit(nodes.get(id),nodes,path);path.remove(node.id());
    }
}

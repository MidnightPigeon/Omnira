package com.mcmagic.omnira.client.guide;

import com.google.gson.*;
import com.mcmagic.omnira.Omnira;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.util.*;

/** Separate map catalogue: never inserted into GuideContents, search, or item-reference indexes. */
public record GuideTree(double radius,List<Node> nodes) {
    public static final int COLUMN_WIDTH=144, ROW_HEIGHT=48;
    public record Node(String id,int stage,String kind,double x,double y,String target,String title,String icon,
                       GuideContents.Entry mapPage,List<String> parents,List<String> related,boolean overview) {
        public boolean ring(){return kind.equals("ring");}
    }
    public static GuideTree read(JsonObject layout,JsonObject localized,GuideContents book){
        double radius=layout.get("radius").getAsDouble();
        if(!Double.isFinite(radius)||radius<100||radius>10000)throw new IllegalArgumentException("Invalid guide circle radius");
        var pages=new HashMap<String,GuideContents.Entry>();
        for(var value:localized.getAsJsonArray("pages")){
            var p=value.getAsJsonObject();String id=p.get("id").getAsString();
            if(!id.startsWith("map:")||book.destination(id).isPresent()||pages.containsKey(id))throw new IllegalArgumentException("Invalid map-only page: "+id);
            var links=new ArrayList<GuideContents.Link>();
            for(var ref:p.getAsJsonArray("links"))book.destination(ref.getAsString()).ifPresent(d->links.add(new GuideContents.Link(ref.getAsString(),d.title())));
            if(links.isEmpty())continue;
            var text=p.getAsJsonArray("text").asList().stream().map(JsonElement::getAsString).toList();
            String icon=p.get("icon").getAsString();
            pages.put(id,new GuideContents.Entry(id,icon,icon,p.get("title").getAsString(),text,false,"",List.copyOf(links),List.of()));
        }
        var nodes=new ArrayList<Node>();var ids=new HashSet<String>();
        for(var value:layout.getAsJsonArray("nodes")){
            var n=value.getAsJsonObject();String id=n.get("id").getAsString(),target=n.get("target").getAsString(),kind=n.get("kind").getAsString();
            if(!ids.add(id))throw new IllegalArgumentException("Duplicate tree node: "+id);
            int stage=n.get("stage").getAsInt();
            boolean grid=!kind.equals("ring")&&n.has("column");
            double x=grid?n.get("column").getAsBigDecimal().intValueExact()*(double)COLUMN_WIDTH:n.get("x").getAsDouble();
            double y=grid?n.get("row").getAsBigDecimal().intValueExact()*(double)ROW_HEIGHT:n.get("y").getAsDouble();
            boolean outsideY=kind.equals("ring")?Math.abs(y)>radius:Math.abs(y)>=radius;
            if(stage<0||stage>3||!Double.isFinite(x)||!Double.isFinite(y)||outsideY||Math.abs(x)>=radius)throw new IllegalArgumentException("Invalid tree node position: "+id);
            if(kind.equals("ring"))x=(n.get("ring_side").getAsInt()<0?-1:1)*Math.sqrt(radius*radius-y*y);
            var mapPage=pages.get(target);var formal=book.destination(target);
            // The book has already applied required_mod visibility to chapters, entries and links.
            if(mapPage==null&&formal.isEmpty())continue;
            String title=mapPage!=null?mapPage.title():formal.orElseThrow().title();
            String icon=mapPage!=null?mapPage.icon():formal.orElseThrow().entry()!=null?formal.get().entry().icon():formal.get().chapter().icon();
            var parents=new ArrayList<String>();
            if(n.has("parent")&&!n.get("parent").isJsonNull()&&!n.get("parent").getAsString().isBlank())parents.add(n.get("parent").getAsString());
            if(n.has("also"))for(var p:n.getAsJsonArray("also"))if(!parents.contains(p.getAsString()))parents.add(p.getAsString());
            var related=n.has("related")?n.getAsJsonArray("related").asList().stream().map(JsonElement::getAsString).distinct().toList():List.<String>of();
            nodes.add(new Node(id,stage,kind,x,y,target,title,icon,mapPage,List.copyOf(parents),related,n.has("overview")&&n.get("overview").getAsBoolean()));
        }
        var visible=new HashSet<String>();nodes.forEach(n->visible.add(n.id()));
        return new GuideTree(radius,nodes.stream().map(n->new Node(n.id(),n.stage(),n.kind(),n.x(),n.y(),n.target(),n.title(),n.icon(),n.mapPage(),n.parents().stream().filter(visible::contains).toList(),n.related().stream().filter(visible::contains).toList(),n.overview())).toList());
    }
    public static GuideTree load(GuideContents book){
        var mc=Minecraft.getInstance();String language=mc.getLanguageManager().getSelected();
        try{
            var layout=resource("guide/tree.json");
            for(String locale:new LinkedHashSet<>(List.of(language,language.startsWith("en")?"en_us":"zh_cn","zh_cn"))){
                var path="guide/"+locale+"/tree_pages.json";
                if(mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("omnira",path)).isPresent())return read(layout,resource(path),book);
            }
        }catch(Exception e){Omnira.LOGGER.error("Cannot load guide tree",e);}
        return new GuideTree(720,List.of());
    }
    private static JsonObject resource(String path)throws IOException{
        var resource=Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("omnira",path)).orElseThrow(()->new IOException(path));
        try(var reader=resource.openAsReader()){return JsonParser.parseReader(reader).getAsJsonObject();}
    }
}

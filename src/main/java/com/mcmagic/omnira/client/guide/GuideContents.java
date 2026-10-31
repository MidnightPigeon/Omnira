package com.mcmagic.omnira.client.guide;

import com.google.gson.*;
import com.mcmagic.omnira.Omnira;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Localized resource-pack content; reopening the book picks up F3+T changes. */
public record GuideContents(String title,Introduction introduction,List<Chapter> chapters,Map<String,String> aliases) {
    public record Source(String icon,String name,String origin) {}
    public record Introduction(String image,int imageWidth,int imageHeight,String caption,List<String> text,List<Source> sources,List<GuideExternalLink> externalLinks) {
        public Introduction(String image,int imageWidth,int imageHeight,String caption,List<String> text,List<Source> sources){
            this(image,imageWidth,imageHeight,caption,text,sources,List.of());
        }
        public int linesPerPage(){return externalLinks.isEmpty()?12:10;}
        public boolean hasArtwork(){return !image.isBlank();}
        public List<String> paragraphs(){
            if(hasArtwork()||caption.isBlank())return text;
            var paragraphs=new ArrayList<String>();paragraphs.add(caption);paragraphs.addAll(text);
            return List.copyOf(paragraphs);
        }
    }
    public record Chapter(String id,String title,String icon,Introduction introduction,List<Entry> entries,List<Chapter> chapters,List<DirectoryRow> directory) {}
    public record DirectoryRow(Entry entry,Chapter chapter) {
        public String title(){return entry!=null?entry.title():chapter.title();}
        public String icon(){return entry!=null?entry.icon():chapter.icon();}
    }
    public record Link(String target,String label) {}
    public record Entry(String id,String item,String icon,String title,List<String> text,boolean recipes,String scene,List<Link> links,List<GuideFigure> figures,List<String> relatedItems) {
        public Entry(String id,String item,String icon,String title,List<String> text,boolean recipes,String scene,List<Link> links,List<GuideFigure> figures){
            this(id,item,icon,title,text,recipes,scene,links,figures,List.of());
        }
        public Entry(String id,String item,String title,List<String> text,boolean recipes,String scene,List<Link> links,List<GuideFigure> figures){
            this(id,item,item,title,text,recipes,scene,links,figures);
        }
    }
    public record Destination(List<Chapter> path,Entry entry) {
        public Chapter chapter() {return path.getLast();}
        public String title() {return entry==null?chapter().title():entry.title();}
    }
    public Optional<Destination> destination(String id) {return id==null?Optional.empty():locate(chapters,List.of(),aliases.getOrDefault(id,id),null);}
    public Optional<Destination> itemDestination(String item) {return locate(chapters,List.of(),null,item);}
    public record DirectoryLocation(Chapter chapter,int row) {}
    public DirectoryLocation parentDirectory(String id) {
        var target=destination(id).orElseThrow();
        var path=target.path();
        var parent=target.entry()!=null?target.chapter():path.size()>1?path.get(path.size()-2):null;
        String child=target.entry()!=null?target.entry().id():target.chapter().id();
        if(parent==null) {
            for(int i=0;i<chapters.size();i++)if(chapters.get(i).id().equals(child))return new DirectoryLocation(null,i);
        } else {
            var rows=parent.directory();
            for(int i=0;i<rows.size();i++) {
                var row=rows.get(i);
                if((row.entry()!=null?row.entry().id():row.chapter().id()).equals(child))return new DirectoryLocation(parent,i);
            }
        }
        throw new IllegalArgumentException("Missing parent directory row: "+id);
    }
    private static Optional<Destination> locate(List<Chapter> chapters,List<Chapter> parents,String id,String item) {
        for(var chapter:chapters) {
            var path=new ArrayList<>(parents);path.add(chapter);
            if(chapter.id().equals(id)) return Optional.of(new Destination(List.copyOf(path),null));
            for(var row:chapter.directory()) {
                if(row.entry()!=null) {
                    var entry=row.entry();
                    if(entry.id().equals(id)||entry.item().equals(item)||item!=null&&entry.relatedItems().contains(item))return Optional.of(new Destination(List.copyOf(path),entry));
                }else{
                    var child=locate(List.of(row.chapter()),path,id,item);
                    if(child.isPresent())return child;
                }
            }
        }
        return Optional.empty();
    }
    private static Introduction introduction(JsonObject object) {
        var intro=object.has("introduction")?object.getAsJsonObject("introduction"):new JsonObject();
        var text=new ArrayList<String>();
        if(intro.has("text")) for(var paragraph:intro.getAsJsonArray("text")) text.add(paragraph.getAsString());
        var sources=new ArrayList<Source>();
        if(intro.has("sources")) for(var value:intro.getAsJsonArray("sources")) {
            var source=value.getAsJsonObject();
            sources.add(new Source(source.has("icon")?source.get("icon").getAsString():"",
                    source.get("name").getAsString(),source.get("origin").getAsString()));
        }
        return new Introduction(intro.has("image")?intro.get("image").getAsString():"",
                intro.has("image_width")?Math.max(1,intro.get("image_width").getAsInt()):112,
                intro.has("image_height")?Math.max(1,intro.get("image_height").getAsInt()):88,
                intro.has("caption")?intro.get("caption").getAsString():"",List.copyOf(text),List.copyOf(sources),GuideExternalLink.read(intro.getAsJsonArray("external_links")));
    }
    private static Chapter chapter(JsonObject chapter) {
        var entries=new ArrayList<Entry>();
        if(chapter.has("entries")) for(var item:chapter.getAsJsonArray("entries")) {
            var entry=item.getAsJsonObject();var text=new ArrayList<String>();
            for(var paragraph:entry.getAsJsonArray("text")) text.add(paragraph.getAsString());
            var links=new ArrayList<Link>();
            if(entry.has("links")) for(var value:entry.getAsJsonArray("links")) {
                var link=value.getAsJsonObject();
                links.add(new Link(link.get("target").getAsString(),link.get("label").getAsString()));
            }
            entries.add(new Entry(entry.get("id").getAsString(),entry.get("item").getAsString(),
                    entry.has("icon")?entry.get("icon").getAsString():entry.get("item").getAsString(),
                    entry.get("title").getAsString(),List.copyOf(text),!entry.has("recipes") || entry.get("recipes").getAsBoolean(),entry.has("scene")?entry.get("scene").getAsString():"",List.copyOf(links),GuideFigure.read(entry.getAsJsonArray("figures")),entry.has("related_items")?entry.getAsJsonArray("related_items").asList().stream().map(JsonElement::getAsString).toList():List.of()));
        }
        var children=new ArrayList<Chapter>();
        if(chapter.has("chapters")) for(var child:chapter.getAsJsonArray("chapters")) children.add(chapter(child.getAsJsonObject()));
        var rows=new LinkedHashMap<String,DirectoryRow>();
        for(var entry:entries)rows.put(entry.id(),new DirectoryRow(entry,null));
        for(var child:children)rows.put(child.id(),new DirectoryRow(null,child));
        var directory=new ArrayList<DirectoryRow>();
        if(!entries.isEmpty()&&!children.isEmpty()&&!chapter.has("directory_order"))throw new IllegalArgumentException("Mixed guide directory requires explicit dependency order: "+chapter.get("id"));
        if(chapter.has("directory_order"))for(var id:chapter.getAsJsonArray("directory_order")) {
            var row=rows.remove(id.getAsString());
            if(row==null)throw new IllegalArgumentException("Unknown or duplicate directory priority: "+id);
            directory.add(row);
        }
        if(chapter.has("directory_order")&&!rows.isEmpty())throw new IllegalArgumentException("Incomplete guide dependency order: "+chapter.get("id"));
        directory.addAll(rows.values());
        return new Chapter(chapter.get("id").getAsString(),chapter.get("title").getAsString(),
                chapter.get("icon").getAsString(),introduction(chapter),List.copyOf(entries),List.copyOf(children),List.copyOf(directory));
    }
    public static GuideContents fromJson(JsonObject root) {
        var chapters=new ArrayList<Chapter>();
        for(var value:root.getAsJsonArray("chapters"))chapters.add(chapter(value.getAsJsonObject()));
        var aliases=new LinkedHashMap<String,String>();
        if(root.has("aliases"))for(var entry:root.getAsJsonObject("aliases").entrySet())aliases.put(entry.getKey(),entry.getValue().getAsString());
        return new GuideContents(root.get("title").getAsString(),introduction(root),List.copyOf(chapters),Map.copyOf(aliases));
    }
    public static ItemStack icon(String item) {
        var id=ResourceLocation.tryParse(item);
        return id==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(id));
    }
    public static GuideContents load() {
        var mc=Minecraft.getInstance();
        String language=mc.getLanguageManager().getSelected();
        var candidates=new LinkedHashSet<>(List.of(language,language.startsWith("en")?"en_us":"zh_cn","zh_cn"));
        for(String locale:candidates) {
            try {
                var root=GuideResourceTree.load(path->{
                    // A resource pack may replace a single entry; missing localized files fall back individually.
                    var fallbacks=new LinkedHashSet<>(List.of(locale,locale.startsWith("en")?"en_us":"zh_cn","zh_cn"));
                    for(String fallback:fallbacks) {
                        var resource=mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("omnira","guide/"+fallback+"/"+path));
                        if(resource.isPresent())try(var reader=resource.get().openAsReader()) {
                            return JsonParser.parseReader(reader).getAsJsonObject();
                        }
                    }
                    throw new java.io.IOException("Missing guide resource: "+locale+"/"+path);
                });
                return fromJson(root);
            } catch(Exception exception) {Omnira.LOGGER.error("Cannot read guide language {}",locale,exception);}
        }
        return new GuideContents("Omnira",introduction(new JsonObject()),List.of(),Map.of());
    }
}

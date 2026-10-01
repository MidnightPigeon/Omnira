package com.mcmagic.omnira.client.guide;

import com.google.gson.*;
import com.mcmagic.omnira.Omnira;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Localized resource-pack content; reopening the book picks up F3+T changes. */
public record GuideContents(String title,Introduction introduction,List<Chapter> chapters) {
    public record Introduction(String image,int imageWidth,int imageHeight,String caption,List<String> text) {}
    public record Chapter(String id,String title,String icon,Introduction introduction,List<Entry> entries,List<Chapter> chapters) {}
    public record Link(String target,String label) {}
    public record Entry(String id,String item,String title,List<String> text,boolean recipes,String scene,List<Link> links) {}
    public record Destination(List<Chapter> path,Entry entry) {
        public Chapter chapter() {return path.getLast();}
        public String title() {return entry==null?chapter().title():entry.title();}
    }
    public Optional<Destination> destination(String id) {return locate(chapters,List.of(),id,null);}
    public Optional<Destination> itemDestination(String item) {return locate(chapters,List.of(),null,item);}
    private static Optional<Destination> locate(List<Chapter> chapters,List<Chapter> parents,String id,String item) {
        for(var chapter:chapters) {
            var path=new ArrayList<>(parents);path.add(chapter);
            if(chapter.id().equals(id)) return Optional.of(new Destination(List.copyOf(path),null));
            for(var entry:chapter.entries())
                if(entry.id().equals(id) || entry.item().equals(item)) return Optional.of(new Destination(List.copyOf(path),entry));
            var child=locate(chapter.chapters(),path,id,item);
            if(child.isPresent()) return child;
        }
        return Optional.empty();
    }
    private static Introduction introduction(JsonObject object) {
        var intro=object.has("introduction")?object.getAsJsonObject("introduction"):new JsonObject();
        var text=new ArrayList<String>();
        if(intro.has("text")) for(var paragraph:intro.getAsJsonArray("text")) text.add(paragraph.getAsString());
        return new Introduction(intro.has("image")?intro.get("image").getAsString():"",
                intro.has("image_width")?Math.max(1,intro.get("image_width").getAsInt()):112,
                intro.has("image_height")?Math.max(1,intro.get("image_height").getAsInt()):88,
                intro.has("caption")?intro.get("caption").getAsString():"",List.copyOf(text));
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
                    entry.get("title").getAsString(),List.copyOf(text),!entry.has("recipes") || entry.get("recipes").getAsBoolean(),entry.has("scene")?entry.get("scene").getAsString():"",List.copyOf(links)));
        }
        var children=new ArrayList<Chapter>();
        if(chapter.has("chapters")) for(var child:chapter.getAsJsonArray("chapters")) children.add(chapter(child.getAsJsonObject()));
        return new Chapter(chapter.get("id").getAsString(),chapter.get("title").getAsString(),
                chapter.get("icon").getAsString(),introduction(chapter),List.copyOf(entries),List.copyOf(children));
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
                var chapters=new ArrayList<Chapter>();
                for(var value:root.getAsJsonArray("chapters")) {
                    chapters.add(chapter(value.getAsJsonObject()));
                }
                return new GuideContents(root.get("title").getAsString(),introduction(root),List.copyOf(chapters));
            } catch(Exception exception) {Omnira.LOGGER.error("Cannot read guide language {}",locale,exception);}
        }
        return new GuideContents("Omnira",introduction(new JsonObject()),List.of());
    }
}

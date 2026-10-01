package com.mcmagic.omnira.client.guide;

import com.google.gson.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Resolves small, relative resource files without depending on the client or filesystem. */
public final class GuideResourceTree {
    @FunctionalInterface public interface Reader {JsonObject read(String path) throws IOException;}
    private final Reader reader;
    private final Set<String> files=new HashSet<>();
    private GuideResourceTree(Reader reader) {this.reader=reader;}
    public static JsonObject load(Reader reader) throws IOException {
        return new GuideResourceTree(reader).node("book.json",0);
    }
    private JsonObject read(String path) throws IOException {
        if(!files.add(path))throw new IOException("Duplicate guide reference: "+path);
        if(files.size()>2048)throw new IOException("Too many guide files");
        try {return reader.read(path).deepCopy();}
        catch(RuntimeException e) {throw new IOException("Invalid guide file: "+path,e);}
    }
    private static String relative(String owner,String ref) throws IOException {
        if(!ref.matches("[a-z0-9_-]+(?:/[a-z0-9_-]+)*\\.json"))throw new IOException("Invalid guide reference in "+owner+": "+ref);
        int slash=owner.lastIndexOf('/');return owner.substring(0,slash+1)+ref;
    }
    private JsonObject node(String path,int depth) throws IOException {
        if(depth>16)throw new IOException("Guide nesting exceeds 16 chapters: "+path);
        var index=read(path);
        if(depth==0 && (!index.has("format") || index.get("format").getAsInt()!=2))throw new IOException("Expected guide format 2: "+path);
        index.remove("format");
        index.add("introduction",read(relative(path,index.get("introduction").getAsString())));
        var entries=new JsonArray();
        if(index.has("entries"))for(var ref:index.getAsJsonArray("entries"))entries.add(read(relative(path,ref.getAsString())));
        if(index.has("entries"))index.add("entries",entries);
        var children=new JsonArray();
        if(index.has("chapters"))for(var ref:index.getAsJsonArray("chapters"))children.add(node(relative(path,ref.getAsString()),depth+1));
        if(index.has("chapters"))index.add("chapters",children);
        return index;
    }
}

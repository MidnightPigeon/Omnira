package com.mcmagic.omnira.sign;

import com.google.gson.*;
import com.mcmagic.omnira.Omnira;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Server-owned plain text pool; each sign persists its own shuffled draw bag. */
@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class WisdomStatements {
    public static final String FILE="omnira-wisdom.json";
    private static List<String> pool=List.of();
    private static java.nio.file.attribute.FileTime stamp;
    public static Path path(){return FMLPaths.CONFIGDIR.get().resolve(FILE);}
    @SubscribeEvent public static void start(ServerAboutToStartEvent event){stamp=null;reload();}
    public static List<String> pool(){reload();return pool;}
    public static String signature(List<String> statements){
        try {
            var bytes=new Gson().toJson(statements).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    public static List<String> parse(JsonElement root){
        if(!root.isJsonObject()||!root.getAsJsonObject().has("statements")
                ||!root.getAsJsonObject().get("statements").isJsonArray())throw new IllegalArgumentException("Expected statements array");
        var valid=new LinkedHashSet<String>();
        for(var entry:root.getAsJsonObject().getAsJsonArray("statements")){
            if(!entry.isJsonPrimitive()||!entry.getAsJsonPrimitive().isString())continue;
            String phrase=normalize(entry.getAsString());
            if(phrase!=null)valid.add(phrase);
        }
        return List.copyOf(valid);
    }
    public static String normalize(String raw){
        String[] lines=raw.replace("\r\n","\n").replace('\r','\n').split("\n",-1);
        if(lines.length>4||raw.isBlank())return null;
        for(int i=0;i<lines.length;i++){
            var line=new StringBuilder();int width=0;
            // Twelve full-width characters; Latin text uses half-width cells.
            for(int point:lines[i].codePoints().toArray()){
                if(Character.isISOControl(point))continue;
                int cells=point<=127?1:2;if(width+cells>24)break;
                line.appendCodePoint(point);width+=cells;
            }
            lines[i]=line.toString();
        }
        return String.join("\n",lines);
    }
    public static String[] positioned(String statement){
        String[] result={"","","",""};if(statement==null||statement.isEmpty())return result;
        var lines=statement.split("\n",-1);int offset=lines.length<=2?1:0;
        for(int i=0;i<Math.min(4,lines.length);i++)result[offset+i]=lines[i];
        return result;
    }
    private static synchronized void reload(){
        try {
            var file=path();
            if(!Files.exists(file)){
                Files.createDirectories(file.getParent());
                try(var input=Objects.requireNonNull(WisdomStatements.class.getResourceAsStream("/omnira-wisdom-defaults.json"))){
                    Files.copy(input,file);
                }
            }
            var modified=Files.getLastModifiedTime(file);if(modified.equals(stamp))return;
            try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
                pool=parse(JsonParser.parseReader(reader));stamp=modified;
            }
        }catch(Exception ex){
            Omnira.LOGGER.warn("Cannot load {}: {}; retaining the last valid statement pool",FILE,ex.getMessage());
            try {stamp=Files.getLastModifiedTime(path());}catch(java.io.IOException ignored){}
        }
    }
    private WisdomStatements(){}
}

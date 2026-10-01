package com.mcmagic.omnira.config;

import com.google.gson.*;
import com.mcmagic.omnira.Omnira;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One server-owned editing surface; vanilla loot tables remain the bundled fallback. */
@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class OmniraLootConfig {
    public enum Mode { WHITELIST, BLACKLIST }
    public record FluidRules(Mode mode,Set<String> whitelist,Set<String> blacklist){
        public boolean allows(Fluid fluid){
            if(fluid==Fluids.EMPTY||!fluid.defaultFluidState().isSource())return false;
            String id=BuiltInRegistries.FLUID.getKey(fluid).toString();
            return mode==Mode.WHITELIST?whitelist.contains(id):!blacklist.contains(id);
        }
    }
    private record Snapshot(FluidRules fluids,Map<ResourceLocation,JsonElement> tables){}
    private static volatile Snapshot current;
    private static String loadedStamp="";
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static Path path(){return FMLPaths.CONFIGDIR.get().resolve("omnira-loot.json");}
    public static String canonicalTableId(String id){
        return switch(id){
            case "omnira:chests/reversion_mill_cellar" -> "omnira:chests/rusty_lake_mill_cellar";
            case "omnira:chests/reversion_mill_workshop" -> "omnira:chests/rusty_lake_mill_workshop";
            default -> id;
        };
    }
    public static boolean migrateTableIds(JsonObject tables){
        boolean changed=false;
        for(String old:List.copyOf(tables.keySet())){
            String id=canonicalTableId(old);if(id.equals(old))continue;
            if(!tables.has(id))tables.add(id,tables.get(old));
            tables.remove(old);changed=true;
        }
        return changed;
    }
    private static boolean migrateDefaultTimeflowTreasure(JsonObject tables){
        String id="omnira:fishing/mire_treasure";
        if(!tables.has(id))return false;
        var pools=tables.getAsJsonObject(id).getAsJsonArray("pools");
        if(pools.size()!=1)return false;
        var entries=pools.get(0).getAsJsonObject().getAsJsonArray("entries");
        if(entries.size()!=3)return false;
        String[] names={"omnira:time_warp_point","minecraft:book","omnira:dream_spell_core"};
        int[] weights={90,8,2};
        for(int i=0;i<3;i++){
            var entry=entries.get(i).getAsJsonObject();
            if(entry.size()!=(i==1?4:3)||!entry.get("type").getAsString().equals("minecraft:item")
                    ||!entry.get("name").getAsString().equals(names[i])||entry.get("weight").getAsInt()!=weights[i])return false;
        }
        var functions=entries.get(1).getAsJsonObject().getAsJsonArray("functions");
        if(functions.size()!=1)return false;
        var enchant=functions.get(0).getAsJsonObject();
        if(enchant.size()!=2||!enchant.get("function").getAsString().equals("minecraft:enchant_randomly")
                ||!enchant.get("options").getAsString().equals("#minecraft:treasure"))return false;
        tables.add(id,defaults().getAsJsonObject("treasureTables").get(id).deepCopy());
        return true;
    }
    public static List<String> sourceFluidIds(){
        return BuiltInRegistries.FLUID.stream().filter(f->f!=Fluids.EMPTY&&f.defaultFluidState().isSource())
                .map(f->BuiltInRegistries.FLUID.getKey(f).toString()).sorted().toList();
    }
    public static boolean migrateDefaultToolTreasures(JsonObject tables){
        return migrateDefaults(tables,"temporal_tools_v1");
    }
    public static boolean migrateDefaultMemoryTreasures(JsonObject tables){
        return migrateDefaults(tables,"memory_rewards_v1");
    }
    public static boolean migrateDefaultBasicResources(JsonObject tables){
        return migrateDefaults(tables,"basic_resources_v1");
    }
    public static boolean migrateDefaultTimeOnly(JsonObject tables){
        return migrateDefaults(tables,"time_only_v1");
    }
    private static boolean migrateDefaults(JsonObject tables,String revision){
        try(var stream=Objects.requireNonNull(OmniraLootConfig.class.getResourceAsStream("/loot_migrations/"+revision+".json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var previous=JsonParser.parseReader(reader).getAsJsonObject();
            var bundled=defaults().getAsJsonObject("treasureTables");boolean changed=false;
            for(var entry:previous.entrySet()){
                if(entry.getValue().equals(tables.get(entry.getKey()))){
                    tables.add(entry.getKey(),bundled.get(entry.getKey()).deepCopy());changed=true;
                }
            }
            return changed;
        }catch(IOException error){throw new IllegalStateException("Cannot read loot migration "+revision,error);}
    }
    /** Memory categories are weighted one-item draws, keeping the six-item contract. */
    public static void validateMemoryTable(JsonObject table){
        if(!table.keySet().equals(Set.of("type","pools"))||!table.get("type").getAsString().equals("minecraft:chest"))
            throw new IllegalArgumentException("Memory tables require type and pools only");
        var pools=table.getAsJsonArray("pools");
        if(pools.size()!=1)throw new IllegalArgumentException("Memory tables require one pool");
        var pool=pools.get(0).getAsJsonObject();
        if(!pool.keySet().equals(Set.of("rolls","entries"))||pool.get("rolls").getAsBigDecimal().compareTo(java.math.BigDecimal.ONE)!=0)
            throw new IllegalArgumentException("Memory pools require exactly one draw");
        var entries=pool.getAsJsonArray("entries");
        if(entries.isEmpty())throw new IllegalArgumentException("Memory material pool cannot be empty");
        for(var raw:entries){
            var e=raw.getAsJsonObject();
            if(!Set.of("type","name","weight").containsAll(e.keySet())||!e.get("type").getAsString().equals("minecraft:item"))
                throw new IllegalArgumentException("Memory entries support only item and weight");
            var id=ResourceLocation.parse(e.get("name").getAsString());
            if(!BuiltInRegistries.ITEM.containsKey(id)||BuiltInRegistries.ITEM.get(id)==net.minecraft.world.item.Items.AIR)
                throw new IllegalArgumentException("Invalid memory material: "+id);
            if(e.has("weight")&&e.get("weight").getAsBigDecimal().intValueExact()<=0)
                throw new IllegalArgumentException("Memory weight must be a positive integer");
        }
    }
    public static List<Fluid> treasureFluids(){
        var snapshot=current;
        if(snapshot==null)return List.of();
        return BuiltInRegistries.FLUID.stream().filter(snapshot.fluids()::allows).toList();
    }
    public static JsonObject defaults(){
        try(var stream=Objects.requireNonNull(OmniraLootConfig.class.getResourceAsStream("/omnira-loot-defaults.json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var root=new JsonObject();root.addProperty("formatVersion",1);
            root.addProperty("instructions","Edit this file, then restart or /reload. Treasure tables use vanilla loot-table JSON. Already opened treasure is unchanged. Empty eligible fluid lists leave treasure unopened.");
            var fluid=new JsonObject();fluid.addProperty("mode","WHITELIST");
            fluid.add("whitelist",GSON.toJsonTree(sourceFluidIds()));fluid.add("blacklist",new JsonArray());
            root.add("fluidTreasure",fluid);root.add("treasureTables",JsonParser.parseReader(reader));return root;
        }catch(IOException ex){throw new IllegalStateException("Cannot read Omnira loot defaults",ex);}
    }
    private static Set<String> ids(JsonArray array){
        var result=new HashSet<String>();
        for(var value:array){
            String id=value.getAsString();
            if(!id.contains(":")||ResourceLocation.tryParse(id)==null)throw new IllegalArgumentException("Invalid fluid ID: "+id);
            result.add(id);
        }
        return Set.copyOf(result);
    }
    public static FluidRules rules(JsonObject root){
        if(root.get("formatVersion").getAsInt()!=1)throw new IllegalArgumentException("Unsupported loot config version");
        var fluid=root.getAsJsonObject("fluidTreasure");
        return new FluidRules(Mode.valueOf(fluid.get("mode").getAsString().toUpperCase(Locale.ROOT)),
                ids(fluid.getAsJsonArray("whitelist")),ids(fluid.getAsJsonArray("blacklist")));
    }
    private static Snapshot decode(JsonObject root,HolderLookup.Provider registries){
        var tables=new HashMap<ResourceLocation,JsonElement>();
        var merged=defaults().getAsJsonObject("treasureTables");
        for(var entry:root.getAsJsonObject("treasureTables").entrySet()){
            if(!merged.has(entry.getKey()))throw new IllegalArgumentException("Unknown treasure table: "+entry.getKey());
            merged.add(entry.getKey(),entry.getValue());
        }
        var ops=RegistryOps.create(JsonOps.INSTANCE,registries);
        for(var entry:merged.entrySet()){
            if(entry.getKey().startsWith("omnira:memory/"))validateMemoryTable(entry.getValue().getAsJsonObject());
            LootTable.DIRECT_CODEC.parse(ops,entry.getValue()).getOrThrow();
            tables.put(ResourceLocation.parse(entry.getKey()),entry.getValue().deepCopy());
        }
        return new Snapshot(rules(root),Map.copyOf(tables));
    }
    private static synchronized void refresh(HolderLookup.Provider registries){
        try{
            var path=path();
            if(!Files.exists(path)){
                Files.createDirectories(path.getParent());
                Files.writeString(path,GSON.toJson(defaults())+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            }
            String stamp=Files.getLastModifiedTime(path)+":"+Files.size(path);
            if(stamp.equals(loadedStamp))return;
            loadedStamp=stamp;
            JsonObject root;
            try(var reader=Files.newBufferedReader(path)){root=JsonParser.parseReader(reader).getAsJsonObject();}
            var tables=root.getAsJsonObject("treasureTables");boolean added=migrateTableIds(tables);
            added=migrateDefaultTimeflowTreasure(tables)||added;
            added=migrateDefaultToolTreasures(tables)||added;
            added=migrateDefaultMemoryTreasures(tables)||added;
            added=migrateDefaultBasicResources(tables)||added;
            added=migrateDefaults(tables,"time_materials_v1")||added;
            added=migrateDefaultTimeOnly(tables)||added;
            current=decode(root,registries);
            for(var entry:defaults().getAsJsonObject("treasureTables").entrySet())if(!tables.has(entry.getKey())){tables.add(entry.getKey(),entry.getValue());added=true;}
            if(added){Files.writeString(path,GSON.toJson(root)+"\n",StandardCharsets.UTF_8);loadedStamp=Files.getLastModifiedTime(path)+":"+Files.size(path);}
            Omnira.LOGGER.info("Loaded Omnira loot config: {} treasure tables, {} eligible fluids",current.tables().size(),treasureFluids().size());
        }catch(Exception ex){
            Omnira.LOGGER.error("Cannot load omnira-loot.json; keeping last valid settings (bundled defaults on first load). File not overwritten.",ex);
            if(current==null)current=decode(defaults(),registries);
        }
    }
    @SubscribeEvent public static void lootTable(LootTableLoadEvent event){
        if(!event.getName().getNamespace().equals("omnira")||!(event.getName().getPath().startsWith("chests/")||event.getName().getPath().startsWith("fishing/")||event.getName().getPath().startsWith("archaeology/")||event.getName().getPath().startsWith("memory/")))return;
        refresh(event.getRegistries());
        var replacement=current.tables().get(event.getName());
        // Decode per reload: NeoForge assigns IDs/freezes table instances after this event.
        if(replacement!=null)event.setTable(LootTable.DIRECT_CODEC.parse(
                RegistryOps.create(JsonOps.INSTANCE,event.getRegistries()),replacement).getOrThrow());
    }
}

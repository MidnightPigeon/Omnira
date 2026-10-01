package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mcmagic.omnira.config.OmniraLootConfig;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.core.registries.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.Files;
import java.util.Set;

@GameTestHolder("omnira_memory_loot")
@PrefixGameTestTemplate(false)
public final class MemoryLootConfigGameTests {
    @GameTest(template="spell_arena") public static void basicResourceRewards(GameTestHelper h)throws Exception{
        var defaults=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        for(var entry:defaults.entrySet()){
            String json=entry.getValue().toString();
            for(String id:new String[]{"temporal_silt","living_temporal_silt","sedimented_temporal_silt",
                    "sedimented_living_temporal_silt","rotted_temporal_silt","rough_spatial_crystal_block",
                    "excited_rough_spatial_crystal_block","time_sand","shadow_rock"})
                h.assertTrue(!json.contains("\"omnira:"+id+"\""),"Terrain reward in "+entry.getKey()+": "+id);
        }
        try(var in=MemoryLootConfigGameTests.class.getResourceAsStream("/loot_migrations/basic_resources_v1.json")){
            var old=JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var custom=old.deepCopy();custom.getAsJsonObject("omnira:chests/golden_throne").addProperty("random_sequence","omnira:custom");
            var saved=custom.get("omnira:chests/golden_throne").deepCopy();
            h.assertTrue(OmniraLootConfig.migrateDefaultBasicResources(old),"Resource migration missing");
            h.assertTrue(!OmniraLootConfig.migrateDefaultBasicResources(old),"Resource migration repeated");
            for(var entry:old.entrySet())h.assertTrue(entry.getValue().equals(defaults.get(entry.getKey())),"Migration differs from bundled rewards");
            OmniraLootConfig.migrateDefaultBasicResources(custom);
            h.assertTrue(saved.equals(custom.get("omnira:chests/golden_throne")),"Custom rewards overwritten");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void configurationAndMigration(GameTestHelper h)throws Exception{
        var defaults=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        int index=0;
        for(String category:new String[]{"time","light","space","shadow"}){
            var table=defaults.getAsJsonObject("omnira:memory/"+category);OmniraLootConfig.validateMemoryTable(table);
            int weight=0;for(var e:table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries"))weight+=e.getAsJsonObject().get("weight").getAsInt();
            h.assertTrue(weight==new int[]{25,7,6,7}[index++],"Legacy weight changed: "+category);
        }
        var bad=defaults.getAsJsonObject("omnira:memory/time").deepCopy();bad.getAsJsonArray("pools").get(0).getAsJsonObject().addProperty("rolls",2);
        boolean rejected=false;try{OmniraLootConfig.validateMemoryTable(bad);}catch(IllegalArgumentException ex){rejected=true;}
        h.assertTrue(rejected,"Memory count contract can be bypassed");
        try(var in=MemoryLootConfigGameTests.class.getResourceAsStream("/loot_migrations/memory_rewards_v1.json")){
            var old=JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var custom=old.deepCopy();custom.getAsJsonObject("omnira:chests/golden_throne_tower").addProperty("random_sequence","omnira:custom");
            var saved=custom.get("omnira:chests/golden_throne_tower").deepCopy();
            h.assertTrue(OmniraLootConfig.migrateDefaultMemoryTreasures(old),"Default migration missing");
            h.assertTrue(!OmniraLootConfig.migrateDefaultMemoryTreasures(old),"Migration repeated");
            OmniraLootConfig.migrateDefaultMemoryTreasures(custom);
            h.assertTrue(saved.equals(custom.get("omnira:chests/golden_throne_tower")),"Custom rewards overwritten");
        }
        var path=OmniraLootConfig.path();String original=Files.readString(path);var key=ResourceLocation.parse("omnira:memory/time");
        try{
            var root=JsonParser.parseString(original).getAsJsonObject();
            root.getAsJsonObject("treasureTables").add(key.toString(),JsonParser.parseString("{\"type\":\"minecraft:chest\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\",\"weight\":1}]}]}"));
            Files.writeString(path,root.toString());var event=new LootTableLoadEvent(h.getLevel().registryAccess(),key,LootTable.EMPTY);OmniraLootConfig.lootTable(event);
            var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO)).create(LootContextParamSets.CHEST);
            var drops=event.getTable().getRandomItems(params,12345);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(net.minecraft.world.item.Items.DIAMOND),"Configured memory table not used");
        }finally{
            Files.writeString(path,original);OmniraLootConfig.lootTable(new LootTableLoadEvent(h.getLevel().registryAccess(),key,LootTable.EMPTY));
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void millAndTowerRewards(GameTestHelper h){
        var level=h.getLevel();var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO)).create(LootContextParamSets.CHEST);
        int facilities=0,cubes=0,shards=0;
        for(String name:new String[]{"rusty_lake_mill_cellar","rusty_lake_mill_workshop","rusty_lake_mill_memory","golden_throne_tower"}){
            // Test bundled rewards independently of retained local custom loot tables.
            var table=LootTable.DIRECT_CODEC.parse(net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),
                    OmniraLootConfig.defaults().getAsJsonObject("treasureTables").get("omnira:chests/"+name)).getOrThrow();
            for(int i=1;i<=256;i++){
                int memories=0;
                for(var stack:table.getRandomItems(params,net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(i))){
                    String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    h.assertTrue(!Set.of("minecraft:black_concrete","minecraft:white_concrete","omnira:dream_crystal","omnira:memory_cube").contains(id),"Obsolete or placeable reward: "+id);
                    if(id.equals("omnira:dream_crystal_shard"))shards+=stack.getCount();
                    if(id.equals("omnira:peaceful_memory_cube")||id.equals("omnira:corrupted_memory_cube"))memories+=stack.getCount();
                    if(name.equals("golden_throne_tower")&&id.equals("omnira:damaged_research_facility"))facilities++;
                }
                h.assertTrue(memories<=1,"More than one memory cube in a mill ball");cubes+=memories;
            }
        }
        h.assertTrue(facilities>0&&cubes>0&&shards>0,"Expected revised rewards missing");h.succeed();
    }
}

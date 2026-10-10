package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.config.OmniraLootConfig;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@GameTestHolder("omnira_paradox_loot")
@PrefixGameTestTemplate(false)
public final class ParadoxLootGameTests {
    private static final String DUST="omnira:paradox_dust";
    private static JsonObject previous()throws IOException{
        try(var stream=Objects.requireNonNull(ParadoxLootGameTests.class.getResourceAsStream("/loot_migrations/paradox_scarcity_v1.json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    private static int weight(JsonObject pool,String item){
        int total=0;
        for(var raw:pool.getAsJsonArray("entries")){
            var e=raw.getAsJsonObject();
            if(item==null||item.equals(e.get("name").getAsString()))total+=e.has("weight")?e.get("weight").getAsInt():1;
        }
        return total;
    }
    @GameTest(template="spell_arena") public static void distributionAndSpecialRewards(GameTestHelper h)throws Exception{
        var defaults=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        for(var before:previous().entrySet()){
            var key=before.getKey();var oldPools=before.getValue().getAsJsonObject().getAsJsonArray("pools");
            var pools=defaults.getAsJsonObject(key).getAsJsonArray("pools");
            h.assertTrue(pools.size()==oldPools.size(),"Pool count changed: "+key);
            boolean spatial=Set.of("omnira:chests/fold_observatory_lower","omnira:chests/fold_observatory_upper",
                    "omnira:chests/calibration_materials","omnira:chests/void_lighthouse").contains(key);
            if(spatial)h.assertTrue(!pools.toString().contains(DUST),"Space filler retains dust: "+key);
            for(int i=0;i<pools.size();i++){
                var oldPool=oldPools.get(i).getAsJsonObject();var pool=pools.get(i).getAsJsonObject();
                if(!oldPool.toString().contains(DUST))h.assertTrue(oldPool.equals(pool),"Special reward changed: "+key);
                if(key.contains("archaeology/")){
                    h.assertTrue(weight(pool,null)==weight(oldPool,null),"Archaeology theme probabilities changed");
                    h.assertTrue(weight(pool,DUST)*5==weight(pool,null),"Dust archaeology chance is not 20%");
                }else if(key.equals("omnira:fishing/starflow_junk")){
                    h.assertTrue(weight(pool,null)==100&&weight(pool,DUST)==15,"Starflow weights incorrect");
                    h.assertTrue(weight(pool,"omnira:life_ender")==2,"Rare fish reward changed");
                }else if(!spatial&&oldPool.toString().contains(DUST)){
                    if(oldPool.getAsJsonArray("entries").size()>1)h.assertTrue(weight(pool,null)==weight(oldPool,null),"Unfilled time-loot weight: "+key);
                    for(var raw:pool.getAsJsonArray("entries")){
                        var e=raw.getAsJsonObject();if(!DUST.equals(e.get("name").getAsString()))continue;
                        h.assertTrue(e.getAsJsonArray("functions").get(0).getAsJsonObject().get("count").getAsInt()==1,"Dust stack still inflated");
                    }
                    if(oldPool.getAsJsonArray("entries").size()==1)h.assertTrue(weight(pool,DUST)*4==weight(pool,null)
                            &&!pool.has("conditions"),"Guaranteed dust pool not replaced by 25% material mix");
                }
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void migrationPreservesCustomAndIndependentPools(GameTestHelper h)throws Exception{
        var old=previous();var custom=old.deepCopy();String key="omnira:chests/doomsday_rite";
        custom.getAsJsonObject(key).addProperty("random_sequence","omnira:custom");var saved=custom.get(key).deepCopy();
        var memory=OmniraLootConfig.defaults().getAsJsonObject("treasureTables").get("omnira:memory/time").deepCopy();
        custom.add("omnira:memory/time",memory.deepCopy());
        h.assertTrue(OmniraLootConfig.migrateDefaultParadoxScarcity(old),"Prior default migration missing");
        h.assertTrue(!OmniraLootConfig.migrateDefaultParadoxScarcity(old),"Migration repeated");
        OmniraLootConfig.migrateDefaultParadoxScarcity(custom);
        h.assertTrue(custom.get(key).equals(saved)&&custom.get("omnira:memory/time").equals(memory),"Custom or independent memory pool overwritten");
        var missing=new JsonObject();h.assertTrue(!OmniraLootConfig.migrateDefaultParadoxScarcity(missing)&&missing.size()==0,"Missing table migration fabricated values");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void actualDrawsAndNativeDefaultsAgree(GameTestHelper h)throws Exception{
        var defaults=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        var ops=RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess());
        var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO)).create(LootContextParamSets.CHEST);
        int oldCount=0,newCount=0;
        for(var entry:previous().entrySet()){
            String key=entry.getKey();
            try(var stream=Objects.requireNonNull(ParadoxLootGameTests.class.getResourceAsStream("/data/omnira/loot_table/"+key.substring("omnira:".length())+".json"));
                var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                h.assertTrue(JsonParser.parseReader(reader).equals(defaults.get(key)),"Native/default mismatch: "+key);
            }
            var old=LootTable.DIRECT_CODEC.parse(ops,entry.getValue()).getOrThrow();
            var current=LootTable.DIRECT_CODEC.parse(ops,defaults.get(key)).getOrThrow();
            if(!key.startsWith("omnira:chests/"))continue;
            for(int seed=1;seed<=256;seed++){
                for(var stack:old.getRandomItems(params,seed))if(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(DUST))oldCount+=stack.getCount();
                for(var stack:current.getRandomItems(params,seed))if(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(DUST))newCount+=stack.getCount();
            }
        }
        h.assertTrue(newCount>0&&newCount<oldCount*.35,"Dust volume not meaningfully reduced: "+oldCount+" -> "+newCount);
        h.succeed();
    }
    private ParadoxLootGameTests(){}
}

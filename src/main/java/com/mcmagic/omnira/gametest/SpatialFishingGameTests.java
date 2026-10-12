package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.config.OmniraLootConfig;
import com.mojang.serialization.JsonOps;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spatial_fishing")
@PrefixGameTestTemplate(false)
public final class SpatialFishingGameTests {
    private static final String FISH="omnira:fishing/starflow_fish";
    private static JsonObject read(String path)throws IOException {
        try(var stream=Objects.requireNonNull(SpatialFishingGameTests.class.getResourceAsStream(path));
                var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    @GameTest(template="spell_arena")
    public static void migrationOnlyReplacesUneditedDefaults(GameTestHelper h)throws IOException {
        var old=read("/loot_migrations/spatial_fishing_v1.json");
        var custom=old.deepCopy();custom.getAsJsonObject(FISH).addProperty("random_sequence","pack:custom");
        var saved=custom.deepCopy();
        h.assertTrue(OmniraLootConfig.migrateDefaultSpatialFishing(old),"Old fishing default not migrated");
        h.assertTrue(old.get(FISH).equals(OmniraLootConfig.defaults().getAsJsonObject("treasureTables").get(FISH)),"Incorrect migration");
        h.assertTrue(!OmniraLootConfig.migrateDefaultSpatialFishing(old),"Migration repeated");
        h.assertTrue(!OmniraLootConfig.migrateDefaultSpatialFishing(custom)&&custom.equals(saved),"Custom fishing overwritten");
        h.assertTrue(!OmniraLootConfig.migrateDefaultSpatialFishing(new JsonObject()),"Missing fishing table fabricated");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void nativeTablesMatchConfigAndSpaceMaterialsAreCatchable(GameTestHelper h)throws IOException {
        var tables=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        var ops=RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess());
        var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,h.absoluteVec(Vec3.ZERO))
                .withParameter(LootContextParams.TOOL,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        for(var entry:tables.entrySet()) {
            h.assertTrue(read("/data/omnira/loot_table/"+entry.getKey().substring("omnira:".length())+".json").equals(entry.getValue()),"Native/config mismatch: "+entry.getKey());
            LootTable.DIRECT_CODEC.parse(ops,entry.getValue()).getOrThrow();
        }
        var fish=LootTable.DIRECT_CODEC.parse(ops,tables.get(FISH)).getOrThrow();
        var caught=new HashSet<String>();
        for(int seed=1;seed<=512;seed++)for(var stack:fish.getRandomItems(params,seed))caught.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        h.assertTrue(caught.equals(Set.of("omnira:star_jellyfish","omnira:infused_spiritual_crystal","omnira:dream_crystal_shard",
                "omnira:spatial_crystal_shard","omnira:resonance_crystal")),"Missing or unexpected spatial catch: "+caught);
        var categories=tables.getAsJsonObject("omnira:fishing/void_starflow").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        int[] weights={85,10,5},quality={-1,-2,2};
        for(int i=0;i<3;i++)h.assertTrue(categories.get(i).getAsJsonObject().get("weight").getAsInt()==weights[i]
                &&categories.get(i).getAsJsonObject().get("quality").getAsInt()==quality[i],"Category or luck balance changed");
        h.succeed();
    }
    private SpatialFishingGameTests(){}
}

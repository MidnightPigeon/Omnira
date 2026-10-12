package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.config.OmniraLootConfig;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.space.geodes.DeflectionContent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spatial_loot")
@PrefixGameTestTemplate(false)
public final class SpatialLootGameTests {
    @GameTest(template="spell_arena")
    public static void lootUsesActualBiome(GameTestHelper h) {
        var level=h.getLevel();var chunk=level.getChunk(0,0);
        var old=new HashMap<BlockPos,Holder<Biome>>();
        for(int x=0;x<4;x++)for(int z=0;z<4;z++)for(int y=level.getMinBuildHeight()/4;y<level.getMaxBuildHeight()/4;y++)
            old.put(new BlockPos(x,y,z),chunk.getNoiseBiome(x,y,z));
        var sampler=level.getChunkSource().randomState().sampler();
        var biomeRegistry=level.registryAccess().registryOrThrow(Registries.BIOME);
        try {
            for(String biome:List.of("omnira:dislocated_geodes","omnira:topological_labyrinth","omnira:phase_crystal_grove","minecraft:plains")) {
                var holder=biomeRegistry.getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse(biome)));
                chunk.fillBiomesFromNoise((x,y,z,s)->holder,sampler);
                var origin=new Vec3(6,248,6);
                h.assertTrue(level.getBiome(BlockPos.containing(origin)).equals(holder),"Wrong loot test biome");
                var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,origin).create(LootContextParamSets.CHEST);
                for(String name:List.of("calibration_materials","calibration_supplies","topological_labyrinth")) {
                    var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("omnira:chests/"+name));
                    var table=level.getServer().reloadableRegistries().getLootTable(key);
                    int crystals=0,excited=0;
                    for(int seed=1;seed<=128;seed++)for(var stack:table.getRandomItems(params,seed)) {
                        if(stack.is(DeflectionContent.CRYSTAL.get()))crystals+=stack.getCount();
                        if(stack.is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get().asItem()))excited+=stack.getCount();
                    }
                    boolean geodes=biome.equals("omnira:dislocated_geodes");
                    h.assertTrue(geodes?crystals>0&&excited==0:crystals==0&&excited>0,"Specialty leaked or filler missing: "+biome+"/"+name);
                }
            }
        } finally {
            chunk.fillBiomesFromNoise((x,y,z,s)->old.get(new BlockPos(Math.floorMod(x,4),y,Math.floorMod(z,4))),sampler);
        }
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void defaultMigrationPreservesCustomLoot(GameTestHelper h)throws IOException {
        JsonObject before;
        try(var stream=Objects.requireNonNull(SpatialLootGameTests.class.getResourceAsStream("/loot_migrations/deflection_biome_v1.json"));
                var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            before=JsonParser.parseReader(reader).getAsJsonObject();
        }
        var custom=before.deepCopy();
        custom.getAsJsonObject("omnira:chests/calibration_supplies").addProperty("random_sequence","pack:custom");
        var saved=custom.get("omnira:chests/calibration_supplies").deepCopy();
        h.assertTrue(OmniraLootConfig.migrateDefaultDeflectionBiome(before),"Old defaults not migrated");
        h.assertTrue(!OmniraLootConfig.migrateDefaultDeflectionBiome(before),"Migration repeated");
        var defaults=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        for(var entry:before.entrySet())h.assertTrue(entry.getValue().equals(defaults.get(entry.getKey())),"Wrong migrated default");
        OmniraLootConfig.migrateDefaultDeflectionBiome(custom);
        h.assertTrue(custom.get("omnira:chests/calibration_supplies").equals(saved),"Custom loot overwritten");
        h.assertTrue(!OmniraLootConfig.migrateDefaultDeflectionBiome(new JsonObject()),"Missing table fabricated");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void nineExcitedBlocksMakeOneFoldPoint(GameTestHelper h) {
        var r=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:space_warp_point")).orElseThrow().value();
        var items=new ArrayList<ItemStack>();for(int i=0;i<9;i++)items.add(new ItemStack(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()));
        var input=CraftingInput.of(3,3,items);
        h.assertTrue(r.matches(input,h.getLevel()),"Nine excited blocks rejected");
        var output=r.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(output.getItem()).toString().equals("omnira:space_warp_point")&&output.getCount()==1,"Wrong fold point output");
        items.set(4,ItemStack.EMPTY);
        h.assertTrue(!r.matches(CraftingInput.of(3,3,items),h.getLevel()),"Incomplete nine-block recipe accepted");
        h.succeed();
    }
    private SpatialLootGameTests(){}
}

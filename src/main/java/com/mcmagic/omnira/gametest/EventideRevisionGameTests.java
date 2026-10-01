package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.time.*;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_eventide_revision")
@PrefixGameTestTemplate(false)
public final class EventideRevisionGameTests {
    @GameTest(template="spell_arena") public static void singleToolTreasures(GameTestHelper h)throws Exception{
        var level=h.getLevel();var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,h.absolutePos(BlockPos.ZERO).getCenter())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int sanctified=0,corrupted=0;
        for(String name:new String[]{"kirisame_attic","drilling_platform"}){
            var table=level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.parse("omnira:chests/"+name)));
            for(int i=1;i<=512;i++){
                int tools=0,books=0;
                for(var stack:table.getRandomItems(params,net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(i))){
                    String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                    if(id.equals(name.equals("kirisame_attic")?"crystal_broom":"spacetime_archaeology_brush"))tools+=stack.getCount();
                    h.assertTrue(!id.equals("infused_grimoire"),"Base grimoire remains in treasure");
                    if(id.equals("sanctified_grimoire")){books+=stack.getCount();sanctified+=stack.getCount();}
                    if(id.equals("corrupted_grimoire")){books+=stack.getCount();corrupted+=stack.getCount();}
                }
                h.assertTrue(tools==1&&books<=1,"Tools must be guaranteed exactly once and grimoires mutually exclusive");
            }
        }
        h.assertTrue(sanctified>0&&corrupted>0,"Missing one upgraded grimoire");
        try(var in=java.util.Objects.requireNonNull(EventideRevisionGameTests.class.getResourceAsStream("/loot_migrations/temporal_tools_v1.json"))){
            var old=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var custom=old.deepCopy();custom.getAsJsonObject("omnira:chests/kirisame_attic").addProperty("random_sequence","omnira:custom");
            var saved=custom.get("omnira:chests/kirisame_attic").deepCopy();
            h.assertTrue(com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultToolTreasures(old),"Old defaults not migrated");
            h.assertTrue(!com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultToolTreasures(old),"Migration not idempotent");
            com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultToolTreasures(custom);
            h.assertTrue(custom.get("omnira:chests/kirisame_attic").equals(saved),"Custom rewards overwritten");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void brickNode(GameTestHelper h){
        var level=h.getLevel();var fixture=h.absolutePos(new BlockPos(4,1,4));
        var p=new BlockPos((fixture.getX()&~15)+4,180,(fixture.getZ()&~15)+4);
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EventideRuins.BIOME);
        level.getChunkAt(p).fillBiomesFromNoise((x,y,z,sampler)->biome,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(p,Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(p.east(),Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(p.east(2),Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        EventideRuins.pulseChunk(level,p.getX()>>4,p.getZ()>>4,1);
        h.assertTrue(level.getBlockState(p).is(EventideMasonry.BRICKS.get()),"Stone bricks did not age");
        h.assertTrue(level.getBlockState(p.east()).is(EventideMasonry.CRACKED.get()),"Cracks were not preserved");
        h.assertTrue(level.getBlockState(p.east(2)).is(Blocks.MOSSY_STONE_BRICKS),"Unrequested brick type converted");
        var plain=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        level.getChunkAt(p).fillBiomesFromNoise((x,y,z,sampler)->plain,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(p,Blocks.STONE_BRICKS.defaultBlockState());
        EventideRuins.pulseChunk(level,p.getX()>>4,p.getZ()>>4,2);
        h.assertTrue(level.getBlockState(p).is(Blocks.STONE_BRICKS),"Converted outside the biome");h.succeed();
    }
    @GameTest(template="spell_arena") public static void riteTreasures(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(0,8,0));
        new DoomsdayRitePiece(origin).postProcess(level,null,level.getChunkSource().getGenerator(),level.random,
                new BoundingBox(origin.getX(),origin.getY(),origin.getZ(),origin.getX()+32,origin.getY()+1,origin.getZ()+32),new ChunkPos(origin),BlockPos.ZERO);
        var whiteEye=origin.offset(16,1,12);var darkEye=origin.offset(16,1,20);
        h.assertTrue(level.getBlockEntity(whiteEye) instanceof LiquidCrystalBallBlockEntity,"White fish eye is not liquid treasure");
        var liquid=(LiquidCrystalBallBlockEntity)level.getBlockEntity(whiteEye);
        h.assertTrue(liquid.hasPendingLoot()&&liquid.tank.isEmpty(),"Liquid treasure is not sealed");
        var ball=(CrystalBallBlockEntity)level.getBlockEntity(darkEye);
        var tag=ball.saveWithFullMetadata(level.registryAccess());
        h.assertTrue(ball.hasPendingLoot()&&tag.getString("LootTable").equals("omnira:chests/doomsday_rite"),"Missing item loot binding");
        var key=net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.parse("omnira:chests/doomsday_rite"));
        h.assertTrue(level.getServer().reloadableRegistries().getLootTable(key)!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Rite loot missing from config");h.succeed();
    }
}

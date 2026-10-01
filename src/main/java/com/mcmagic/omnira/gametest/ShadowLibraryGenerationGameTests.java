package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.world.structure.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_shadow_generation")
@PrefixGameTestTemplate(false)
public final class ShadowLibraryGenerationGameTests {
    @GameTest(template="spell_arena",timeoutTicks=1200)
    public static void layoutsTerrainAndChunkPersistence(GameTestHelper h) throws Exception {
        var level=h.getLevel();int marks=0;long markedSeed=0;var counts=new java.util.HashSet<Integer>();
        for(int i=0;i<5000;i++) {
            var plan=new ShadowLibraryLayout(i);counts.add(plan.rooms.size());
            h.assertTrue(plan.rooms.size()>=4 && plan.rooms.size()<=7 && plan.rooms.getFirst().kind().equals("stacks"),"Room count/entrance changed");
            for(String kind:new String[]{"research","sealed"})h.assertTrue(plan.rooms.stream().filter(r->r.kind().equals(kind)).count()==1,"Special room duplicated");
            h.assertTrue(plan.rooms.stream().map(r->r.x()+","+r.z()).distinct().count()==plan.rooms.size(),"Rooms overlap");
            for(int j=1;j<plan.rooms.size();j++) {
                var r=plan.rooms.get(j);h.assertTrue(r.parent()>=0 && r.parent()<j,"Disconnected room");
                var p=plan.rooms.get(r.parent());h.assertTrue(Math.abs(r.x()-p.x())+Math.abs(r.z()-p.z())==1,"Non-adjacent corridor");
            }
            if(plan.waymark){marks++;markedSeed=i;}
        }
        h.assertTrue(counts.size()==4 && marks>850 && marks<1150,"Room count or 20 percent waymark distribution incorrect");
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.fromNamespaceAndPath("omnira","shadow_library"));
        h.assertTrue(structure instanceof ShadowLibraryStructure,"Structure not registered");
        h.assertTrue(structure.biomes().size()==1 && structure.biomes().stream().allMatch(b->b.is(ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("omnira","shadow_woodland")))),"Wrong biome restriction");
        NoiseBasedChunkGenerator gen;
        try(var reader=new java.io.InputStreamReader(ShadowLibraryGenerationGameTests.class.getResourceAsStream("/data/omnira/dimension/dream_realm.json"),java.nio.charset.StandardCharsets.UTF_8)) {
            gen=(NoiseBasedChunkGenerator)net.minecraft.world.level.chunk.ChunkGenerator.CODEC.parse(
                    RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),
                    com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().get("generator")).getOrThrow();
        }
        var noise=RandomState.create(gen.generatorSettings().value(),level.registryAccess().registryOrThrow(Registries.NOISE).asLookup(),level.getSeed());
        var height=LevelHeightAccessor.create(0,256);int found=0;
        for(int i=0;i<32 && found<2;i++) {
            var start=structure.generate(level.registryAccess(),gen,gen.getBiomeSource(),noise,level.getStructureManager(),level.getSeed(),
                    new ChunkPos(i*32-512,i*17-256),0,height,structure.biomes()::contains);
            if(!start.isValid())continue;found++;
            h.assertTrue(start.getBoundingBox().maxY()<94 && start.getBoundingBox().minY()>1,"Wrong underground layer");
            var context=StructurePieceSerializationContext.fromLevel(level);var piece=(ShadowLibraryPiece)start.getPieces().getFirst();
            var tag=piece.createTag(context);
            h.assertTrue(tag.equals(new ShadowLibraryPiece(context,tag).createTag(context)),"Layout changed after reload");
        }
        h.assertTrue(found>0,"No viable library sites in real dream terrain");
        var origin=h.absolutePos(new BlockPos(60,35,60));var piece=new ShadowLibraryPiece(origin,origin.getY()+18,markedSeed);
        var box=piece.getBoundingBox();var context=StructurePieceSerializationContext.fromLevel(level);
        // Reverse chunk order, reconstructing the piece from disk for every chunk.
        for(int cx=box.maxX()>>4;cx>=box.minX()>>4;cx--)for(int cz=box.maxZ()>>4;cz>=box.minZ()>>4;cz--) {
            var clip=new BoundingBox(cx*16,level.getMinBuildHeight(),cz*16,cx*16+15,level.getMaxBuildHeight()-1,cz*16+15);
            new ShadowLibraryPiece(context,piece.createTag(context)).postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),
                    RandomSource.create(cx*37L+cz),clip,new ChunkPos(cx,cz),origin);
        }
        var plan=piece.layout();var lootSeeds=new java.util.HashSet<Long>();
        for(var r:plan.rooms) {
            var base=origin.offset(r.origin());
            var ball=base.offset(r.kind().equals("stacks")?6:r.kind().equals("sealed")?4:2,1,
                    r.kind().equals("stacks")?5:r.kind().equals("sealed")?6:3);
            var be=level.getBlockEntity(ball);
            h.assertTrue(be!=null && be.saveWithoutMetadata(level.registryAccess()).getString("LootTable").equals("omnira:chests/shadow_library_"+r.kind()),"Loot ball lost");
            h.assertTrue(lootSeeds.add(((com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)be).getLootTableSeed()),"Shared loot seed");
            if(r.kind().equals("research")) {
                var second=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)level.getBlockEntity(base.offset(8,1,7));
                h.assertTrue(second!=null && lootSeeds.add(second.getLootTableSeed()),"Research balls do not roll independently");
                h.assertTrue(level.getBlockState(base.offset(3,1,5)).is(ModBlocks.ANALYSIS_ARTISAN_TABLE.get())==plan.analysisTable,"Analysis table rerolled");
                h.assertTrue(level.getBlockState(base.offset(7,1,5)).is(Blocks.ENCHANTING_TABLE)==plan.enchantingTable,"Enchanting table rerolled");
            }
            if(r.kind().equals("sealed")) {
                h.assertTrue(level.getBlockState(base.offset(2,1,4)).is(ModBlocks.WAYMARK.get())
                        && level.getBlockState(base.offset(2,2,4)).is(ModBlocks.WAYMARK.get()),"Waymark halves missing");
                h.assertTrue(level.getBlockState(base.offset(4,1,3)).is(Blocks.SPAWNER),"Ghost spawner lost");
                var mark=(com.mcmagic.omnira.block.entity.WaymarkBlockEntity)level.getBlockEntity(base.offset(2,1,4));
                var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"library-visitor"));
                var directory=com.mcmagic.omnira.travel.WaymarkDirectory.get(level);
                h.assertTrue(!directory.knows(player.getUUID(),mark.id()),"Natural mark discovered automatically");
                mark.discover(player);
                h.assertTrue(directory.knows(player.getUUID(),mark.id()) && !mark.name().isBlank(),"Natural mark cannot be discovered");
            }
            if(r.parent()<0)continue;var p=plan.rooms.get(r.parent());
            int dx=Integer.signum(r.x()-p.x()),dz=Integer.signum(r.z()-p.z());
            for(int t=p.half()-1;t<=16-r.half()+1;t++)for(int y=1;y<=2;y++)
                h.assertTrue(level.getBlockState(origin.offset(p.x()*16+dx*t,y,p.z()*16+dz*t)).isAir(),"Blocked corridor/door");
        }
        for(int y=1;y<18;y++)h.assertTrue(level.getBlockState(origin.offset(-3,y,-1)).is(Blocks.LADDER),"Broken entrance ladder");
        h.assertTrue(level.getBlockState(origin.offset(-3,18,-1)).is(ShadowWoodContent.TRAPDOOR.get()),"Surface hatch missing");
        h.succeed();
    }
}

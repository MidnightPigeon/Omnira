package com.mcmagic.omnira.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.*;

/** Generates an entire vanilla village, including its real pools, processors and loot NBT. */
@GameTestHolder("omnira_lootr_village")
@PrefixGameTestTemplate(false)
public final class LootrVillageGameTests {
    @GameTest(template="spell_arena",timeoutTicks=600) public static void generatedVillageChestsConvert(GameTestHelper h) {
        h.assertTrue(net.neoforged.fml.ModList.get().isLoaded("lootr"),"Requires Lootr");
        var level=h.getLevel();var generator=level.getChunkSource().getGenerator();
        var origin=new ChunkPos(h.absolutePos(new BlockPos(512,0,512)));
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.parse("minecraft:village_plains"));
        var start=structure.generate(level.registryAccess(),generator,generator.getBiomeSource(),
                level.getChunkSource().randomState(),level.getStructureManager(),level.getSeed(),origin,0,level,b->true);
        h.assertTrue(start.isValid(),"Vanilla village generation failed");
        var box=start.getBoundingBox();
        var min=new ChunkPos(box.minX()>>4,box.minZ()>>4);var max=new ChunkPos(box.maxX()>>4,box.maxZ()>>4);
        // Lootr waits for all surrounding chunks. Keep this independent test area loaded until completion.
        var loaded=ChunkPos.rangeClosed(new ChunkPos(min.x-2,min.z-2),new ChunkPos(max.x+2,max.z+2)).toList();
        for(var chunk:loaded){level.setChunkForced(chunk.x,chunk.z,true);level.getChunk(chunk.x,chunk.z);}
        var chunks=ChunkPos.rangeClosed(min,max).toList();
        for(var chunk:chunks)start.placeInChunk(level,level.structureManager(),generator,level.random,
                new BoundingBox(chunk.getMinBlockX(),level.getMinBuildHeight(),chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(),level.getMaxBuildHeight(),chunk.getMaxBlockZ()),chunk);
        var chests=new java.util.LinkedHashMap<BlockPos,String>();
        for(var chunk:chunks)for(var be:level.getChunk(chunk.x,chunk.z).getBlockEntities().values())
            if(be instanceof RandomizableContainerBlockEntity container&&container.getLootTable()!=null)
                chests.put(be.getBlockPos().immutable(),container.getLootTable().location().toString());
        h.assertTrue(!chests.isEmpty(),"Generated village has no loot containers");
        com.mojang.logging.LogUtils.getLogger().info("Lootr village probe: {} pieces, {} loot containers: {}",
                start.getPieces().size(),chests.size(),chests);
        h.succeedWhen(()->{
            for(var pos:chests.keySet())h.assertTrue(com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(level.getBlockEntity(pos)),
                    "Village chest failed natural conversion: "+pos+" table="+chests.get(pos));
            for(var chunk:loaded)level.setChunkForced(chunk.x,chunk.z,false);
        });
    }
}

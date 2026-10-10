package com.mcmagic.omnira.gametest;
import com.mcmagic.omnira.time.EventideRuins;
import com.mcmagic.omnira.archaeology.ArchaeologyContent;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.reversal.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_eventide_soils")
@PrefixGameTestTemplate(false)
public final class EventideSoilGameTests {
    @GameTest(template="spell_arena") public static void overworldNodeAtGroundLevel(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var base=new BlockPos((origin.getX()&~15)+6,level.getMinBuildHeight()+3,(origin.getZ()&~15)+6);
        var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var ruins=biomes.getHolderOrThrow(EventideRuins.BIOME);var plain=biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        level.getChunkAt(base).fillBiomesFromNoise((x,y,z,s)->y*4<0?ruins:plain,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(base,Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(base.east(),Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(base.east(2),Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(base.south(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base.south(2),ModBlocks.TEMPORAL_MOTE.get().defaultBlockState());
        var outside=new BlockPos(base.getX(),224,base.getZ());level.setBlockAndUpdate(outside,Blocks.DIRT.defaultBlockState());
        var clock=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();long old=level.getGameTime();
        int cx=base.getX()>>4,cz=base.getZ()>>4;boolean forced=level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(cx,cz));
        level.setChunkForced(cx,cz,true);
        try{
            clock.setGameTime(6000);
            com.mcmagic.omnira.time.TimeBiomePulse.tick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,level));
            clock.setGameTime(6001);
            for(int i=0;i<1024&&!level.getBlockState(base).is(DreamContent.TEMPORAL_SILT.get());i++)
                com.mcmagic.omnira.time.TimeBiomePulse.tick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,level));
            h.assertTrue(level.getBlockState(base).is(DreamContent.TEMPORAL_SILT.get()),"Overworld node skipped ground-level grass");
            h.assertTrue(level.getBlockState(base.east()).is(com.mcmagic.omnira.time.EventideMasonry.BRICKS.get()),"Stone bricks unchanged");
            h.assertTrue(level.getBlockState(base.east(2)).is(com.mcmagic.omnira.time.EventideMasonry.CRACKED.get()),"Cracked bricks unchanged");
            h.assertTrue(level.getBlockState(outside).is(Blocks.DIRT),"Different vertical biome was converted");
            h.assertTrue(level.getBlockState(base.south()).is(Blocks.GRAVEL),"Stone skipped gravel or overlap advanced twice");
            EventideRuins.pulseChunk(level,cx,cz,2);
            h.assertTrue(level.getBlockState(base).is(MireContent.SILT.get()),"Second node failed to rot silt");
            h.assertTrue(level.getBlockState(base.south()).is(Blocks.SAND),"Gravel did not become sand");
            EventideRuins.pulseChunk(level,cx,cz,3);
            h.assertTrue(level.getBlockState(base.south()).is(ArchaeologyContent.SAND.get()),"Sand did not become time sand");
        }finally{clock.setGameTime(old);level.setChunkForced(cx,cz,forced);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void soilStages(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var base=new BlockPos((origin.getX()&~15)+2,224,(origin.getZ()&~15)+2);
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EventideRuins.BIOME);
        level.getChunkAt(base).fillBiomesFromNoise((x,y,z,s)->biome,level.getChunkSource().randomState().sampler());
        var blocks=new net.minecraft.world.level.block.Block[]{Blocks.SAND,Blocks.RED_SAND,Blocks.GRAVEL,Blocks.DIRT,Blocks.COARSE_DIRT,Blocks.ROOTED_DIRT,Blocks.GRASS_BLOCK};
        for(int i=0;i<blocks.length;i++){
            level.setBlockAndUpdate(base.east(i).below(),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(base.east(i),blocks[i].defaultBlockState());
        }
        level.setBlockAndUpdate(base.south(),MireContent.SILT.get().defaultBlockState());
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4,1);
        for(int i=0;i<blocks.length;i++)h.assertTrue(level.getBlockState(base.east(i)).is(i<2?ArchaeologyContent.SAND.get():i==2?Blocks.SAND:DreamContent.TEMPORAL_SILT.get()),"Wrong first transition at "+i);
        h.assertTrue(level.getBlockState(base.south()).is(MireContent.SILT.get()),"Rotted silt reset");
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4,2);
        for(int i=3;i<blocks.length;i++)h.assertTrue(level.getBlockState(base.east(i)).is(MireContent.SILT.get()),"Silt did not rot next node");
        var plain=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        level.getChunkAt(base).fillBiomesFromNoise((x,y,z,s)->plain,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(base,Blocks.DIRT.defaultBlockState());EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4,3);
        h.assertTrue(level.getBlockState(base).is(Blocks.DIRT),"Converted outside ruins");h.succeed();
    }
    @GameTest(template="spell_arena") public static void activateForOneEnergy(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(3,3,3));
        level.setBlockAndUpdate(at,ReversalContent.MACHINE.get().defaultBlockState());
        var machine=(ReversalBlockEntity)level.getBlockEntity(at);
        machine.setItem(0,new ItemStack(DreamContent.TEMPORAL_SILT.get()));machine.setItem(2,new ItemStack(ModItems.TIME_MICROCORE.get()));
        for(int i=0;i<100;i++)machine.tick();
        h.assertTrue(machine.getItem(0).isEmpty()&&machine.getItem(1).is(DreamContent.LIVING_TEMPORAL_SILT.get().asItem())&&machine.getItem(1).getCount()==1&&machine.energy()==7,"Activation cost/output incorrect");h.succeed();
    }
}

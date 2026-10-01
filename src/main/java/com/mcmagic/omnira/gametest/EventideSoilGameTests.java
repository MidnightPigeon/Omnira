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
        for(int i=0;i<blocks.length;i++)h.assertTrue(level.getBlockState(base.east(i)).is(i<3?ArchaeologyContent.SAND.get():DreamContent.TEMPORAL_SILT.get()),"Wrong first transition at "+i);
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

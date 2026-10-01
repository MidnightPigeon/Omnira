package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.time.RecurrenceGarden;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_recurrence")
@PrefixGameTestTemplate(false)
public final class RecurrenceGardenGameTests {
    @GameTest(template="spell_arena")
    public static void shallowPonds(GameTestHelper h){
        int garden=0,wet=0;
        boolean found=false;
        for(int x=-384;x<0;x++)for(int z=0;z<384;z++){
            if(!com.mcmagic.omnira.spacetime.StrataBiomeSource.recurrenceGarden(x,z))continue;
            garden++;
            if(!com.mcmagic.omnira.time.RecurrencePonds.contains(x,z))continue;
            wet++;
            if(found)continue;
            found=true;
            h.assertTrue(com.mcmagic.omnira.spacetime.CorridorLayout.block(x,222,z).is(DreamContent.TEMPORAL_SILT.get())
                    &&com.mcmagic.omnira.mire.MireContent.fluid(com.mcmagic.omnira.spacetime.CorridorLayout.block(x,223,z).getFluidState()),
                    "Garden pond is too shallow for eel spawning");
            h.assertTrue(com.mcmagic.omnira.mire.MireContent.fluid(com.mcmagic.omnira.spacetime.CorridorLayout.block(x,224,z).getFluidState())
                    &&com.mcmagic.omnira.spacetime.CorridorLayout.timeSurface(x,z)==224,
                    "Garden pond water must be level with the surrounding ground");
        }
        h.assertTrue(garden>0&&wet>0&&wet<garden/20,"Garden pools missing or too extensive");
        h.succeed();
    }
    @GameTest(template="spell_arena",batch="biome_boundary")
    public static void biomeBoundary(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var inside=new BlockPos((origin.getX()&~15)+15,180,(origin.getZ()&~15)+8);
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var garden=registry.getHolderOrThrow(RecurrenceGarden.BIOME);
        var outside=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME,net.minecraft.resources.ResourceLocation.parse("minecraft:plains")));
        level.getChunkAt(inside).fillBiomesFromNoise((x,y,z,sampler)->garden,level.getChunkSource().randomState().sampler());
        level.getChunkAt(inside.east()).fillBiomesFromNoise((x,y,z,sampler)->outside,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(inside,DreamContent.DREAM_CRYSTAL_BEDROCK.get().defaultBlockState());
        level.setBlockAndUpdate(inside.east(),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(inside.south(),DreamContent.DREAM_CRYSTAL_BEDROCK.get().defaultBlockState());
        level.setBlockAndUpdate(inside.south().east(),DreamContent.SMALL_DREAM_BUD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING,Direction.EAST));
        RecurrenceGarden.pulseChunk(level,inside.getX()>>4,inside.getZ()>>4);
        h.assertTrue(level.getBlockState(inside.east()).isAir(),"Garden matrix grew across biome boundary");
        h.assertTrue(level.getBlockState(inside.south().east()).is(DreamContent.SMALL_DREAM_BUD.get()),"Crystal outside garden advanced");
        h.succeed();
    }

    @GameTest(template="spell_arena",batch="garden_cycle")
    public static void cycle(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var soil=new BlockPos((origin.getX()&~15)+8,180,(origin.getZ()&~15)+8);
        var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(RecurrenceGarden.BIOME);
        var center=new net.minecraft.world.level.ChunkPos(soil);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            level.getChunk(center.x+x,center.z+z).fillBiomesFromNoise((a,b,c,sampler)->holder,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(soil,DreamContent.TEMPORAL_SILT.get().defaultBlockState());
        h.assertTrue(!level.getBlockTicks().hasScheduledTick(soil,DreamContent.TEMPORAL_SILT.get()),"Garden soil retained natural sediment timer");
        level.setBlockAndUpdate(soil.above(),TimeNatureContent.GRASS.get().defaultBlockState());
        var farm=soil.east();level.setBlockAndUpdate(farm,Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(farm.above(),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        var bud=soil.west().above();level.setBlockAndUpdate(bud,DreamContent.DREAM_CRYSTAL.get().defaultBlockState().setValue(AmethystClusterBlock.FACING,Direction.UP));
        var matrix=soil.north(2);level.setBlockAndUpdate(matrix,DreamContent.DREAM_CRYSTAL_BEDROCK.get().defaultBlockState());
        RecurrenceGarden.pulseChunk(level,soil.getX()>>4,soil.getZ()>>4);
        h.assertTrue(level.getBlockState(soil).is(TimeNatureContent.SILT.get()),"Soil did not sediment at node");
        level.setBlockAndUpdate(soil.south(),MireContent.LIQUID.get().defaultBlockState());
        h.assertTrue(!level.getBlockTicks().hasScheduledTick(soil,TimeNatureContent.SILT.get()),"Garden sediment scheduled fluid decay");
        h.assertTrue(level.getBlockState(soil.above()).is(TimeNatureContent.FLOWER.get()),"Grass did not become flower");
        h.assertTrue(level.getBlockState(farm.above()).getValue(CropBlock.AGE)==0,"Mature crop did not restart");
        h.assertTrue(level.getBlockState(bud).is(DreamContent.SMALL_DREAM_BUD.get()),"Mature crystal did not cycle to first stage");
        h.assertTrue(level.getBlockState(matrix.above()).is(DreamContent.SMALL_DREAM_BUD.get()),"Empty matrix face did not start growth");
        RecurrenceGarden.pulseChunk(level,soil.getX()>>4,soil.getZ()>>4);
        h.assertTrue(level.getBlockState(soil).is(MireContent.SILT.get()),"Fluid contact did not add rotted stage");
        h.assertTrue(level.getBlockState(soil.above()).is(TimeNatureContent.GRASS.get()),"Flower did not become grass");
        h.assertTrue(level.getBlockState(bud).is(DreamContent.MEDIUM_DREAM_BUD.get()),"Crystal did not advance exactly one stage");
        h.assertTrue(level.getBlockState(matrix.above()).is(DreamContent.MEDIUM_DREAM_BUD.get()),"New crystal advanced more than one stage per cycle");
        RecurrenceGarden.pulseChunk(level,soil.getX()>>4,soil.getZ()>>4);
        h.assertTrue(level.getBlockState(soil).is(DreamContent.TEMPORAL_SILT.get()),"Rotted soil did not return to ordinary state");
        h.succeed();
    }
}

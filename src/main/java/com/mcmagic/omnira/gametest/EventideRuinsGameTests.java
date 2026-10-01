package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.archaeology.ArchaeologyContent;
import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.spacetime.CorridorLayout;
import com.mcmagic.omnira.spacetime.StrataBiomeSource;
import com.mcmagic.omnira.time.EventideRuins;
import com.mcmagic.omnira.time.EventideMasonry;
import com.mcmagic.omnira.time.TemporalCreatureTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_eventide")
@PrefixGameTestTemplate(false)
public final class EventideRuinsGameTests {
    @GameTest(template="spell_arena") public static void liquidsConvertAtNode(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var base=new BlockPos((origin.getX()&~15)+4,180,(origin.getZ()&~15)+4);
        var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EventideRuins.BIOME);
        level.getChunkAt(base).fillBiomesFromNoise((x,y,z,sampler)->holder,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(base,Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(base.east(2),Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(base.east(4),Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,4));
        var stair=Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true);
        level.setBlockAndUpdate(base.east(6),stair);
        var existing=MireContent.LIQUID.get().defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,3);
        level.setBlockAndUpdate(base.east(8),existing);
        h.assertTrue(level.getBlockState(base).is(Blocks.WATER),"Liquid converted before the node");
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4);
        for(int dx:new int[]{0,2,4})h.assertTrue(level.getBlockState(base.east(dx)).is(MireContent.LIQUID.get()),"Source or flowing liquid not converted");
        h.assertTrue(level.getBlockState(base.east(6)).equals(stair),"Waterlogged building block overwritten");
        h.assertTrue(level.getBlockState(base.east(8)).equals(existing),"Existing timeflow level changed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void creatureTags(GameTestHelper h){
        h.assertTrue(MireContent.EEL.get().is(TemporalCreatureTags.TIME)
                &&ArchaeologyContent.BLINDFISH.get().is(TemporalCreatureTags.TIME),"Time creatures missing entity tag");
        h.assertTrue(ModEntityTypes.DREAM_MIRROR.get().is(TemporalCreatureTags.DREAM)
                &&ModEntityTypes.LIGHT_SPIRIT.get().is(TemporalCreatureTags.DREAM)
                &&ModEntityTypes.SHADOW_GHOST.get().is(TemporalCreatureTags.DREAM),"Dream creatures missing entity tag");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void creaturePulse(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var base=new BlockPos((origin.getX()&~15)+7,180,(origin.getZ()&~15)+7);
        var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EventideRuins.BIOME);
        level.getChunkAt(base).fillBiomesFromNoise((x,y,z,sampler)->holder,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(base.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base.east(3).below(),Blocks.STONE.defaultBlockState());
        Cow baby=EntityType.COW.create(level);baby.setAge(-24000);baby.setPos(base.getX()+.5,base.getY(),base.getZ()+.5);
        Cow adult=EntityType.COW.create(level);adult.setPos(base.getX()+3.5,base.getY(),base.getZ()+.5);
        level.addFreshEntity(baby);level.addFreshEntity(adult);
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4,20);
        h.assertTrue(!baby.isBaby()&&!baby.isRemoved(),"Baby should grow but survive the current cycle");
        h.assertTrue(adult.isRemoved()&&level.getBlockState(base.east(3)).is(ModBlocks.TIME_WARP_POINT.get()),"Adult did not become warp point");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,adult.getBoundingBox().inflate(3)).isEmpty(),"Adult dropped loot");
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4,20);
        h.assertTrue(!baby.isRemoved(),"Grown baby processed twice in one cycle");
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4,21);
        h.assertTrue(baby.isRemoved()&&level.getBlockState(base).is(ModBlocks.TIME_WARP_POINT.get()),"Grown creature did not advance on the next cycle");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void terrain(GameTestHelper h){
        int sand=0,silt=0;boolean found=false;
        for(int x=-320;x<320;x+=4)for(int z=-320;z<320;z+=4){
            if(!StrataBiomeSource.eventideRuins(x,z))continue;
            found=true;
            var surface=CorridorLayout.block(x,CorridorLayout.timeSurface(x,z),z);
            if(surface.is(EventideMasonry.BRICKS.get())||surface.is(EventideMasonry.CRACKED.get())
                    ||surface.is(EventideMasonry.SANDBOUND.get()))sand++;
            else if(surface.is(MireContent.SILT.get()))silt++;
            else h.fail("Unexpected eventide surface material");
        }
        h.assertTrue(found&&sand>5&&silt>5,"Eventide biome or surface mix missing");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void decayAndCrystal(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var base=new BlockPos((origin.getX()&~15)+6,180,(origin.getZ()&~15)+6);
        var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EventideRuins.BIOME);
        level.getChunkAt(base).fillBiomesFromNoise((x,y,z,sampler)->holder,level.getChunkSource().randomState().sampler());
        var plant=base.above();level.setBlockAndUpdate(base,MireContent.SILT.get().defaultBlockState());
        level.setBlockAndUpdate(plant,TimeNatureContent.FLOWER.get().defaultBlockState());
        var crop=base.east().above();level.setBlockAndUpdate(crop.below(),Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState());
        var matrix=base.west(2).above();level.setBlockAndUpdate(matrix,DreamContent.DREAM_CRYSTAL_BEDROCK.get().defaultBlockState());
        level.setBlockAndUpdate(matrix.east().below(),Blocks.STONE.defaultBlockState());
        var placedCluster=base.west(3).above();
        level.setBlockAndUpdate(placedCluster.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(placedCluster,DreamContent.DREAM_CRYSTAL.get().defaultBlockState());
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4);
        h.assertTrue(level.getBlockState(plant).isAir()&&level.getBlockState(crop).is(Blocks.WHEAT)
                &&level.getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE)==7,"Plant or immature crop did not advance correctly");
        h.assertTrue(level.getBlockState(placedCluster).is(ArchaeologyContent.SAND.get()),"Placed mature cluster did not decay");
        h.assertTrue(level.getBlockState(matrix.east()).is(DreamContent.DREAM_CRYSTAL.get()),"Matrix did not form a full cluster");
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4);
        h.assertTrue(level.getBlockState(matrix.east()).is(ArchaeologyContent.SAND.get())&&level.getBlockState(crop).isAir(),"Mature crop or cluster did not decay");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void placedSiltAndBud(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,1,4));
        var base=new BlockPos((origin.getX()&~15)+8,180,(origin.getZ()&~15)+8);
        var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EventideRuins.BIOME);
        level.getChunkAt(base).fillBiomesFromNoise((x,y,z,sampler)->holder,level.getChunkSource().randomState().sampler());
        var silts=new net.minecraft.world.level.block.Block[]{DreamContent.TEMPORAL_SILT.get(),DreamContent.LIVING_TEMPORAL_SILT.get(),
                TimeNatureContent.SILT.get(),TimeNatureContent.LIVING_SILT.get()};
        for(int i=0;i<silts.length;i++)level.setBlockAndUpdate(base.offset(i,0,0),silts[i].defaultBlockState());
        var bud=base.above();level.setBlockAndUpdate(bud,TimeNatureContent.SMALL_SPATIAL_BUD.get().defaultBlockState());
        EventideRuins.pulseChunk(level,base.getX()>>4,base.getZ()>>4);
        for(int i=0;i<silts.length;i++)h.assertTrue(level.getBlockState(base.offset(i,0,0)).is(MireContent.SILT.get()),"Silt did not decay at node");
        h.assertTrue(level.getBlockState(bud).is(TimeNatureContent.SPATIAL_CLUSTER.get()),"Placed bud did not mature at node");
        h.succeed();
    }
}

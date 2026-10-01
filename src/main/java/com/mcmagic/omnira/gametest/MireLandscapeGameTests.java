package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_landscape")
@PrefixGameTestTemplate(false)
public final class MireLandscapeGameTests {
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void vegetationPlacement(GameTestHelper h){
        var l=h.getLevel();
        var biomes=l.registryAccess().registryOrThrow(Registries.BIOME);
        var mire=biomes.getHolderOrThrow(MireCycle.BIOME);
        var corridor=biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:spacetime_corridor")));
        var feature=l.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(ResourceLocation.parse("omnira:mire_vegetation"));
        int grass=0,trees=0,lilies=0;
        var base=h.absolutePos(new BlockPos(96,0,96));
        for(int cx=0;cx<4;cx++)for(int cz=0;cz<4;cz++){
            int ox=((base.getX()>>4)+cx)*16,oz=((base.getZ()>>4)+cz)*16;
            l.getChunkAt(new BlockPos(ox,224,oz)).fillBiomesFromNoise((x,y,z,s)->y*4>=192?mire:corridor,l.getChunkSource().randomState().sampler());
            for(int x=ox;x<ox+16;x++)for(int z=oz;z<oz+16;z++){
                for(int y=225;y<245;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
                l.setBlock(new BlockPos(x,224,z),x%16<8?TimeNatureContent.SILT.get().defaultBlockState():MireContent.LIQUID.get().defaultBlockState(),2);
            }
            feature.placeWithBiomeCheck(l,l.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(cx*31L+cz),new BlockPos(ox,0,oz));
            for(int x=ox;x<ox+16;x++)for(int z=oz;z<oz+16;z++){
                var p=new BlockPos(x,225,z);var state=l.getBlockState(p);
                if(state.is(TimeNatureContent.GRASS.get())){grass++;h.assertTrue(l.getFluidState(p.below()).isEmpty(),"Grass placed on water");}
                if(state.is(MireContent.DECAYED.get()))lilies++;
                if(state.is(TimeNatureContent.LOG.get()))trees++;
            }
        }
        h.assertTrue(grass>0&&lilies>0&&trees>0,"Missing mire vegetation: "+grass+" grass, "+lilies+" lilies, "+trees+" trees");
        h.succeed();
    }
}

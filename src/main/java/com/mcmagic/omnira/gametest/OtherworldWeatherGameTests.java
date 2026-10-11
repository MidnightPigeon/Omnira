package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_otherworld_weather") @PrefixGameTestTemplate(false)
public final class OtherworldWeatherGameTests {
    @GameTest(template="spell_arena") public static void rainAndThunderCannotLeakFromOverworld(GameTestHelper h)throws Exception{
        var overworld=h.getLevel().getServer().overworld();var data=(ServerLevelData)overworld.getLevelData();
        int clear=data.getClearWeatherTime(),rainTime=data.getRainTime(),thunderTime=data.getThunderTime();
        boolean rain=data.isRaining(),thunder=data.isThundering();float rainStrength=overworld.getRainLevel(1),thunderStrength=overworld.thunderLevel;
        try{
            overworld.setWeatherParameters(0,6000,true,true);overworld.setRainLevel(1);overworld.setThunderLevel(1);
            h.assertTrue(overworld.isRaining()&&overworld.isThundering(),"Overworld weather was disabled");
            var cycle=ServerLevel.class.getDeclaredMethod("advanceWeatherCycle");cycle.setAccessible(true);
            for(var key:java.util.List.of(ModDimensions.DREAM_REALM,ModDimensions.SPACETIME_CORRIDOR)){
                var level=overworld.getServer().getLevel(key);h.assertTrue(level!=null,"Missing test dimension");
                h.assertTrue(level.getLevelData().isRaining(),"Test does not exercise inherited world weather");
                level.setRainLevel(1);level.setThunderLevel(1);
                h.assertTrue(level.rainLevel==0&&level.thunderLevel==0,"Weather packet setters retained nonzero strength");
                level.rainLevel=1;level.thunderLevel=1;
                h.assertTrue(level.getRainLevel(.5F)==0&&level.getThunderLevel(.5F)==0&&!level.isRaining()&&!level.isThundering(),"Otherworld exposes rain or thunder");
                cycle.invoke(level);
                h.assertTrue(level.rainLevel==0&&level.thunderLevel==0,"Weather cycle retained stale strength");
                level.setWeatherParameters(9000,9000,false,false);
                h.assertTrue(data.isRaining()&&data.isThundering()&&data.getRainTime()==6000&&data.getClearWeatherTime()==0,"Otherworld changed shared overworld weather");
                h.assertTrue(!level.isRainingAt(new BlockPos(0,250,0)),"High terrain sees rain");
            }
            h.assertTrue(!OtherworldWeather.disabled(overworld)&&!OtherworldWeather.disabled(overworld.getServer().getLevel(Level.NETHER)),"Suppression affects unrelated dimensions");
        }finally{
            overworld.setWeatherParameters(clear,rainTime,rain,thunder);data.setThunderTime(thunderTime);
            overworld.setRainLevel(rainStrength);overworld.setThunderLevel(thunderStrength);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void coldHighTerrainCannotAccumulateSnowOrIce(GameTestHelper h){
        for(var key:java.util.List.of(ModDimensions.DREAM_REALM,ModDimensions.SPACETIME_CORRIDOR)){
            var level=h.getLevel().getServer().getLevel(key);h.assertTrue(level!=null,"Missing test dimension");
            var chunk=level.getChunk(0,0);
            var cold=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.SNOWY_PLAINS);
            var section=chunk.getSections()[level.getSectionIndex(240)];var old=section.getBiomes().recreate();
            // Preserve all biome cells while changing only the high test section.
            for(int x=0;x<4;x++)for(int y=0;y<4;y++)for(int z=0;z<4;z++)old.set(x,y,z,section.getBiomes().get(x,y,z));
            var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(int x=5;x<=7;x++)for(int z=5;z<=7;z++)for(int y=247;y<=255;y++){
                var p=new BlockPos(x,y,z);saved.put(p,level.getBlockState(p));level.setBlock(p,y==247?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            }
            try{
                chunk.fillBiomesFromNoise((x,y,z,sampler)->y>=60?cold:chunk.getNoiseBiome(x,y,z),level.getChunkSource().randomState().sampler());
                var water=new BlockPos(6,248,6);level.setBlock(water,Blocks.WATER.defaultBlockState(),2);
                h.assertTrue(level.getBiome(water).is(Biomes.SNOWY_PLAINS),"Cold high-altitude test biome missing");
                h.assertTrue(cold.value().shouldFreeze(level,water),"Cold test water would not freeze without guard");
                level.tickPrecipitation(water);
                h.assertTrue(level.getBlockState(water).is(Blocks.WATER)&&level.getBlockState(water.above()).isAir(),"Otherworld precipitation froze water or placed snow");
                level.setBlock(water,Blocks.AIR.defaultBlockState(),2);
                BlockPos snow=null;
                search:for(int x=5;x<=7;x++)for(int z=5;z<=7;z++)for(int y=248;y<=254;y++){
                    var p=new BlockPos(x,y,z);
                    if(!level.getBlockState(p).isAir()||level.getBrightness(LightLayer.BLOCK,p)>=10)continue;
                    level.setBlock(p.below(),Blocks.STONE.defaultBlockState(),2);
                    if(cold.value().shouldSnow(level,p)){snow=p;break search;}
                }
                h.assertTrue(snow!=null,"No dark, supported cold snow test position");
                level.tickPrecipitation(snow);
                h.assertTrue(level.getBlockState(snow).isAir(),"High otherworld terrain accumulated snow");
            }finally{
                saved.forEach((p,s)->level.setBlock(p,s,2));
                chunk.fillBiomesFromNoise((x,y,z,sampler)->y>=60?old.get(Math.floorMod(x,4),Math.floorMod(y,4),Math.floorMod(z,4)):chunk.getNoiseBiome(x,y,z),level.getChunkSource().randomState().sampler());
            }
        }
        h.succeed();
    }
}

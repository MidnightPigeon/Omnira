package com.mcmagic.omnira.spacetime;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class CorridorGenerator extends ChunkGenerator {
    private static final int[][] STRATA={{24,CorridorLayout.SPACE_CEILING},{CorridorLayout.SEPARATOR_BOTTOM,CorridorLayout.SEPARATOR_TOP},{CorridorLayout.TIME_BASE,230}};
    public static final MapCodec<CorridorGenerator> CODEC=RecordCodecBuilder.mapCodec(i->i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(CorridorGenerator::getBiomeSource)).apply(i,CorridorGenerator::new));
    public CorridorGenerator(BiomeSource source){super(source);}
    @Override protected MapCodec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public java.util.concurrent.CompletableFuture<ChunkAccess> fillFromNoise(Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
        var pos=new BlockPos.MutableBlockPos();
        for(int lx=0;lx<16;lx++)for(int lz=0;lz<16;lz++){
            int x=chunk.getPos().getBlockX(lx),z=chunk.getPos().getBlockZ(lz);
            for(var band:STRATA)for(int y=band[0];y<=band[1];y++){
                var state=CorridorLayout.block(x,y,z);if(state.isAir())continue;
                pos.set(x,y,z);chunk.setBlockState(pos,state,false);
                if(state.is(com.mcmagic.omnira.archaeology.ArchaeologyContent.SUSPICIOUS.get())){
                    var be=new net.minecraft.world.level.block.entity.BrushableBlockEntity(pos.immutable(),state);
                    be.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                            net.minecraft.resources.ResourceLocation.parse("omnira:archaeology/epoch_"+state.getValue(com.mcmagic.omnira.archaeology.TimeSandBlock.ERA))),
                            com.mcmagic.omnira.archaeology.EpochalTerrain.hash(x,y,z));chunk.setBlockEntity(be);
                }
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG).update(lx,y,lz,state);
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).update(lx,y,lz,state);
            }
        }
        return java.util.concurrent.CompletableFuture.completedFuture(chunk);
    }
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor height,RandomState random){
        for(int y=255;y>=0;y--)if(type.isOpaque().test(CorridorLayout.block(x,y,z)))return y+1;
        return 0;
    }
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor height,RandomState random){
        var states=new net.minecraft.world.level.block.state.BlockState[256];for(int y=0;y<256;y++)states[y]=CorridorLayout.block(x,y,z);return new NoiseColumn(0,states);
    }
    @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step){}
    @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk){}
    @Override public void applyBiomeDecoration(WorldGenLevel level,ChunkAccess chunk,StructureManager structures){super.applyBiomeDecoration(level,chunk,structures);}
    @Override public void spawnOriginalMobs(WorldGenRegion region){}
    @Override public int getGenDepth(){return 256;}
    @Override public int getMinY(){return 0;}
    @Override public int getSeaLevel(){return 0;}
    @Override public int getSpawnHeight(LevelHeightAccessor height){return 129;}
    @Override public void addDebugScreenInfo(java.util.List<String> lines,RandomState random,BlockPos pos){}
}

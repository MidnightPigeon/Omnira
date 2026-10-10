package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.archaeology.ArchaeologyContent;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.time.TemporalSoils;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.common.Tags;

/** Chunk-owned targets prevent overlapping sources from advancing a block twice. */
public final class TemporalWeathering {
    private static final TagKey<Block> STONES=TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:stones"));
    private static final TagKey<Block> DIRTS=TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:dirts"));
    private static final TagKey<Block> SANDS=TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:sands"));

    public static boolean source(BlockState state){return state.is(ModBlocks.TIME_WARP_POINT.get())||state.is(ModBlocks.TEMPORAL_MOTE.get());}

    public static void pulseChunk(ServerLevel level,int cx,int cz){
        var targets=new HashSet<BlockPos>();
        for(int nx=cx-1;nx<=cx+1;nx++)for(int nz=cz-1;nz<=cz+1;nz++){
            var chunk=level.getChunkSource().getChunk(nx,nz,ChunkStatus.FULL,false);
            if(chunk==null)continue;
            var sections=chunk.getSections();
            for(int i=0;i<sections.length;i++){
                var section=sections[i];if(!section.maybeHas(TemporalWeathering::source))continue;
                int baseY=chunk.getSectionYFromSectionIndex(i)<<4;
                for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                    if(!source(section.getBlockState(x,y,z)))continue;
                    var center=new BlockPos((nx<<4)+x,baseY+y,(nz<<4)+z);
                    for(int dx=-5;dx<=5;dx++)for(int dy=-5;dy<=5;dy++)for(int dz=-5;dz<=5;dz++){
                        if(dx*dx+dy*dy+dz*dz>25)continue;
                        var target=center.offset(dx,dy,dz);
                        if((target.getX()>>4)==cx&&(target.getZ()>>4)==cz&&!level.isOutsideBuildHeight(target))targets.add(target);
                    }
                }
            }
        }
        for(var pos:targets){
            // The biome owns this node; overlapping anomalies must not advance it twice.
            if(level.getBiome(pos).is(com.mcmagic.omnira.time.EventideRuins.BIOME))continue;
            var state=level.getBlockState(pos);var next=next(state,level.random.nextBoolean());
            if(next!=state&&level.getBlockEntity(pos)==null)level.setBlockAndUpdate(pos,next);
        }
    }

    public static BlockState next(BlockState state,boolean active){
        if(state.is(TemporalSoils.SILTS)||state.is(ArchaeologyContent.SAND.get()))return state;
        var mineral=mineralNext(state);if(mineral!=state)return mineral;
        if(state.is(BlockTags.DIRT)||state.is(DIRTS)||state.is(Blocks.FARMLAND))
            return (active?DreamContent.LIVING_TEMPORAL_SILT:DreamContent.TEMPORAL_SILT).get().defaultBlockState();
        return state;
    }
    public static BlockState mineralNext(BlockState state){
        if(state.is(TemporalSoils.SILTS)||state.is(ArchaeologyContent.SAND.get()))return state;
        if(state.is(BlockTags.SAND)||state.is(SANDS))return ArchaeologyContent.SAND.get().defaultBlockState();
        if(state.is(Tags.Blocks.GRAVELS))return Blocks.SAND.defaultBlockState();
        if(state.is(STONES))return Blocks.GRAVEL.defaultBlockState();
        return state;
    }
    private TemporalWeathering(){}
}

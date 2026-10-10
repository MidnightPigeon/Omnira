package com.mcmagic.omnira.space;

import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Older remnants were ordinary blocks and have no saved block entity yet. */
@EventBusSubscriber(modid="omnira")
public final class SpatialRemnantMigration {
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!event.getLevel().isClientSide()&&event.getChunk() instanceof LevelChunk chunk)restoreMissing(chunk);
    }
    public static void restoreMissing(LevelChunk chunk){
        var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++){
            var section=sections[i];
            if(!section.maybeHas(s->s.is(ModBlocks.SPATIAL_REMNANT.get())))continue;
            int baseY=chunk.getSectionYFromSectionIndex(i)<<4;
            for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                if(section.getBlockState(x,y,z).is(ModBlocks.SPATIAL_REMNANT.get()))
                    chunk.getBlockEntity(new BlockPos(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z),LevelChunk.EntityCreationType.IMMEDIATE);
            }
        }
    }
    private SpatialRemnantMigration(){}
}

package com.mcmagic.omnira.space.geodes;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Only loaded crystal buds and clusters exert a continuous field. Floor contacts queue turns separately. */
@EventBusSubscriber(modid="omnira")
public final class DeflectionSources {
    private static final java.util.Map<ServerLevel,DeflectionSources> LEVELS=new java.util.WeakHashMap<>();
    private final Long2ObjectOpenHashMap<Long2IntOpenHashMap> sections=new Long2ObjectOpenHashMap<>();

    public static void update(ServerLevel level,BlockPos pos,int radius){
        var sources=LEVELS.get(level);
        if(sources==null){
            if(radius==0)return;
            sources=new DeflectionSources();
            LEVELS.put(level,sources);
        }
        long section=SectionPos.asLong(pos);
        if(radius>0)sources.sections.computeIfAbsent(section,k->new Long2IntOpenHashMap()).put(pos.asLong(),radius);
        else{
            var entries=sources.sections.get(section);
            if(entries!=null){
                entries.remove(pos.asLong());
                if(entries.isEmpty())sources.sections.remove(section);
            }
        }
    }
    @SubscribeEvent public static void load(ChunkEvent.Load event){
        if(event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level)
            chunk.findBlocks((BlockState s)->DeflectionContent.radius(s)>0,(pos,state)->update(level,pos,DeflectionContent.radius(state)));
    }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event){
        if(!(event.getChunk() instanceof LevelChunk chunk)||!(chunk.getLevel() instanceof ServerLevel level))return;
        var sources=LEVELS.get(level);
        if(sources==null)return;
        for(int y=chunk.getMinSection();y<chunk.getMaxSection();y++)
            sources.sections.remove(SectionPos.asLong(chunk.getPos().x,y,chunk.getPos().z));
    }
    static int coverage(ServerLevel level,Entity entity){
        var sources=LEVELS.get(level);
        if(sources==null||sources.sections.isEmpty())return 0;
        var b=entity.getBoundingBox();
        int minSectionY=Math.max(level.getMinSection(),Mth.floor(b.minY-5)>>4);
        int maxSectionY=Math.min(level.getMaxSection()-1,Mth.floor(b.maxY+5)>>4);
        int count=0;
        for(int x=Mth.floor(b.minX-5)>>4;x<=Mth.floor(b.maxX+5)>>4;x++)
            for(int y=minSectionY;y<=maxSectionY;y++)
                for(int z=Mth.floor(b.minZ-5)>>4;z<=Mth.floor(b.maxZ+5)>>4;z++){
                    var entries=sources.sections.get(SectionPos.asLong(x,y,z));
                    if(entries==null)continue;
                    var it=entries.long2IntEntrySet().fastIterator();
                    while(it.hasNext()){
                        var e=it.next();long p=e.getLongKey();
                        double cx=BlockPos.getX(p)+.5,cy=BlockPos.getY(p)+.5,cz=BlockPos.getZ(p)+.5;
                        double dx=cx-Math.clamp(cx,b.minX,b.maxX),dy=cy-Math.clamp(cy,b.minY,b.maxY),dz=cz-Math.clamp(cz,b.minZ,b.maxZ);
                        if(dx*dx+dy*dy+dz*dz<=e.getIntValue()*e.getIntValue())count++;
                    }
                }
        return count;
    }
}

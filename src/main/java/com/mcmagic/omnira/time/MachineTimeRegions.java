package com.mcmagic.omnira.time;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Per-level index of loaded sections that can sample either local time biome. */
@EventBusSubscriber(modid="omnira")
public final class MachineTimeRegions {
    public interface Owner { MachineTimeRegions omnira$timeRegions(); }
    public interface Section { void omnira$trackTime(MachineTimeRegions regions, long key); }

    private final Long2IntOpenHashMap candidates = new Long2IntOpenHashMap();

    public boolean active() { return !candidates.isEmpty(); }
    public boolean nearby(BlockPos pos) {
        return pos != null && candidates.containsKey(SectionPos.asLong(pos));
    }

    public void change(long source, int delta) {
        // Vanilla's blended sample can cross any section face, edge or corner.
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++) {
            long key=SectionPos.offset(source,x,y,z);
            if (candidates.addTo(key,delta)+delta==0) candidates.remove(key);
        }
    }

    public static void track(LevelChunk chunk, MachineTimeRegions regions) {
        var pos=chunk.getPos();
        var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++) {
            long key=SectionPos.asLong(pos.x,chunk.getMinSection()+i,pos.z);
            ((Section)sections[i]).omnira$trackTime(regions,key);
        }
    }

    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if(event.getChunk() instanceof LevelChunk chunk)
            track(chunk,((Owner)chunk.getLevel()).omnira$timeRegions());
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event) {
        if(event.getChunk() instanceof LevelChunk chunk) track(chunk,null);
    }
}

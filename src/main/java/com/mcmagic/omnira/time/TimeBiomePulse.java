package com.mcmagic.omnira.time;

import com.mcmagic.omnira.world.dimension.ModDimensions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** One five-minute clock and loaded-chunk work queue per level. */
@EventBusSubscriber(modid="omnira")
public final class TimeBiomePulse {
    public static final int PERIOD=6000;
    private record Batch(long cycle,ArrayDeque<Long> chunks,ArrayList<BlockPos> gardenMatrices) {}
    private static final WeakHashMap<ServerLevel,ArrayDeque<Batch>> PENDING=new WeakHashMap<>();

    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        boolean spacetime=level.dimension().equals(ModDimensions.SPACETIME_CORRIDOR);
        if(!spacetime&&!level.dimension().equals(Level.OVERWORLD))return;
        long now=level.getGameTime();
        if(now>0&&now%PERIOD==0)enqueue(level);
        var batches=PENDING.get(level);
        if(batches==null||batches.isEmpty())return;
        var batch=batches.peekFirst();
        long key=batch.chunks().removeFirst();int cx=ChunkPos.getX(key),cz=ChunkPos.getZ(key);
        if(level.getChunkSource().getChunk(cx,cz,ChunkStatus.FULL,false)!=null){
            RecurrenceGarden.pulseChunk(level,cx,cz,batch.gardenMatrices());
            if(spacetime)EventideRuins.pulseChunk(level,cx,cz,batch.cycle());
        }
        if(batch.chunks().isEmpty()){
            RecurrenceGarden.finish(level,batch.gardenMatrices());
            batches.removeFirst();
            if(batches.isEmpty())PENDING.remove(level);
        }
    }

    private static void enqueue(ServerLevel level){
        var chunks=new ArrayDeque<Long>();var seen=new HashSet<Long>();
        int radius=level.getServer().getPlayerList().getViewDistance();
        for(var player:level.players()){
            // Players are sampled at the node, not later as queued chunks are processed.
            if(level.dimension().equals(ModDimensions.SPACETIME_CORRIDOR))
                EventideRuins.pulsePlayer(level,player,level.getGameTime()/PERIOD);
            if(level.getBiome(player.blockPosition()).is(RecurrenceGarden.BIOME))RecurrenceGarden.advanceGrid(player);
            int cx=player.getBlockX()>>4,cz=player.getBlockZ()>>4;
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                int x=cx+dx,z=cz+dz;long key=ChunkPos.asLong(x,z);
                if(seen.add(key)&&level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false)!=null)chunks.addLast(key);
            }
        }
        if(!chunks.isEmpty())PENDING.computeIfAbsent(level,ignored->new ArrayDeque<>()).addLast(new Batch(level.getGameTime()/PERIOD,chunks,new ArrayList<>()));
    }

    private TimeBiomePulse(){}
}

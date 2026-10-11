package com.mcmagic.omnira.space.anchor;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/** Rotating shares keep many sources from multiplying terrain scans without bound. */
public final class ActivationScanBudget {
    private static final Map<ServerLevel,State> LEVELS=new WeakHashMap<>();
    private static final class State {long tick=Long.MIN_VALUE;int remaining;final Map<BlockPos,Long> sources=new LinkedHashMap<>();}
    public static int take(ServerLevel level,BlockPos owner){
        var s=LEVELS.computeIfAbsent(level,k->new State());long now=level.getGameTime();
        if(s.tick!=now){s.tick=now;s.remaining=4096;s.sources.values().removeIf(t->now-t>40);}
        s.sources.put(owner.immutable(),now);int size=s.sources.size(),index=0;
        for(var pos:s.sources.keySet()){if(pos.equals(owner))break;index++;}
        int share=Math.min(512,Math.max(1,4096/size));
        int rank=Math.floorMod(index-(int)Math.floorMod(now,size),size);
        if(rank>=4096/share)return 0;
        int granted=Math.min(share,s.remaining);s.remaining-=granted;return granted;
    }
    private ActivationScanBudget(){}
}

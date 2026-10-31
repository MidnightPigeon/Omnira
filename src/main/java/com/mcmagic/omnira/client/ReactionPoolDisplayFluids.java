package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.PoolFluids;
import java.util.*;
import net.neoforged.neoforge.fluids.FluidStack;

/** Render-thread snapshots only. Never expose these mutable stacks to transfer code. */
public final class ReactionPoolDisplayFluids {
    private record Snapshot(long revision,List<FluidStack> layers) {}
    private static final Map<PoolFluids,Snapshot> CACHE=new WeakHashMap<>();
    public static List<FluidStack> layers(PoolFluids tank){
        var snapshot=CACHE.get(tank);
        if(snapshot==null||snapshot.revision()!=tank.revision()){
            snapshot=new Snapshot(tank.revision(),tank.snapshot().reversed());
            CACHE.put(tank,snapshot);
        }
        return snapshot.layers();
    }
    public static void reset(){CACHE.clear();}
    private ReactionPoolDisplayFluids(){}
}

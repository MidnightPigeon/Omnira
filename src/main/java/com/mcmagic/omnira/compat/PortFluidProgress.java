package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.space.SpacetimePortState;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import java.lang.ref.WeakReference;
import java.util.*;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Called after a real fill, following only this network's already-discovered outbound flows. */
public final class PortFluidProgress {
    public static List<SpacetimePortState> route(Level world,BlockFace start,BlockFace end,FluidStack fluid,
            Map<BlockPos,WeakReference<FluidTransportBehaviour>> pipes){
        record Step(BlockPos pos,List<SpacetimePortState> ports){}
        var queue=new ArrayDeque<Step>();var seen=new HashSet<BlockPos>();queue.add(new Step(start.getPos(),List.of()));
        while(!queue.isEmpty()){
            var step=queue.remove();if(!seen.add(step.pos))continue;
            if(step.pos.equals(end.getPos()))return step.ports;
            var ref=pipes.get(step.pos);var pipe=ref==null?null:ref.get();if(pipe==null)continue;
            for(var entry:pipe.interfaces.entrySet()){
                var outbound=entry.getValue().provideOutboundFlow();if(outbound.isEmpty()||!FluidStack.isSameFluidSameComponents(outbound,fluid))continue;
                var edge=new PortFluidFace(world,step.pos,entry.getKey());var next=edge.getConnectedPos();
                if(!pipes.containsKey(next)&&!next.equals(end.getPos()))continue;
                var ports=step.ports;var port=SpacetimePortState.at(world,step.pos.relative(entry.getKey()));
                if(port!=null&&port.other()!=null&&!next.equals(step.pos.relative(entry.getKey()))){ports=new ArrayList<>(ports);ports.add(port);ports.add(port.other());}
                queue.add(new Step(next,ports));
            }
        }return null;
    }
    private PortFluidProgress(){}
}

package com.mcmagic.omnira.compat;

import com.simibubi.create.content.fluids.*;
import com.simibubi.create.content.fluids.pump.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class PortFluidUpdates {
    private record Step(BlockPos pos,int distance){}
    private record Pump(PumpBlockEntity entity,Direction side){}
    public static boolean propagate(LevelAccessor world,BlockPos start,BlockState initial){
        if(!(world instanceof Level level)||level.isClientSide)return false;
        var queue=new ArrayDeque<Step>();var visited=new HashSet<BlockPos>();
        var pipes=new ArrayList<FluidTransportBehaviour>();var pumps=new HashSet<Pump>();
        boolean remote=false;queue.add(new Step(start,0));
        while(!queue.isEmpty()){
            var step=queue.remove();if(!level.hasChunkAt(step.pos)||!visited.add(step.pos))continue;
            var pipe=FluidPropagator.getPipe(level,step.pos);if(pipe==null)continue;pipes.add(pipe);
            var state=step.pos.equals(start)?initial:level.getBlockState(step.pos);
            for(var side:FluidPropagator.getPipeConnections(state,pipe)){
                var edge=new PortFluidFace(level,step.pos,side);var target=edge.getConnectedPos();var incoming=edge.getOppositeFace();
                remote|=!target.equals(step.pos.relative(side));if(!level.hasChunkAt(target))continue;
                var nextState=level.getBlockState(target);var entity=level.getBlockEntity(target);
                if(entity instanceof PumpBlockEntity pump){
                    if(nextState.getBlock() instanceof PumpBlock&&nextState.getValue(PumpBlock.FACING).getAxis()==incoming.getAxis())pumps.add(new Pump(pump,incoming));
                }else if(!visited.contains(target)){
                    var next=FluidPropagator.getPipe(level,target);
                    if(next!=null&&(step.distance<FluidPropagator.getPumpRange()||next.hasAnyPressure())&&next.canHaveFlowToward(nextState,incoming))queue.add(new Step(target,step.distance+1));
                }
            }
        }
        if(!remote)return false;
        pipes.forEach(FluidTransportBehaviour::wipePressure);pumps.forEach(p->p.entity.updatePipesOnSide(p.side));return true;
    }
    public static void changed(Level level,BlockPos pos){
        if(!level.isClientSide&&level.hasChunkAt(pos))FluidPropagator.propagateChangedPipe(level,pos,level.getBlockState(pos));
    }
    private PortFluidUpdates(){}
}

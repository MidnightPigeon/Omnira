package com.mcmagic.omnira.compat.create;

import com.mcmagic.omnira.block.entity.KineticAssemblyBlockEntity;
import com.mcmagic.omnira.block.entity.ManaEngineBlockEntity;
import com.mcmagic.omnira.compat.CreateReactionPool;
import com.mcmagic.omnira.compat.CreateSpacetimePort;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.Direction;

/** Optional Create accounting; never discard a reservation for an unverified topology. */
public final class OmniraKineticAccounting {
    public static float fixedImpact(KineticBlockEntity machine,float total){
        float rpm=Math.abs(machine.getTheoreticalSpeed());
        if(rpm==0)return 0;
        float coefficient=total/rpm;
        return (double)coefficient*rpm>total?Math.nextDown(coefficient):coefficient;
    }

    public static void refreshImpact(KineticBlockEntity machine,float coefficient){
        if(!machine.hasNetwork())return;
        var net=machine.getOrCreateNetwork();
        if(!java.util.Objects.equals(net.members.get(machine),coefficient))net.updateStressFor(machine,coefficient);
        if(machine.getLevel().getGameTime()%20==0)repairClosedNetwork(net);
    }

    public static void repairClosedNetwork(KineticNetwork net){
        if(net.members.isEmpty()||net.members.size()>64)return;
        double stress=0,capacity=0;
        // First check the cheap aggregate; most networks need no topology walk.
        for(var member:net.members.keySet())stress+=net.getActualStressOf(member);
        for(var source:net.sources.keySet())capacity+=net.getActualCapacityOf(source);
        if(net.getSize()==net.members.size()&&Math.abs(net.calculateStress()-stress)<.001
                &&Math.abs(net.calculateCapacity()-capacity)<.001)return;
        if(closedLoadedNetwork(net)){
            net.initFromTE(0,0,0);
            // Create's aggregate updater does not sync a size-only change.
            net.sync();
        }
    }

    private static boolean closedLoadedNetwork(KineticNetwork net){
        if(net.members.isEmpty()||net.members.size()>64||net.sources.isEmpty())return false;
        for(var source:net.sources.keySet())if(!net.members.containsKey(source))return false;
        for(var member:net.members.keySet()){
            // Unknown gearboxes, belts and third-party connections may reach unloaded consumers.
            if(!(member instanceof KineticAssemblyBlockEntity||member instanceof KineticCrystalWorkstationEntity
                    ||member instanceof ManaEngineBlockEntity||member instanceof SpacetimeEngineKinetics.Output
                    ||member instanceof CreateReactionPool.Port||member instanceof CreateSpacetimePort.PortEntity))return false;
            var level=member.getLevel();var pos=member.getBlockPos();var state=member.getBlockState();
            if(level==null||!java.util.Objects.equals(member.network,net.id)||!level.hasChunkAt(pos)
                    ||level.getBlockEntity(pos)!=member||!(state.getBlock() instanceof IRotate rotate))return false;
            for(var face:Direction.values())if(rotate.hasShaftTowards(level,pos,state,face)){
                var adjacent=pos.relative(face);
                if(!level.hasChunkAt(adjacent))return false;
                var otherState=level.getBlockState(adjacent);
                if(otherState.getBlock() instanceof IRotate other&&other.hasShaftTowards(level,adjacent,otherState,face.getOpposite())){
                    var entity=level.getBlockEntity(adjacent);
                    if(!(entity instanceof KineticBlockEntity kinetic)||!net.members.containsKey(kinetic))return false;
                }
            }
            if(member instanceof CreateSpacetimePort.PortEntity endpoint){
                var port=endpoint.port();var other=port.other();
                if(other==null||!CreateSpacetimePort.shaft(port)||!CreateSpacetimePort.shaft(other)
                        ||!net.members.containsKey(other.host))return false;
            }
        }
        return true;
    }
    private OmniraKineticAccounting(){}
}

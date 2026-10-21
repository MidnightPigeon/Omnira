package com.mcmagic.omnira.compat.create;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;

/** Loaded only behind the Create presence check. */
public final class PulseKinetics {
    public static boolean connects(Level level,BlockPos pos,Direction side){var state=level.getBlockState(pos);return state.getBlock() instanceof com.simibubi.create.content.kinetics.base.IRotate rotate&&rotate.hasShaftTowards(level,pos,state,side);}
    public static boolean nativeSource(net.minecraft.world.level.block.entity.BlockEntity source){
        return source instanceof com.mcmagic.omnira.block.entity.ManaEngineAccess||source instanceof SpacetimeEngineKinetics.Output;
    }
    public static int output(net.minecraft.world.level.block.entity.BlockEntity source){
        return source instanceof SpacetimeEngineKinetics.Output?com.mcmagic.omnira.engine.SpacetimeEngineBlock.side(source.getBlockState().getValue(com.mcmagic.omnira.engine.SpacetimeEngineBlock.PART)):0;
    }
    public static void processed(net.minecraft.world.level.block.entity.BlockEntity host){
        if(!(host instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity kinetic)||!kinetic.hasNetwork()||kinetic.isOverStressed()||kinetic.getSpeed()==0)return;
        var network=kinetic.getOrCreateNetwork();
        if(network.getActualStressOf(kinetic)<=0)return;
        var path=new java.util.HashSet<BlockPos>();var at=kinetic;
        while(path.add(at.getBlockPos())){
            if(nativeSource(at)&&externalLoad(at)>0)com.mcmagic.omnira.progression.BranchProgress.machine(host,"first_pulse");
            var orb=com.mcmagic.omnira.pulse.PulseOrbEntity.from(at);
            if(orb!=null)com.mcmagic.omnira.pulse.PulseNetworks.convertedProcessing(orb,host);
            if(at instanceof com.mcmagic.omnira.space.SpacetimePortHost port&&port.port().other()!=null)
                com.mcmagic.omnira.progression.BranchProgress.port(at,1);
            if(!at.hasSource()||!at.getLevel().hasChunkAt(at.source))break;
            var next=at.getLevel().getBlockEntity(at.source);
            if(!(next instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity member))break;at=member;
        }
    }
    /** Imported transit is already in the pulse graph; count only independent shaft loads here. */
    public static double independentStress(com.simibubi.create.content.kinetics.KineticNetwork network){
        double stress=network.calculateStress()-PulseOrbKinetics.importedStress(network);
        for(var member:java.util.List.copyOf(network.members.keySet())){
            if(!(member.getLevel() instanceof net.minecraft.server.level.ServerLevel level))continue;
            boolean paid=false;
            for(var source:network.sources.keySet())if(nativeSource(source)&&com.mcmagic.omnira.pulse.PulseNetworks.covered(level,source.getBlockPos(),output(source),member.getBlockPos())){paid=true;break;}
            if(paid)stress-=network.getActualStressOf(member);
        }
        return Math.max(0,stress);
    }
    public static double externalLoad(net.minecraft.world.level.block.entity.BlockEntity source){
        if(!nativeSource(source)||!(source instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity kinetic)||!kinetic.hasNetwork())return 0;
        var network=kinetic.getOrCreateNetwork();double nativeCapacity=0,foreign=0;
        for(var generator:java.util.List.copyOf(network.sources.keySet())){
            if(nativeSource(generator))nativeCapacity+=network.getActualCapacityOf(generator);
            else if(!(generator instanceof PulseOrbKinetics.Output))foreign+=network.getActualCapacityOf(generator);
        }
        if(nativeCapacity<=0||!network.sources.containsKey(kinetic))return 0;
        double demand=Math.min(nativeCapacity,Math.max(0,independentStress(network)-foreign));
        return demand*network.getActualCapacityOf(kinetic)/nativeCapacity;
    }
    private PulseKinetics(){}
}

package com.mcmagic.omnira.pulse;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;

/** The patch always keeps the source's pulse presentation; conversion happens at the remote face. */
public final class PulseOutput {
    public static final int PULSE=0,ROTATION=1,ELECTRIC=2;
    public static boolean nativeConnection(Level level,BlockPos source,Direction face){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server))return false;
        var target=source.relative(face);var side=face.getOpposite();
        if(!level.hasChunkAt(target))return false;
        var port=com.mcmagic.omnira.space.SpacetimePortState.at(level,target);
        if(port!=null){
            if(port.face()!=face)return false;
            var remote=port.other();if(remote==null)return false;
            target=remote.attached();side=remote.face();
        }
        return level.hasChunkAt(target)&&(level.getBlockState(target).is(PulseContent.WIRE.get())||PulseDevices.at(server,target,side)!=null);
    }
    public static int mode(Level level,BlockPos source,Direction face){
        if(level==null||level.isClientSide)return PULSE;var target=source.relative(face);
        if(!level.hasChunkAt(target)||com.mcmagic.omnira.space.SpacetimePortState.at(level,target)!=null)return PULSE;
        return receivingMode(level,target,face.getOpposite());
    }
    /** Port visuals stay native, but a remote external receiver still reserves this output. */
    public static int consumptionMode(Level level,BlockPos source,Direction face){
        if(level==null||level.isClientSide)return PULSE;var target=source.relative(face);
        if(!level.hasChunkAt(target))return PULSE;
        var port=com.mcmagic.omnira.space.SpacetimePortState.at(level,target);
        if(port==null)return receivingMode(level,target,face.getOpposite());
        var remote=port.face()==face?port.other():null;
        return remote==null?PULSE:receivingMode(level,remote.attached(),remote.face());
    }
    private static int receivingMode(Level level,BlockPos target,Direction side){
        if(!level.hasChunkAt(target))return PULSE;
        if(level instanceof net.minecraft.server.level.ServerLevel server&&PulseDevices.at(server,target,side)!=null)return PULSE;
        if(net.neoforged.fml.ModList.get().isLoaded("create")&&com.mcmagic.omnira.compat.create.PulseKinetics.connects(level,target,side))return ROTATION;
        var energy=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,target,side);
        return energy!=null&&energy.canReceive()?ELECTRIC:PULSE;
    }
    private PulseOutput(){}
}

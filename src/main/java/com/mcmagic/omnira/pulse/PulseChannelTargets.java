package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.space.SpacetimePortState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Shared hit classification; authoritative roots are resolved only on the server. */
public final class PulseChannelTargets {
    public record Target(BlockPos root,BlockPos inlet,Direction face){}
    public static boolean accepts(Level level,BlockPos pos,Direction face){
        if(!level.hasChunkAt(pos))return false;
        var port=SpacetimePortState.at(level,pos);
        if(port!=null){
            var remote=port.accessible(face)?port.other():null;
            return remote!=null&&nativeReceiver(level,remote.attached(),remote.face());
        }
        return nativeReceiver(level,pos,face);
    }
    private static boolean nativeReceiver(Level level,BlockPos pos,Direction face){
        if(!level.hasChunkAt(pos))return false;
        var state=level.getBlockState(pos);var entity=level.getBlockEntity(pos);
        if(state.is(PulseContent.WIRE.get())||entity instanceof PulseOrbHost)return true;
        if(entity instanceof com.mcmagic.omnira.block.entity.AssemblyAccess
                ||state.getBlock() instanceof com.mcmagic.omnira.workstation.CrystalWorkstationBlock
                ||state.is(com.mcmagic.omnira.registry.ModBlocks.REACTION_POOL.get()))
            return PulseVisualConnections.connects(level,pos,face);
        return false;
    }
    static Target resolve(ServerLevel level,BlockPos pos,Direction face){
        if(!accepts(level,pos,face))return null;
        var port=SpacetimePortState.at(level,pos);
        if(port!=null){var remote=port.other();if(remote==null)return null;pos=remote.attached();face=remote.face();}
        if(level.getBlockState(pos).is(PulseContent.WIRE.get()))return new Target(pos.immutable(),pos.immutable(),face);
        var device=PulseDevices.at(level,pos,face);
        return device!=null&&(device.base()>0||device.throughput()>0)?new Target(device.root().immutable(),pos.immutable(),face):null;
    }
    private PulseChannelTargets(){}
}

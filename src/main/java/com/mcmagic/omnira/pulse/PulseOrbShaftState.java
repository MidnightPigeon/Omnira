package com.mcmagic.omnira.pulse;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Server-selected socket presentation: two mode bits per face, then six presence bits. */
public final class PulseOrbShaftState {
    public static boolean connected(int state,Direction face){return (state&(1<<(12+face.ordinal())))!=0;}
    public static int mode(int state,Direction face){return state>>(face.ordinal()*2)&3;}
    public static boolean incoming(int state,Direction face){return (state&(1<<(18+face.ordinal())))!=0;}
    public static boolean outgoing(int state,Direction face){return (state&(1<<(24+face.ordinal())))!=0;}
    public static int capture(PulseOrbEntity orb){
        var level=orb.getLevel();int result=0,flow=PulseNetworks.visualFlow(orb);boolean create=net.neoforged.fml.ModList.get().isLoaded("create");
        for(var face:Direction.values()){
            var pos=orb.getBlockPos().relative(face);if(!level.hasChunkAt(pos))continue;
            int mode=PulseOutput.PULSE;boolean nativePulse=PulseOutput.nativeConnection(level,orb.getBlockPos(),face),connected=nativePulse;
            if(!connected&&create){
                connected=face==orb.inlet()?com.mcmagic.omnira.compat.create.PulseOrbKinetics.inputShaft(orb)!=null
                        :com.mcmagic.omnira.compat.create.PulseKinetics.connects(level,pos,face.getOpposite());
                if(connected)mode=PulseOutput.ROTATION;
            }
            if(!connected){
                var energy=level.getCapability(Capabilities.EnergyStorage.BLOCK,pos,face.getOpposite());
                connected=face==orb.inlet()?orb.conversion.imported(face)>0:energy!=null&&energy.canReceive();
                if(connected)mode=PulseOutput.ELECTRIC;
            }
            if(connected){
                if(com.mcmagic.omnira.space.SpacetimePortState.at(level,pos)!=null)mode=PulseOutput.PULSE;
                result|=1<<(12+face.ordinal())|mode<<(2*face.ordinal());
                if(nativePulse){
                    if((flow&(1<<face.ordinal()))!=0)result|=1<<(18+face.ordinal());
                    if((flow&(1<<(6+face.ordinal())))!=0)result|=1<<(24+face.ordinal());
                }else result|=1<<((face==orb.inlet()?18:24)+face.ordinal());
            }
        }return result;
    }
    private PulseOrbShaftState(){}
}

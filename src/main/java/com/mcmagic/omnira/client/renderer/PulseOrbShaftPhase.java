package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.pulse.*;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Optional client linkage: the isolated input uses its neighbour's phase, never the relay output speed. */
public final class PulseOrbShaftPhase {
    private static KineticBlockEntity neighbour(PulseOrbEntity orb,Direction face){
        var level=orb.getLevel();var pos=orb.getBlockPos().relative(face);var side=face.getOpposite();
        if(level==null||!level.hasChunkAt(pos)||PulseOutput.nativeConnection(level,orb.getBlockPos(),face))return null;
        var port=com.mcmagic.omnira.space.SpacetimePortState.at(level,pos);
        if(port!=null){var other=port.other();if(port.face()!=face||other==null)return null;pos=other.attached();side=other.face();}
        if(!level.hasChunkAt(pos))return null;var state=level.getBlockState(pos);
        return state.getBlock() instanceof IRotate rotate&&rotate.hasShaftTowards(level,pos,state,side)
                &&level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic?kinetic:null;
    }
    public static boolean connected(PulseOrbEntity orb,Direction face){return neighbour(orb,face)!=null;}
    public static float degrees(PulseOrbEntity orb,Direction face){
        var input=neighbour(orb,face);if(input==null)return 0;
        BlockEntity source=face==orb.inlet()?input:orb.host;
        if(!(source instanceof KineticBlockEntity kinetic)||kinetic.getSpeed()==0)return 0;
        float world=(float)Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(kinetic,source.getBlockPos(),face.getAxis()));
        return world*(face.getOpposite().getAxisDirection()==Direction.AxisDirection.POSITIVE?1:-1);
    }
    private PulseOrbShaftPhase(){}
}

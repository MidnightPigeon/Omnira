package com.mcmagic.omnira.space.sea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;

public final class StarflowFishing {
    public static int emit(ServerLevel level,ParticleOptions original,double x,double y,double z,int count,double dx,double dy,double dz,double speed){
        BlockPos pos=BlockPos.containing(x,y-.01,z);if(!VoidSeaContent.fluid(level.getFluidState(pos)))pos=pos.below();
        var fluid=level.getFluidState(pos);if(!VoidSeaContent.fluid(fluid))return 0;
        boolean bubble=original==ParticleTypes.BUBBLE;
        return level.sendParticles(bubble?com.mcmagic.omnira.registry.ModParticles.STARFLOW_BUBBLE.get():com.mcmagic.omnira.registry.ModParticles.STARFLOW_RIPPLE.get(),x,pos.getY()+fluid.getHeight(level,pos)+.025,z,count,dx,bubble?dy:0,dz,Math.min(speed,.04));
    }
    private StarflowFishing(){}
}

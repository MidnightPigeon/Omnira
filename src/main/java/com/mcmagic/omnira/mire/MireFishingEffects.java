package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Visual adaptation only: fishing timers, luck and bite windows remain vanilla. */
public final class MireFishingEffects {
    public static boolean surface(BlockState state){return state.is(Blocks.WATER)||MireContent.fluid(state.getFluidState());}
    public static ParticleOptions particle(ParticleOptions original){
        return original==ParticleTypes.BUBBLE?ModParticles.TIMEFLOW_BUBBLE.get():ModParticles.TIMEFLOW_RIPPLE.get();
    }
    public static int emit(ServerLevel level,ParticleOptions original,double x,double y,double z,int count,double dx,double dy,double dz,double speed){
        var p=BlockPos.containing(x,y-.01,z);
        if(!MireContent.fluid(level.getFluidState(p)))p=p.below();
        var fluid=level.getFluidState(p);
        if(!MireContent.fluid(fluid))return 0;
        double surface=p.getY()+fluid.getHeight(level,p)+.025;
        boolean bubble=original==ParticleTypes.BUBBLE;
        // Wake velocity stays tangent to the surface; bite rings do not leap like water splashes.
        return level.sendParticles(particle(original),x,surface,z,count,dx,bubble?dy:0,dz,bubble?Math.min(speed,.04):Math.min(speed,.1));
    }
    private MireFishingEffects(){}
}

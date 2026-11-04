package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;

public final class StarflowFishing {
    public static int emit(ServerLevel level,ParticleOptions original,double x,double y,double z,int count,double dx,double dy,double dz,double speed){
        BlockPos pos=BlockPos.containing(x,y-.01,z);if(!VoidSeaContent.fluid(level.getFluidState(pos)))pos=pos.below();
        var fluid=level.getFluidState(pos);if(!VoidSeaContent.fluid(fluid))return 0;
        boolean bubble=original==ParticleTypes.BUBBLE;
        var particle=(original==ParticleTypes.SPLASH || bubble)?ModParticles.STARFLOW_FOAM.get():ModParticles.STARFLOW_RIPPLE.get();
        return level.sendParticles(particle,x,pos.getY()+fluid.getHeight(level,pos)+(original==ParticleTypes.SPLASH || bubble?-.075:.025),z,count,dx,bubble?dy:0,dz,Math.min(speed,.04));
    }
    /** Small local cues remain visible to the angler outside the normal particle packet radius. */
    public static void showPhase(net.minecraft.world.entity.projectile.FishingHook hook,int phase,boolean star,int biteAge){
        if(phase==0)return;
        int tick=hook.tickCount;
        boolean sparkle=star && tick%8==0;
        boolean ripple=phase==3?(biteAge-1)%12==0:tick%(phase==2?5:16)==0;
        if(!sparkle && !ripple)return;
        var level=hook.level();
        var pos=hook.blockPosition();
        var fluid=level.getFluidState(pos);
        if(fluid.isEmpty()){pos=pos.below();fluid=level.getFluidState(pos);}
        if(fluid.isEmpty())return;
        // The bite pulls the hook under the surface; do not draw rings against a submerged block face.
        if(level.getFluidState(pos.above()).getType().isSame(fluid.getType())){
            pos=pos.above();
            fluid=level.getFluidState(pos);
        }
        double x=hook.getX(),z=hook.getZ(),y=pos.getY()+fluid.getHeight(level,pos)+.06;
        if(Math.abs(hook.getY()-y)>1.5)return;
        boolean spatial=VoidSeaContent.fluid(fluid),temporal=com.mcmagic.omnira.mire.MireContent.fluid(fluid);
        double angle=tick*.19+hook.getId();
        if(sparkle)level.addParticle(ParticleTypes.END_ROD,true,x+.22*Math.cos(angle),y+.12,z+.22*Math.sin(angle),0,.012,0);
        if(!ripple || !(spatial || temporal))return;
        var foam=spatial?ModParticles.STARFLOW_FOAM.get():ModParticles.TIMEFLOW_FOAM.get();
        if(phase==1){
            level.addParticle(foam,true,x+.36*Math.cos(angle),y-.14,z+.36*Math.sin(angle),0,0,0);
            return;
        }
        boolean bite=phase==3;
        var ring=bite?(spatial?ModParticles.STARFLOW_BITE_RING.get():ModParticles.TIMEFLOW_BITE_RING.get())
                :(spatial?ModParticles.STARFLOW_RIPPLE.get():ModParticles.TIMEFLOW_RIPPLE.get());
        level.addParticle(ring,true,x,y,z,0,0,0);
        int count=bite?5:2;
        for(int i=0;i<count;i++){
            double a=angle+i*2*Math.PI/count;
            level.addParticle(foam,true,x+.25*Math.cos(a),y-.12,z+.25*Math.sin(a),.018*Math.cos(a),bite?.10:.025,.018*Math.sin(a));
        }
    }
    private StarflowFishing(){}
}

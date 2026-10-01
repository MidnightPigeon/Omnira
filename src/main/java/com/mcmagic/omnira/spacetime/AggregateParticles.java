package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class AggregateParticles {
    private AggregateParticles(){}
    public static void emit(ServerLevel level,Vec3 center,boolean inward){
        for(int i=0;i<2;i++){
            double y=level.random.nextDouble()*2-1,angle=level.random.nextDouble()*Math.PI*2,r=Math.sqrt(1-y*y);
            var direction=new Vec3(Math.cos(angle)*r,y,Math.sin(angle)*r);
            var point=center.add(direction.scale(inward?1.05:.2));var motion=direction.scale(inward?-.045:.035);
            level.sendParticles(ModParticles.PARADOX_SPARK.get(),point.x,point.y,point.z,0,motion.x,motion.y,motion.z,1);
        }
    }
}

package com.mcmagic.omnira.world.dimension;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class TransitionParticles {
    private static final DustParticleOptions DUST=new DustParticleOptions(new Vector3f(.68F,.8F,1),1.1F);
    private TransitionParticles(){}
    private static final java.util.Map<net.minecraft.world.entity.Entity,Integer> ARRIVALS=new java.util.WeakHashMap<>();
    public static void arrive(net.minecraft.world.entity.Entity entity){ARRIVALS.put(entity,0);}
    @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        var iterator=ARRIVALS.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var entity=entry.getKey();int age=entry.getValue()+1;
            if(entity.isRemoved() || age>20){iterator.remove();continue;}
            entry.setValue(age);
            if(age%4==0 && !entity.isPassenger() && entity.level() instanceof ServerLevel level)
                ring(level,entity.position(),Math.max(.6,entity.getBbWidth()*.6)*age/20.0,entity.getBbHeight(),1-age/20.0);
        }
    }
    @net.neoforged.bus.api.SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event){ARRIVALS.clear();}
    public static void ring(ServerLevel level,Vec3 bottom,double radius,double height,double progress){
        for(int i=0;i<16;i++){
            double angle=i*Math.PI/8+progress*Math.PI*2;
            level.sendParticles(DUST,bottom.x+Math.cos(angle)*radius,bottom.y+height*progress,
                    bottom.z+Math.sin(angle)*radius,1,.025,.025,.025,0);
        }
    }
}

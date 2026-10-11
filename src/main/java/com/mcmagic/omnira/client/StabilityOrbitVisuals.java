package com.mcmagic.omnira.client;

import java.util.LinkedHashMap;
import com.mcmagic.omnira.space.anchor.StabilityVisualPayload;
import com.mcmagic.omnira.client.renderer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class StabilityOrbitVisuals {
    private record Entry(float radius,long seen){}
    private static final LinkedHashMap<Vec3,Entry> ORBITS=new LinkedHashMap<>();
    private static Object world;
    @EventBusSubscriber(modid="omnira",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Setup {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){
        event.enqueueWork(()->StabilityVisualPayload.receive=list->{
            var level=Minecraft.getInstance().level;if(level==null)return;
            if(world!=level){ORBITS.clear();world=level;}
            for(var o:list){ORBITS.remove(o.center());ORBITS.put(o.center(),new Entry(o.radius(),level.getGameTime()));}
            while(ORBITS.size()>256)ORBITS.remove(ORBITS.keySet().iterator().next());
        });
    }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();var level=mc.level;if(world!=level){ORBITS.clear();world=level;}
        if(level==null)return;long tick=level.getGameTime();double time=tick+event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();var it=ORBITS.entrySet().iterator();int count=0;
        while(it.hasNext()){
            var entry=it.next();var point=entry.getKey();var orbit=entry.getValue();double d=point.distanceToSqr(camera);
            if(tick-orbit.seen>20||d>32*32){it.remove();continue;}
            if(count>=128||!event.getFrustum().isVisible(AABB.ofSize(point,orbit.radius*2+1,orbit.radius*2+1,orbit.radius*2+1)))continue;
            count++;pose.pushPose();pose.translate(point.x-camera.x,point.y-camera.y,point.z-camera.z);
            int motes=d>256?1:3;
            for(int i=0;i<motes;i++){
                double a=time*.12+i*Math.PI*2/3;
                StellarVisuals.cube(pose,mc.renderBuffers().bufferSource(),(float)Math.cos(a)*orbit.radius,(float)Math.sin(a*2)*orbit.radius*.4F,(float)Math.sin(a)*orbit.radius,.025F,0x8CE6FF,.8F);
            }
            pose.popPose();
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){ORBITS.clear();world=null;}
    private StabilityOrbitVisuals(){}
}

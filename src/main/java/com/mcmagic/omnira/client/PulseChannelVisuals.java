package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.client.renderer.PlayerPulseRingMotion;
import com.mcmagic.omnira.pulse.PulseChanneling;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Tracking-only ring meshes use the existing pulse atlas and globally sorted glass buffer. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class PulseChannelVisuals {
    private record Visual(long started,long received,boolean active){}
    private static final Map<UUID,Visual> PLAYERS=new HashMap<>();
    private static ClientLevel world;
    public static void receive(UUID player,int elapsed){
        var level=Minecraft.getInstance().level;if(level==null)return;
        if(world!=level){PLAYERS.clear();world=level;}
        if(elapsed<0){PLAYERS.remove(player);return;}
        elapsed=Math.min(PulseChanneling.CHARGE_TICKS,elapsed);
        PLAYERS.put(player,new Visual(level.getGameTime()-elapsed,level.getGameTime(),elapsed>=PulseChanneling.CHARGE_TICKS));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();var level=mc.level;
        if(world!=level){PLAYERS.clear();world=level;}
        if(level==null||PLAYERS.isEmpty())return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);double time=level.getGameTime()+partial;
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();
        var out=mc.renderBuffers().bufferSource().getBuffer(CrystalGlassLayer.PULSE_RING);
        var it=PLAYERS.entrySet().iterator();
        while(it.hasNext()){
            var entry=it.next();var player=level.getPlayerByUUID(entry.getKey());var visual=entry.getValue();
            if(player==null){if(level.getGameTime()-visual.received>200)it.remove();continue;}
            if(!player.isAlive()||player.isSpectator()){it.remove();continue;}
            var position=player.getPosition(partial);if(position.distanceToSqr(camera)>64*64||!event.getFrustum().isVisible(new AABB(position,position).inflate(1,2,1)))continue;
            pose.pushPose();pose.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);
            float height=player.getBbHeight(),roll=com.mcmagic.omnira.space.abyss.GravityTransition.roll(player,partial);
            pose.translate(0,height/2,0);pose.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot)));
            if(roll!=0)pose.mulPose(Axis.ZP.rotationDegrees(roll));pose.translate(0,-height/2,0);
            double elapsed=Math.max(0,time-visual.started);float radius=Math.max(.5F,player.getBbWidth()*.9F);
            for(int ring=0;ring<PlayerPulseRingMotion.RINGS;ring++){
                var frame=PlayerPulseRingMotion.at(elapsed,ring,visual.active);if(frame.alpha()<=0)continue;
                pose.pushPose();pose.translate(0,height*frame.height(),0);pose.mulPose(Axis.YP.rotationDegrees((float)(time*(ring%2==0?1:-1)+ring*17)));
                for(int corner=0;corner<4;corner++){
                    float u=corner>=2?1:0,v=corner==1||corner==2?1:0;
                    out.addVertex(pose.last(),(u-.5F)*radius*2,0,(v-.5F)*radius*2)
                            .setColor(0x81,0xD4,0xFA,(int)(frame.alpha()*255)).setUv(u,v)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),0,1,0);
                }
                pose.popPose();
            }
            pose.popPose();
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){PLAYERS.clear();world=null;}
    private PulseChannelVisuals(){}
}

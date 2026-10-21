package com.mcmagic.omnira.client;

import com.mcmagic.omnira.weaver.WeaverVisualPayload;
import com.mcmagic.omnira.spell.visual.SpellVolumeGeometry;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class WeaverVisuals {
    private record Effect(net.minecraft.client.multiplayer.ClientLevel level,WeaverVisualPayload data,long born){}
    private static final List<Effect> EFFECTS=new ArrayList<>();
    public static void receive(WeaverVisualPayload data){
        var level=Minecraft.getInstance().level;if(level==null||data.kind()<0||data.kind()>3||!Float.isFinite(data.radius())||data.radius()<0||data.radius()>15)return;
        if(EFFECTS.size()>=48)EFFECTS.removeFirst();EFFECTS.add(new Effect(level,data,level.getGameTime()));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null){EFFECTS.clear();return;}
        EFFECTS.removeIf(e->e.level()!=mc.level||mc.level.getGameTime()-e.born()>=12);
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();
        var out=mc.renderBuffers().bufferSource().getBuffer(CrystalGlassLayer.COLOR);
        for(var effect:EFFECTS){
            float progress=(mc.level.getGameTime()-effect.born()+event.getPartialTick().getGameTimeDeltaPartialTick(false))/12F;
            var data=effect.data();pose.pushPose();pose.translate(data.from().x-camera.x,data.from().y-camera.y,data.from().z-camera.z);
            if(data.kind()==2)beam(out,pose,data.to().subtract(data.from()),1-progress);
            else{
                float radius=Math.max(.01F,(data.kind()==0?progress:1-progress)*data.radius());
                if(data.kind()==3)radius=Math.max(.01F,progress*data.radius());
                int color=data.kind()==1?0xBAF5CA:0xACDEFF;
                volume(out,pose,SpellVolumeGeometry.ball(),radius,color,(data.kind()==3?.15F:.09F)*(1-progress));
                for(int axis=0;axis<3;axis++){
                    pose.pushPose();if(axis==1)pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));if(axis==2)pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
                    volume(out,pose,data.kind()==3?SpellVolumeGeometry.ring():SpellVolumeGeometry.thinRing(),radius,color,(data.kind()==3?(axis==0?.85F:.65F):.7F)*(1-progress));pose.popPose();
                }
            }
            pose.popPose();
        }
    }
    public static void volume(VertexConsumer out,PoseStack pose,List<SpellVolumeGeometry.Quad> mesh,float radius,int color,float alpha){
        for(var q:mesh)for(var v:q.vertices())out.addVertex(pose.last().pose(),v[0]*radius,v[1]*radius,v[2]*radius).setColor(color>>16&255,color>>8&255,color&255,(int)(255*alpha));
    }
    private static void beam(VertexConsumer out,PoseStack pose,Vec3 end,float alpha){
        var forward=end.normalize();var right=forward.cross(Math.abs(forward.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(.045);
        var up=right.cross(forward).normalize().scale(.045);
        Vec3[] corners={right.add(up),right.subtract(up),right.scale(-1).subtract(up),right.scale(-1).add(up)};
        for(int i=0;i<4;i++)for(var v:List.of(corners[i],corners[(i+1)%4],corners[(i+1)%4].add(end),corners[i].add(end)))
            out.addVertex(pose.last().pose(),(float)v.x,(float)v.y,(float)v.z).setColor(255,255,255,(int)(alpha*230));
    }
    private WeaverVisuals(){}
}

package com.mcmagic.omnira.client;

import com.mcmagic.omnira.visual.*;
import com.mcmagic.omnira.spell.visual.SpellVolumeGeometry;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class MachineRangeVisuals {
    private record Effect(RangePreviewPayload data,long born,List<SpellVolumeGeometry.Quad> ring){}
    private static final LinkedHashMap<BlockPos,Effect> ACTIVE=new LinkedHashMap<>();
    private static Object world;
    @EventBusSubscriber(modid="omnira",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->RangePreviewPayload.receive=MachineRangeVisuals::receive);}
    }
    private static void receive(RangePreviewPayload p){
        var level=Minecraft.getInstance().level;if(level==null||p.radius()<1||p.radius()>64||p.chunk()&&p.radius()!=8)return;
        if(world!=level){ACTIVE.clear();world=level;}
        ACTIVE.remove(p.source());ACTIVE.put(p.source(),new Effect(p,level.getGameTime(),p.chunk()?List.of():SpellVolumeGeometry.rangeRing(p.radius(),1)));
        while(ACTIVE.size()>16)ACTIVE.remove(ACTIVE.keySet().iterator().next());
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGH)
    public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var mc=Minecraft.getInstance();if(world!=mc.level){ACTIVE.clear();world=mc.level;}if(mc.level==null||ACTIVE.isEmpty())return;
        var out=mc.renderBuffers().bufferSource().getBuffer(CrystalGlassLayer.COLOR);var pose=event.getPoseStack();var camera=event.getCamera().getPosition();
        var iterator=ACTIVE.values().iterator();
        while(iterator.hasNext()){
            var effect=iterator.next();var p=effect.data();float age=mc.level.getGameTime()-effect.born()+event.getPartialTick().getGameTimeDeltaPartialTick(false);
            float alpha=MachineRangePreview.alpha(age,p.radius());if(alpha<=0){iterator.remove();continue;}
            float radius=MachineRangePreview.extent(age,p.radius());if(radius<=0)continue;
            Vec3 center=p.chunk()?new Vec3((p.source().getX()>>4)*16+8,p.source().getY()+.5,(p.source().getZ()>>4)*16+8):p.source().getCenter();
            var bounds=p.chunk()?new net.minecraft.world.phys.AABB(center.x-8,mc.level.getMinBuildHeight(),center.z-8,center.x+8,mc.level.getMaxBuildHeight(),center.z+8):net.minecraft.world.phys.AABB.ofSize(center,p.radius()*2+2,p.radius()*2+2,p.radius()*2+2);
            if(!event.getFrustum().isVisible(bounds))continue;
            pose.pushPose();pose.translate(center.x-camera.x,center.y-camera.y,center.z-camera.z);
            if(p.chunk())walls(out,pose,radius,(float)(mc.level.getMinBuildHeight()-center.y),(float)(mc.level.getMaxBuildHeight()-center.y),alpha);
            else for(int axis=0;axis<3;axis++){
                pose.pushPose();if(axis==1)pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));if(axis==2)pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
                WeaverVisuals.volume(out,pose,effect.ring(),radius,0xACDEFF,.42F*alpha);pose.popPose();
            }
            pose.popPose();
        }
    }
    private static void quad(VertexConsumer out,PoseStack pose,float x1,float z1,float x2,float z2,float low,float high,float alpha,float bottom,float top){
        for(float[] v:new float[][]{{x1,low,z1},{x2,low,z2},{x2,high,z2},{x1,high,z1}})
            out.addVertex(pose.last().pose(),v[0],v[1],v[2]).setColor(172,222,255,(int)(255*alpha*MachineRangePreview.verticalAlpha(v[1],bottom,top)));
    }
    private static void walls(VertexConsumer out,PoseStack pose,float r,float low,float high,float alpha){
        quad(out,pose,-r,-r,r,-r,low,high,.04F*alpha,low,high);quad(out,pose,r,-r,r,r,low,high,.04F*alpha,low,high);
        quad(out,pose,r,r,-r,r,low,high,.04F*alpha,low,high);quad(out,pose,-r,r,-r,-r,low,high,.04F*alpha,low,high);
        // Four square corner pillars, plus sparse one-block horizontal marks.
        float w=.125F;
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            float a=x*r,b=z*r;quad(out,pose,a-w,b-w,a+w,b-w,low,high,.55F*alpha,low,high);quad(out,pose,a+w,b-w,a+w,b+w,low,high,.55F*alpha,low,high);
            quad(out,pose,a+w,b+w,a-w,b+w,low,high,.55F*alpha,low,high);quad(out,pose,a-w,b+w,a-w,b-w,low,high,.55F*alpha,low,high);
        }
        for(float y=(float)Math.ceil(low/4)*4;y<high;y+=4){
            quad(out,pose,-r,-r,r,-r,y,y+.0625F,.12F*alpha,low,high);quad(out,pose,r,-r,r,r,y,y+.0625F,.12F*alpha,low,high);
            quad(out,pose,r,r,-r,r,y,y+.0625F,.12F*alpha,low,high);quad(out,pose,-r,r,-r,-r,y,y+.0625F,.12F*alpha,low,high);
        }
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){ACTIVE.clear();world=null;}
}

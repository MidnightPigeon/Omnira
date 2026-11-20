package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.network.ChaosBeamState;
import com.mcmagic.omnira.spell.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ChaosBeamVisuals {
    private static final Map<UUID,ChaosBeamState> beams=new HashMap<>();
    // Third-person item poses are camera-relative. First-person items have a separate projection
    // and render after the world pass, so keep their projected tip for the next world frame.
    private record Tip(Vector3f point,boolean firstPerson,int frame){}
    private static final Map<UUID,Tip> tips=new HashMap<>();
    private static int frame;
    private static ClientLevel world;
    public static void captureTip(ItemStack stack,ItemDisplayContext context,PoseStack pose,float x,float y,float z){
        if(beams.isEmpty())return;
        boolean first=context.firstPerson();
        if(!first && context!=ItemDisplayContext.THIRD_PERSON_LEFT_HAND && context!=ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.level!=world)return;
        boolean left=context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        for(var state:beams.values()){
            var player=world.getPlayerByUUID(state.caster());
            if(player==null || (first && player!=mc.getCameraEntity()))continue;
            if(((player.getMainArm()==HumanoidArm.LEFT)==(state.hand()==InteractionHand.MAIN_HAND))!=left)continue;
            var held=player.getItemInHand(state.hand());
            if(first ? !held.is(stack.getItem()) : held!=stack)continue;
            var point=pose.last().pose().transformPosition(x,y,z,new Vector3f());
            if(first)new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).transformProject(point);
            tips.put(state.caster(),new Tip(point,first,frame));
            break;
        }
    }
    private static void useWorld(ClientLevel level){
        if(world!=level){beams.clear();tips.clear();world=level;}
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){useWorld(null);}
    public static void receive(ChaosBeamState state){
        useWorld(Minecraft.getInstance().level);
        if(state.range()<=0){beams.remove(state.caster());tips.remove(state.caster());}
        else if(world!=null)beams.put(state.caster(),state);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var mc=Minecraft.getInstance();
        useWorld(mc.level);
        int currentFrame=frame++;
        if(beams.isEmpty() || world==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera=event.getCamera().getPosition();
        var pose=event.getPoseStack();
        var out=mc.renderBuffers().bufferSource().getBuffer(CrystalGlassLayer.COLOR);
        var iter=beams.values().iterator();
        while(iter.hasNext()){
            var beam=iter.next();
            double age=world.getGameTime()-beam.pulseTick()+partial;
            if(age>40){iter.remove();tips.remove(beam.caster());continue;}
            var p=world.getPlayerByUUID(beam.caster());
            if(p==null || !p.isAlive())continue;
            var eye=p.getEyePosition(partial);
            var forward=p.getViewVector(partial);
            var hit=SpellCasting.target(p,eye,forward,beam.range(),true,true).getLocation();
            boolean first=mc.getCameraEntity()==p && mc.options.getCameraType().isFirstPerson();
            var tip=tips.get(beam.caster());
            if(tip==null || tip.firstPerson()!=first || tip.frame()!=currentFrame)continue;
            var point=new Vector3f(tip.point());
            if(first)new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert().transformProject(point);
            if(!point.isFinite())continue;
            var from=camera.add(point.x,point.y,point.z);
            var end=hit.subtract(from);
            var dir=end.normalize();
            if(end.lengthSqr()<1e-6)continue;
            int color=Mth.hsvToRgb((float)(((world.getGameTime()%120)+partial)/120.0),.42F,1);
            pose.pushPose();pose.translate(from.x-camera.x,from.y-camera.y,from.z-camera.z);
            sigil(out,pose,dir,color,(world.getGameTime()%160+partial)*Math.PI/80);
            WeaverVisuals.beam(out,pose,end,.9F,color);
            if(age>=0 && age<=ChaosBeam.TRAVEL_TICKS){
                var center=end.scale(age/ChaosBeam.TRAVEL_TICKS);
                var right=dir.cross(Math.abs(dir.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
                var up=right.cross(dir);
                for(int i=0;i<12;i++){
                    var a=radial(right,up,i);
                    var b=radial(right,up,i+1);
                    for(var v:List.of(a.scale(.14),a.scale(.24),b.scale(.24),b.scale(.14))){
                        var pt=center.add(v);
                        out.addVertex(pose.last().pose(),(float)pt.x,(float)pt.y,(float)pt.z).setColor(245,240,255,255);
                    }
                }
            }
            pose.popPose();
        }
    }
    private static void sigil(VertexConsumer out,PoseStack pose,Vec3 normal,int color,double phase){
        var right=normal.cross(Math.abs(normal.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        var up=right.cross(normal);
        // A narrow outer seal and four inward diamond marks leave the beam aperture clear.
        for(int i=0;i<16;i++){
            double a=phase+i*Math.PI/8,b=a+Math.PI/8;
            var u=right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            var v=right.scale(Math.cos(b)).add(up.scale(Math.sin(b)));
            quad(out,pose,color,u.scale(.125),u.scale(.137),v.scale(.137),v.scale(.125));
            if(i%4==0){
                var tangent=up.scale(Math.cos(a)).subtract(right.scale(Math.sin(a)));
                var mid=u.scale(.096);
                quad(out,pose,0xF1DEFF,u.scale(.068),mid.add(tangent.scale(.013)),u.scale(.122),mid.subtract(tangent.scale(.013)));
            }
        }
    }
    private static void quad(VertexConsumer out,PoseStack pose,int color,Vec3 a,Vec3 b,Vec3 c,Vec3 d){
        for(var point:List.of(a,b,c,d))out.addVertex(pose.last().pose(),(float)point.x,(float)point.y,(float)point.z)
                .setColor(color>>16&255,color>>8&255,color&255,240);
    }
    private static Vec3 radial(Vec3 right,Vec3 up,int index){
        double angle=index*Math.PI/6;
        return right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
    }
}

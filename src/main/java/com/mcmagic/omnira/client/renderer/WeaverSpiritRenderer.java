package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.entity.WeaverSpirit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class WeaverSpiritRenderer extends EntityRenderer<WeaverSpirit> {
    public static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/entity/weaver_spirit.png");
    public WeaverSpiritRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.8F;}
    @Override public void render(WeaverSpirit entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        float time=entity.motionClock(partial);
        float ambient=entity.edgeClock(partial),age=entity.departureAge(partial);int exit=entity.departure();
        pose.pushPose();pose.translate(0,1.5+(exit==2?com.mcmagic.omnira.weaver.WeaverDeparture.deathRise(age):entity.active()?Math.sin(time*.035)*.015:0),0);pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        int overlay=OverlayTexture.pack(0,entity.hurtTime>0&&exit==0);
        int spiritAlpha=(int)((105+Math.sin(time*.04)*12)*(entity.phasing()?.8:1));
        for(int pass=0;pass<2;pass++){
            if(pass==1)for(int i=0;i<7;i++){
                if(exit!=0&&age>32+i*8)continue;
                double angle=i*2.399+time*.015;pose.pushPose();
                pose.translate(-.65+.25*Math.cos(angle),.65*Math.sin(angle*.8),.72*Math.cos(angle*1.1));
                pose.mulPose(Axis.YP.rotationDegrees(yaw-180));
                WeaverRunes.draw(i*3,pose,buffers,.025F);pose.popPose();
            }
            var out=buffers.getBuffer(pass==0&&!entity.phasing()?RenderType.entityCutoutNoCull(TEXTURE):CrystalGlassLayer.WEAVER_SPIRIT);
            for(int group=0;group<8;group++){
                pose.pushPose();
                if(exit==1&&group!=0&&group!=2&&group!=7){
                    float loosen=com.mcmagic.omnira.weaver.WeaverDeparture.loosen(group,age);pose.translate(loosen*.22,loosen*.35,0);
                }
                if(group==2||group==7){float scale=com.mcmagic.omnira.weaver.WeaverDeparture.coreScale(exit,age);pose.scale(scale,scale,scale);}
                for(var gear:WeaverSpiritGeometry.GEARS)if(gear.group()==group){
                    pose.translate(gear.x()/16,gear.y()/16,gear.z()/16);
                    pose.mulPose((gear.axis()==0?Axis.XP:gear.axis()==1?Axis.YP:Axis.ZP).rotationDegrees(time*gear.speed()*2));
                    pose.translate(-gear.x()/16,-gear.y()/16,-gear.z()/16);
                }
                for(var face:WeaverSpiritGeometry.FACES){
                    boolean spirit=face.material()==1;
                    boolean fitting=group==0 && face.material()==3;
                    if(face.group()!=group||(spirit||fitting)!=(pass==1))continue;
                    var center=face.vertices();
                    if(!com.mcmagic.omnira.weaver.WeaverDeparture.showFace(exit,age,group,(center[0]+center[6])/2,(center[1]+center[7])/2,(center[2]+center[8])/2))continue;
                    int color=group==2&&entity.cloudNear()?0xFFC48AFF:group==2&&entity.redCore()?0xFFFF6878:group==7?0xFF8B969F:group==6?0xFF596970:0xFFFFFFFF;
                    if(fitting)color=0x8099DEE5;
                    else if(face.material()==3)color=0xFFFFFFFF;
                    if(entity.phasing()&&!fitting)color=(color&0xFFFFFF)|0xCC000000;
                    int brightness=face.material()==2?LightTexture.FULL_BRIGHT:light;
                    float[] vertices=face.vertices(),uv=face.uv();
                    for(int i=0;i<4;i++)out.addVertex(pose.last(),vertices[i*3]/16,vertices[i*3+1]/16,vertices[i*3+2]/16)
                            .setColor(spirit?((int)(spiritAlpha*WeaverSpiritGeometry.edgeOpacity(vertices[i*3]))<<24)|0xFFFFFF:color)
                            .setUv(uv[i*2],uv[i*2+1]).setOverlay(overlay).setLight(brightness)
                            .setNormal(pose.last(),face.nx(),face.ny(),face.nz());
                }
                pose.popPose();
            }
        }
        var particles=buffers.getBuffer(CrystalGlassLayer.COLOR);
        if(exit==0 || com.mcmagic.omnira.weaver.WeaverDeparture.showFace(exit,age,1,19,-5,0))
            driveLinkage(time,pose,particles);
        pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(ambient*WeaverSpiritGeometry.SEAM_SPEED));
        for(var mote:WeaverSpiritGeometry.SEAM_MOTES){
            if(exit!=0&&age>80)continue;
            pose.pushPose();pose.translate(mote.x()/16,mote.y()/16,mote.z()/16);pose.scale(mote.size()/16,mote.size()/16,mote.size()/16);
            for(var face:WeaverSpiritGeometry.SEAM_PIXEL)for(int i=0;i<4;i++){
                var v=face.vertices();particles.addVertex(pose.last().pose(),v[i*3],v[i*3+1],v[i*3+2])
                        .setColor(mote.color()>>16&255,mote.color()>>8&255,mote.color()&255,(int)(mote.alpha()*255));
            }
            pose.popPose();
        }
        pose.popPose();
        for(int i=0;i<9;i++){
            if(exit!=0&&age>50+i*5)continue;
            double a=i*2.4+time*.027;pose.pushPose();pose.translate(-.6+.28*Math.cos(a),.65*Math.sin(a*.7),.6*Math.cos(a*1.3));
            com.mcmagic.omnira.client.WeaverVisuals.volume(particles,pose,com.mcmagic.omnira.spell.visual.SpellVolumeGeometry.ball(),.035F,i%2==0?0xC3EDFF:0xFFFFFF,.8F);pose.popPose();
        }
        int[] colors={0x90E5AB,0x90CDF7,0xBF9BEB};
        if(exit==0)for(int i=0;i<entity.strength();i++)com.mcmagic.omnira.client.WeaverVisuals.volume(particles,pose,com.mcmagic.omnira.spell.visual.SpellVolumeGeometry.thinRing(),1.62F+i*.14F,colors[i],.45F);
        if(entity.cloudNear()&&(exit==0||age<100)){
            pose.pushPose();pose.scale(2.5F,2.5F,2.5F);com.mcmagic.omnira.client.ProbabilityCloudClient.draw(ambient,pose,buffers);pose.popPose();
        }
        if(exit==2)com.mcmagic.omnira.client.renderer.WeaverDepartureVisuals.draw(age,yaw,pose,buffers,light);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    // The eccentric shares the main wheel angle. The far pin slides along Z inside the fork.
    private static void driveLinkage(float time,PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer out){
        double angle=Math.toRadians(time*WeaverSpiritGeometry.GEARS.getFirst().speed()*2);
        double dy=2.5*Math.cos(angle), crankZ=2.5*Math.sin(angle);
        double reach=Math.sqrt(100-dy*dy);
        pose.pushPose();
        pose.scale(1f/16,1f/16,1f/16);
        pixelPart(pose,out,19,-5+dy,crankZ,2.2f,2,2,0,0xFFD1F6FF);
        float tilt=(float)Math.toDegrees(Math.atan2(dy,reach));
        pixelPart(pose,out,18.8,-5+dy/2,crankZ+reach/2,1.2f,1.4f,10,tilt,0xFFA3BCCA);
        pixelPart(pose,out,18.8,-5,crankZ+reach+2,1.6f,1.6f,5,0,0xFFA4D7DF);
        for(int i=0;i<2;i++){
            double travel=(time*.022+i*.5)%1;
            pixelPart(pose,out,8,-4.7,-11-6*travel,.8f,.8f,1.5f,0,0xC0ADF9FF);
            pixelPart(pose,out,8,6.3,11+6*travel,.8f,.8f,1.5f,0,0xC0ADF9FF);
        }
        pose.popPose();
    }
    private static void pixelPart(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer out,
                                  double x,double y,double z,float w,float h,float d,float tilt,int color){
        pose.pushPose();
        pose.translate(x,y,z);
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.scale(w,h,d);
        for(var face:WeaverSpiritGeometry.SEAM_PIXEL){
            var v=face.vertices();
            for(int i=0;i<4;i++)out.addVertex(pose.last().pose(),v[i*3],v[i*3+1],v[i*3+2]).setColor(color);
        }
        pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(WeaverSpirit entity){return TEXTURE;}
}

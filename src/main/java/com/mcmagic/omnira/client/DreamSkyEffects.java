package com.mcmagic.omnira.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.Random;

/** Replaces the entire vanilla sky pass, so neither sun nor moon is submitted. */
public final class DreamSkyEffects extends DimensionSpecialEffects {
    private record SkyVertex(float x,float y,float z,int color) {}
    private static final SkyVertex[] SKY=sky();
    private static final float[][] STARS=stars();
    public DreamSkyEffects() {super(Float.NaN,false,SkyType.NONE,false,false);}
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float brightness) {return new Vec3(.59,.64,.82);}
    @Override public boolean isFoggyAt(int x,int z) {return false;}
    @Override public float[] getSunriseColor(float time,float partial) {return null;}
    @Override public boolean renderClouds(ClientLevel level,int ticks,float partial,PoseStack pose,double x,double y,double z,Matrix4f view,Matrix4f projection) {return true;}
    @Override public boolean renderSky(ClientLevel level,int ticks,float partial,Matrix4f view,Camera camera,Matrix4f projection,boolean foggy,Runnable setupFog) {
        setupFog.run();
        if(foggy || camera.getFluidInCamera()==FogType.LAVA || camera.getFluidInCamera()==FogType.POWDER_SNOW) return true;
        if(camera.getEntity() instanceof net.minecraft.world.entity.LivingEntity living
                && (living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) || living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))) return true;
        var pose=new PoseStack();pose.mulPose(view);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1,1,1,1);
        FogRenderer.setupNoFog();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var background=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(var vertex:SKY) background.addVertex(pose.last().pose(),vertex.x(),vertex.y(),vertex.z()).setColor(vertex.color());
            BufferUploader.drawWithShader(background.buildOrThrow());
            pose.mulPose(Axis.YP.rotationDegrees((float)((level.getGameTime()+partial)*.0007%360)));
            var buffer=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(int star=0;star<STARS.length;star++) {
                int color=star%3==0?0xFFFFEFFF:0xFFF1FAFF;
                float[] points=STARS[star];
                for(int i=0;i<12;i+=3) buffer.addVertex(pose.last().pose(),points[i],points[i+1],points[i+2]).setColor(color);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.setShaderColor(1,1,1,1);setupFog.run();
        }
        return true;
    }
    private static SkyVertex[] sky() {
        var vertices=new SkyVertex[64*32*4];
        int index=0;
        for(int latitude=0;latitude<32;latitude++) for(int longitude=0;longitude<64;longitude++) {
            for(int corner=0;corner<4;corner++) {
                double elevation=-Math.PI/2+Math.PI*(latitude+(corner>=2?1:0))/32;
                double angle=2*Math.PI*(longitude+(corner==1 || corner==2?1:0))/64;
                double x=Math.cos(elevation)*Math.cos(angle),y=Math.sin(elevation),z=Math.cos(elevation)*Math.sin(angle);
                // Direction-based colors meet continuously across the sphere, including its seam.
                double blend=.5+.5*Math.sin(2.1*x+1.4*z+.8*y);
                double glow=Math.pow(1-Math.abs(y),2)*12;
                int r=(int)(130+48*blend+glow),g=(int)(181-23*blend+glow),b=(int)(224+12*blend+glow);
                vertices[index++]=new SkyVertex((float)(x*110),(float)(y*110),(float)(z*110),0xFF000000|r<<16|g<<8|b);
            }
        }
        return vertices;
    }
    private static float[][] stars() {
        var random=new Random(714923);
        float[][] stars=new float[650][12];
        for(float[] star:stars) {
            double y=random.nextDouble()*2-1,a=random.nextDouble()*Math.PI*2,r=Math.sqrt(1-y*y);
            Vec3 normal=new Vec3(r*Math.cos(a),y,r*Math.sin(a));
            Vec3 right=normal.cross(Math.abs(y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
            Vec3 up=normal.cross(right).normalize();
            double size=.09+random.nextDouble()*.16;
            for(int i=0;i<4;i++) {
                Vec3 point=normal.scale(90).add(right.scale((i==0 || i==1?-1:1)*size)).add(up.scale((i==0 || i==3?-1:1)*size));
                star[i*3]=(float)point.x;star[i*3+1]=(float)point.y;star[i*3+2]=(float)point.z;
            }
        }
        return stars;
    }
}

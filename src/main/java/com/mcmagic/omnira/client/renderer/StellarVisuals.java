package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

public final class StellarVisuals {
    private static final int[][] CORNERS={{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static int detail(PoseStack pose,int full){
        if(!WorldGlassBuffers.collecting())return full;
        var m=pose.last().pose();float x=m.m00()*.5F+m.m10()*.5F+m.m20()*.5F+m.m30();
        float y=m.m01()*.5F+m.m11()*.5F+m.m21()*.5F+m.m31(),z=m.m02()*.5F+m.m12()*.5F+m.m22()*.5F+m.m32();
        float d=x*x+y*y+z*z;return d>1024?full/4:d>256?full/2:full;
    }
    public static void cube(PoseStack pose,MultiBufferSource buffers,float x,float y,float z,float radius,int color,float alpha){
        cube(pose,buffers.getBuffer(CrystalGlassLayer.COLOR),x,y,z,radius,color,alpha);
    }
    public static void cube(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer mesh,float x,float y,float z,float radius,int color,float alpha){
        var matrix=pose.last().pose();
        for(var face:FACES)for(int n:face)mesh.addVertex(matrix,x+CORNERS[n][0]*radius,y+CORNERS[n][1]*radius,z+CORNERS[n][2]*radius)
            .setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha);
    }
    public static void cube(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer mesh,float x,float y,float z,float radius,int color){
        var matrix=pose.last().pose();
        for(var face:FACES)for(int n:face)mesh.addVertex(matrix,x+CORNERS[n][0]*radius,y+CORNERS[n][1]*radius,z+CORNERS[n][2]*radius)
            .setColor(color);
    }
    public static void cluster(PoseStack pose,MultiBufferSource buffers,double time){
        cluster(pose,buffers,time,false);
    }
    public static void cluster(PoseStack pose,MultiBufferSource buffers,double time,boolean white){
        int count=detail(pose,72),step=72/count;
        for(int i=0;i<72;i+=step){
            double phase=time*(.012+(i%7)*.001)+i*2.39996;
            double radius=.055+.21*Math.sqrt((i+.5)/72.0),latitude=Math.sin(i*1.73)*.85;
            float x=.5F+(float)(Math.cos(phase)*radius*Math.sqrt(1-latitude*latitude));
            float z=.5F+(float)(Math.sin(phase)*radius*Math.sqrt(1-latitude*latitude));
            float y=.5F+(float)(latitude*radius+Math.sin(phase*1.5)*.025);
            cube(pose,buffers,x,y,z,.006F+(i%4)*.0015F,white?0xFFFFFF:i%3==0?0xFFE8B8:i%3==1?0xD4EFFF:0xDCC9FF,.85F);
        }
    }
    public static void centeredItem(ItemStack item,PoseStack pose,MultiBufferSource buffers,int overlay){
        if(item.isEmpty())return;
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(.26F,.26F,.26F);
        Minecraft.getInstance().getItemRenderer().renderStatic(item,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,overlay,
            pose,CrystalGlassLayer.contents(buffers),Minecraft.getInstance().level,0);pose.popPose();
    }
    public static void baseStars(PoseStack pose,MultiBufferSource buffers,double time){
        baseStars(pose,buffers,time,false);
    }
    public static void baseStars(PoseStack pose,MultiBufferSource buffers,double time,boolean white){
        int count=detail(pose,12),step=12/count;
        for(int i=0;i<12;i+=step){
            double angle=i*Math.PI*2/12;
            float x=.5F+(float)Math.cos(angle)*.32F,z=.5F+(float)Math.sin(angle)*.32F;
            float y=.16F+(float)Math.sin(time*.055+i*1.7)*.025F;
            cube(pose,buffers,x,y,z,.009F,white?0xFFFFFF:i%2==0?0xC8E5FF:0xFFF2CC,.55F+.35F*(float)Math.sin(time*.08+i));
        }
    }
    private StellarVisuals(){}
}

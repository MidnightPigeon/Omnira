package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

public final class StellarVisuals {
    public static void cube(PoseStack pose,MultiBufferSource buffers,float x,float y,float z,float radius,int color,float alpha){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);var matrix=pose.last().pose();
        float[][] v={{x-radius,y-radius,z-radius},{x+radius,y-radius,z-radius},{x+radius,y+radius,z-radius},{x-radius,y+radius,z-radius},
            {x-radius,y-radius,z+radius},{x+radius,y-radius,z+radius},{x+radius,y+radius,z+radius},{x-radius,y+radius,z+radius}};
        int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
        for(var face:faces)for(int n:face)mesh.addVertex(matrix,v[n][0],v[n][1],v[n][2])
            .setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha);
    }
    public static void cluster(PoseStack pose,MultiBufferSource buffers,double time){
        for(int i=0;i<72;i++){
            double phase=time*(.012+(i%7)*.001)+i*2.39996;
            double radius=.055+.21*Math.sqrt((i+.5)/72.0),latitude=Math.sin(i*1.73)*.85;
            float x=.5F+(float)(Math.cos(phase)*radius*Math.sqrt(1-latitude*latitude));
            float z=.5F+(float)(Math.sin(phase)*radius*Math.sqrt(1-latitude*latitude));
            float y=.5F+(float)(latitude*radius+Math.sin(phase*1.5)*.025);
            cube(pose,buffers,x,y,z,.006F+(i%4)*.0015F,i%3==0?0xFFE8B8:i%3==1?0xD4EFFF:0xDCC9FF,.85F);
        }
    }
    public static void centeredItem(ItemStack item,PoseStack pose,MultiBufferSource buffers,int overlay){
        if(item.isEmpty())return;
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(.26F,.26F,.26F);
        Minecraft.getInstance().getItemRenderer().renderStatic(item,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,overlay,
            pose,CrystalGlassLayer.contents(buffers),Minecraft.getInstance().level,0);pose.popPose();
    }
    public static void baseStars(PoseStack pose,MultiBufferSource buffers,double time){
        for(int i=0;i<12;i++){
            double angle=i*Math.PI*2/12;
            float x=.5F+(float)Math.cos(angle)*.32F,z=.5F+(float)Math.sin(angle)*.32F;
            float y=.16F+(float)Math.sin(time*.055+i*1.7)*.025F;
            cube(pose,buffers,x,y,z,.009F,i%2==0?0xC8E5FF:0xFFF2CC,.55F+.35F*(float)Math.sin(time*.08+i));
        }
    }
    private StellarVisuals(){}
}

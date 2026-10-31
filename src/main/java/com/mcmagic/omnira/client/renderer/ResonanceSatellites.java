package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Frequency markers orbit outside the globe, using the same late glass batch as its contents. */
public final class ResonanceSatellites {
    private static final float[][] RING=ring();
    private ResonanceSatellites() {}
    private static float[][] ring(){
        var points=new java.util.LinkedHashSet<Long>();
        for(int i=0;i<160;i++){
            double a=i*Math.PI/80;
            int x=(int)Math.round(Math.cos(a)*20),z=(int)Math.round(Math.sin(a)*20);
            points.add(((long)x<<32)|(z&0xffffffffL));
        }
        float[][] result=new float[points.size()][2];int i=0;
        for(long point:points){result[i][0]=(int)(point>>32)/32F;result[i++][1]=(int)point/32F;}
        return result;
    }
    public static void render(ItemStack[] markers,PoseStack pose,MultiBufferSource buffers,double time,int overlay){
        if(markers[0].isEmpty()&&markers[1].isEmpty())return;
        boolean active=!markers[0].isEmpty()&&!markers[1].isEmpty();
        pose.pushPose();pose.translate(.5,.625,.5);pose.mulPose(Axis.ZP.rotationDegrees(16));
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(var point:RING)StellarVisuals.cube(pose,mesh,point[0],0,point[1],.016F,active?0xB38AE5:0x776389,active?.65F:.35F);
        var mc=Minecraft.getInstance();var contents=CrystalGlassLayer.contents(buffers);
        for(int i=0;i<2;i++){
            var item=markers[i];if(item.isEmpty())continue;
            double angle=time*.018+i*Math.PI;
            pose.pushPose();pose.translate(Math.cos(angle)*.625,0,Math.sin(angle)*.625);
            pose.mulPose(Axis.YP.rotation((float)-angle));pose.scale(.18F,.18F,.18F);
            var baked=mc.getItemRenderer().getModel(item,mc.level,null,i);
            UnstableAggregateRenderer.inStorage(()->mc.getItemRenderer().render(item,ItemDisplayContext.FIXED,false,pose,contents,LightTexture.FULL_BRIGHT,overlay,baked));
            pose.popPose();
        }
        pose.popPose();
    }
}

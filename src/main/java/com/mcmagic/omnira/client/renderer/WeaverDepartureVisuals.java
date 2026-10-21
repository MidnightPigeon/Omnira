package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Eight bounded waves of physical-looking debris; no item entities or persistent particle lists. */
public final class WeaverDepartureVisuals {
    private static final int[] CRYSTAL_COLORS={0xBCEAFF,0xB6A1F0,0xF0FCFF};
    public static void draw(float age,float yaw,PoseStack pose,MultiBufferSource buffers,int light){
        for(int wave=0;wave<8;wave++){
            float t=age-12-wave*12;if(t<0||t>=40)continue;float alpha=1-t/40;
            pose.pushPose();pose.translate(0,-com.mcmagic.omnira.weaver.WeaverDeparture.deathRise(t),0);
            for(int i=0;i<18;i++){
                double angle=i*2.399+wave*.83;float travel=t*.065F;
                pose.pushPose();pose.translate(Math.cos(angle)*travel,(i%5-1)*t*.025-t*t*.0008,Math.sin(angle)*travel);
                pose.mulPose(Axis.XP.rotationDegrees(t*(i%2==0?8:-6)));pose.mulPose(Axis.ZP.rotationDegrees(i*29+t*5));
                float scale=.6F*Math.min(1,(40-t)/8);
                pose.scale(scale,scale,scale);
                var out=buffers.getBuffer(CrystalGlassLayer.WEAVER_SPIRIT);
                for(var face:WeaverSpiritGeometry.DEBRIS.get((wave+i)%WeaverSpiritGeometry.DEBRIS.size()))for(int n=0;n<4;n++){
                    var v=face.vertices();var uv=face.uv();out.addVertex(pose.last(),v[n*3]/16,v[n*3+1]/16,v[n*3+2]/16)
                            .setColor(255,255,255,(int)(255*alpha)).setUv(uv[n*2],uv[n*2+1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),face.nx(),face.ny(),face.nz());
                }
                pose.popPose();
            }
            for(int i=0;i<12;i++){
                double angle=Math.PI/2+(i+.5)*Math.PI/12;
                pose.pushPose();pose.translate(Math.cos(angle)*(t*.08+.2),(i%5-2)*t*.028-t*t*.0006,Math.sin(angle)*t*.08);
                pose.mulPose(Axis.YP.rotationDegrees(i*37+t*7));pose.mulPose(Axis.ZP.rotationDegrees(i*23-t*4));
                float scale=.8F*Math.min(1,(40-t)/8);pose.scale(scale,scale,scale);
                int color=CRYSTAL_COLORS[(wave+i)%CRYSTAL_COLORS.length];
                var out=buffers.getBuffer(CrystalGlassLayer.WEAVER_SPIRIT);
                for(var face:WeaverSpiritGeometry.CRYSTAL_DEBRIS.get((wave+i)%WeaverSpiritGeometry.CRYSTAL_DEBRIS.size()))for(int n=0;n<4;n++){
                    float shade=.78F+.22F*Math.max(0,face.ny()*.8F-face.nx()*.3F-face.nz()*.5F);
                    var v=face.vertices();var uv=face.uv();out.addVertex(pose.last(),v[n*3]/16,v[n*3+1]/16,v[n*3+2]/16)
                            .setColor((int)((color>>16&255)*shade),(int)((color>>8&255)*shade),(int)((color&255)*shade),(int)(210*alpha)).setUv(uv[n*2],uv[n*2+1])
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),face.nx(),face.ny(),face.nz());
                }
                pose.popPose();
            }
            for(int i=0;i<12;i++){
                double angle=i*Math.PI/6+wave*.4;pose.pushPose();pose.translate(Math.cos(angle)*t*.08,(i%3-1)*t*.035,Math.sin(angle)*t*.08);
                pose.mulPose(Axis.YP.rotationDegrees(yaw-180));
                WeaverRunes.draw(wave*5+i,pose,buffers,.065F,alpha);pose.popPose();
            }
            pose.popPose();
        }
    }
    private WeaverDepartureVisuals(){}
}

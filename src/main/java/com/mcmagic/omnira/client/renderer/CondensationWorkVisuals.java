package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Bounded, three-dimensional pixel motes rising from the active table's central recess. */
final class CondensationWorkVisuals {
    private static final int COUNT=16;
    private static final double LIFE=28;
    static void render(double age,double emissionEnd,PoseStack pose,MultiBufferSource buffers){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<COUNT;i++){
            double offset=i*LIFE/COUNT;
            double latest=Math.min(age,emissionEnd)-offset;if(latest<0)continue;
            double born=offset+Math.floor(latest/LIFE)*LIFE;
            double elapsed=age-born;if(elapsed<0||elapsed>=LIFE)continue;
            double phase=elapsed/LIFE,angle=i*2.399963+phase*.35;
            double spread=.08+(i%4)*.043;
            float x=.5F+(float)(Math.cos(angle)*spread),z=.5F+(float)(Math.sin(angle)*spread);
            float y=.83F+(float)phase*.78F,r=.024F+(i%3)*.004F;
            float alpha=(float)(Math.min(1,phase*8)*Math.min(1,(1-phase)*5))*.92F;
            StellarVisuals.cube(pose,mesh,x,y,z,r,i%3==0?0xF0FFFF:i%3==1?0xA9EAF2:0xD3F7FF,alpha);
        }
    }
    private CondensationWorkVisuals(){}
}

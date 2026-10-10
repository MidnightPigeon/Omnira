package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.space.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;

public final class FoldGateRenderer implements BlockEntityRenderer<FoldGateBlockEntity> {
    public FoldGateRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(FoldGateBlockEntity gate,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(gate.getLevel()==null||!FoldGateBlock.active(gate.getBlockState()))return;
        pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(-gate.getBlockState().getValue(FoldGateBlock.FACING).toYRot()));
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);var m=pose.last().pose();
        double time=(gate.getLevel().getGameTime()+partial)*.06;boolean stable=FoldGateBlock.stable(gate.getBlockState());
        for(int x=0;x<12;x++)for(int y=0;y<30;y++){
            double phase=y*.45-time+(stable?Math.sin(x*.5)*.8:Math.sin(x*1.2+time*2)*2);
            float wave=(float)(.5+.5*Math.sin(phase));
            int r=(int)((stable?.25:.55)*160+wave*70),g=(int)(110+wave*(stable?110:40)),b=225;
            int alpha=(int)(45+wave*70);float x0=(x-6)/16F,x1=x0+1/16F,y0=y/16F,y1=y0+1/16F;
            mesh.addVertex(m,x0,y0,0).setColor(r,g,b,alpha);mesh.addVertex(m,x1,y0,0).setColor(r,g,b,alpha);
            mesh.addVertex(m,x1,y1,0).setColor(r,g,b,alpha);mesh.addVertex(m,x0,y1,0).setColor(r,g,b,alpha);
        }
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(FoldGateBlockEntity gate){return true;}
}

package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.DreamPortalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;

public final class DreamPortalRenderer implements BlockEntityRenderer<DreamPortalBlockEntity> {
    public DreamPortalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(DreamPortalBlockEntity portal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(portal.getLevel()==null) return;
        pose.pushPose();pose.translate(.5,.5,.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        float scale=portal.scale(partial);
        pose.scale(scale,scale,scale);
        swirl(pose,buffers,portal.getLevel().getGameTime()+partial);
        pose.popPose();
    }
    public static void swirl(PoseStack pose,MultiBufferSource buffers,double time) {
        var mesh=buffers.getBuffer(RenderType.debugQuads());
        var matrix=pose.last().pose();
        for(int x=-8;x<8;x++) for(int y=-8;y<8;y++) {
            double radius=Math.hypot(x+.5,y+.5);
            if(radius>7.8) continue;
            double wave=Math.sin(Math.atan2(y+.5,x+.5)*3-radius*.85+time*.11);
            float alpha=(float)((wave>.15?.78:.20)*Math.min(1,(8-radius)*1.6));
            int color=radius<1.6?0xF8F2FF:wave>.65?0xD6F7F4:wave>.15?0xAEA3DC:0x657D99;
            float a=x/8F,b=y/8F,c=(x+1)/8F,d=(y+1)/8F;
            mesh.addVertex(matrix,a,b,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
            mesh.addVertex(matrix,c,b,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
            mesh.addVertex(matrix,c,d,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
            mesh.addVertex(matrix,a,d,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
        }
    }
}

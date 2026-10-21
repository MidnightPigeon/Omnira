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
        swirl(pose,buffers,time,CrystalGlassLayer.COLOR);
    }
    public static void swirl(PoseStack pose,MultiBufferSource buffers,double time,RenderType layer) {
        var mesh=buffers.getBuffer(layer);
        var matrix=pose.last().pose();
        for(var cell:DreamPortalPattern.CELLS) {
            int x=cell.x(),y=cell.y(),color=DreamPortalPattern.color(cell,time);float alpha=(color>>>24)/255F;
            float a=x/8F,b=y/8F,c=(x+1)/8F,d=(y+1)/8F;
            mesh.addVertex(matrix,a,b,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
            mesh.addVertex(matrix,c,b,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
            mesh.addVertex(matrix,c,d,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
            mesh.addVertex(matrix,a,d,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255));
        }
    }
}

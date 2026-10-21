package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

/** Four full-bright orbiting pixels, including inventory and stored-item displays. */
public final class InfusedCrystalRenderer extends BlockEntityWithoutLevelRenderer {
    private static final net.minecraft.client.resources.model.ModelResourceLocation BODY=CrystalGridRenderer.part("infused_spiritual_crystal","body"),
            MOTE=CrystalGridRenderer.part("infused_spiritual_crystal","mote");
    public InfusedCrystalRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    private void drawBody(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        StaticModelQuads.draw(mc.getModelManager().getModel(BODY),pose,buffers.getBuffer(CrystalGlassLayer.ATLAS),light,overlay);
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        drawBody(pose,buffers,light,overlay);
        var motes=StaticModelQuads.collect(mc.getModelManager().getModel(MOTE));
        var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        for(int i=0;i<4;i++){
            double a=time*.04+i*Math.PI/2;
            pose.pushPose();pose.translate(.5+Math.cos(a)*.38,.48+Math.sin(a*1.7)*.26,.5+Math.sin(a)*.32);
            mc.getItemRenderer().renderQuadList(pose,out,motes,ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        }
    }
}

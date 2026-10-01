package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

/** Four full-bright orbiting pixels, including inventory and stored-item displays. */
public final class InfusedCrystalRenderer extends BlockEntityWithoutLevelRenderer {
    public InfusedCrystalRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    private void draw(String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(CrystalGridRenderer.part("infused_spiritual_crystal",part)),
                ItemStack.EMPTY,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        draw("body",pose,buffers,light,overlay);
        for(int i=0;i<4;i++){
            double a=time*.04+i*Math.PI/2;
            pose.pushPose();pose.translate(.5+Math.cos(a)*.38,.48+Math.sin(a*1.7)*.26,.5+Math.sin(a)*.32);
            draw("mote",pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        }
    }
}

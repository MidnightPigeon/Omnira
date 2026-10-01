package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class MilkSuspensionRenderer extends BlockEntityWithoutLevelRenderer {
    public MilkSuspensionRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model() {return CrystalGridRenderer.part("milk_suspension","liquid");}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        double t=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(.5,.5+Math.sin(t*.04)*.012,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(t*.65%360)));
        draw(stack,pose,buffers,light,overlay);
        // Local model particles also work in inventory previews without spawning world entities.
        for(int i=0;i<6;i++) {
            double phase=t*.045+i*Math.PI/3;
            pose.pushPose();
            pose.translate(Math.cos(phase)*.39,Math.sin(phase*1.7+i)*.26,Math.sin(phase)*.39);
            float size=.065F+.015F*(float)Math.sin(phase*2);
            pose.scale(size,size,size);
            draw(stack,pose,buffers,light,overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    private void draw(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        pose.pushPose();pose.translate(-.5,-.5,-.5);
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model()),stack,light,overlay,pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS));
        pose.popPose();
    }
}

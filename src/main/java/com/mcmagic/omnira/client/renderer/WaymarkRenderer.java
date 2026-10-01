package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.WaymarkBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class WaymarkRenderer implements BlockEntityRenderer<WaymarkBlockEntity> {
    public WaymarkRenderer(BlockEntityRendererProvider.Context context) {}
    public static ModelResourceLocation model() {return CrystalGridRenderer.part("spacetime_waymark","crystal");}
    @Override public void render(WaymarkBlockEntity mark,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        double time=mark.getLevel()==null?0:mark.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,.54+Math.sin(time*.045)*.025,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*.35%360)));pose.translate(-.5,0,-.5);
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model()),ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(WaymarkBlockEntity mark) {return true;}
}

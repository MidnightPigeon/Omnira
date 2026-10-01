package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;

public final class CrystalPedestalRenderer implements BlockEntityRenderer<CrystalPedestalBlockEntity> {
    public static final double DISPLAY_HEIGHT = .98;
    public static final float DISPLAY_SCALE = .75F;
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public CrystalPedestalRenderer(BlockEntityRendererProvider.Context context) {items=context.getItemRenderer();}
    @Override public void render(CrystalPedestalBlockEntity pedestal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(pedestal.getLevel()==null || pedestal.getItem(0).isEmpty()) return;
        double time=pedestal.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,DISPLAY_HEIGHT+Math.sin(time*.035)*.01,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*.6%360)));
        pose.scale(DISPLAY_SCALE,DISPLAY_SCALE,DISPLAY_SCALE);
        items.renderStatic(pedestal.getItem(0),ItemDisplayContext.FIXED,light,overlay,pose,buffers,pedestal.getLevel(),0);
        pose.popPose();
    }
}

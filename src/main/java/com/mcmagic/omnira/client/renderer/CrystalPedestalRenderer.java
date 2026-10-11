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
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(CrystalPedestalBlockEntity pedestal){
        var p=pedestal.getBlockPos();return new net.minecraft.world.phys.AABB(p.getX()-.25,p.getY(),p.getZ()-.25,p.getX()+1.25,p.getY()+1.8,p.getZ()+1.25);
    }
    @Override public boolean shouldRender(CrystalPedestalBlockEntity pedestal,net.minecraft.world.phys.Vec3 camera){
        return BlockEntityRenderer.super.shouldRender(pedestal,camera)&&com.mcmagic.omnira.client.LabyrinthDisplayVisibility.visible(pedestal,camera,getRenderBoundingBox(pedestal));
    }
    @Override public void render(CrystalPedestalBlockEntity pedestal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(pedestal.getLevel()==null || pedestal.getItem(0).isEmpty()) return;
        double time=pedestal.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,DISPLAY_HEIGHT+Math.sin(time*.035)*.01,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*.6%360)));
        pose.scale(DISPLAY_SCALE,DISPLAY_SCALE,DISPLAY_SCALE);
        items.renderStatic(pedestal.getItem(0),ItemDisplayContext.FIXED,light,overlay,pose,CrystalGlassLayer.contents(buffers),pedestal.getLevel(),0);
        pose.popPose();
    }
}

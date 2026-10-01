package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.RitualEnergyCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class RitualEnergyCoreRenderer implements BlockEntityRenderer<RitualEnergyCoreBlockEntity> {
    public RitualEnergyCoreRenderer(BlockEntityRendererProvider.Context context) {}
    public static ModelResourceLocation model() {return CrystalGridRenderer.part("ritual_energy_core","cube");}
    public static ModelResourceLocation advancedModel() {return CrystalGridRenderer.part("advanced_ritual_energy_core","cube");}
    public static void cube(PoseStack pose,MultiBufferSource buffers,double time,int overlay) {
        cube(pose,buffers,time,overlay,false);
    }
    public static void cube(PoseStack pose,MultiBufferSource buffers,double time,int overlay,boolean advanced) {
        pose.pushPose();
        pose.translate(.5,.71+Math.sin(time*.035)*.015,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*.7%360)));
        pose.mulPose(Axis.ZP.rotationDegrees((float)(time*.45%360)));
        pose.translate(-.5,-.5,-.5);
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(advanced?advancedModel():model()),ItemStack.EMPTY,
                LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        pose.popPose();
    }
    @Override public void render(RitualEnergyCoreBlockEntity core,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        cube(pose,buffers,core.getLevel()==null?0:core.getLevel().getGameTime()+partial,overlay,
                core.getBlockState().is(com.mcmagic.omnira.registry.ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()));
    }
}

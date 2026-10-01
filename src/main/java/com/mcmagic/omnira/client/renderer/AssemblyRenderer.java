package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.AssemblyAccess;
import com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class AssemblyRenderer implements BlockEntityRenderer<BlockEntity> {
    private final CrystalProcessingTableRenderer contents;
    public AssemblyRenderer(BlockEntityRendererProvider.Context context) {contents=new CrystalProcessingTableRenderer(context);}
    public static ModelResourceLocation shaft() {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/assembly_shaft"));
    }
    @Override public void render(BlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!(entity instanceof AssemblyAccess access)) return;
        contents.render(access.assembly(),partial,pose,buffers,light,overlay);
        float angle=entity instanceof ArcaneAssemblyTableBlockEntity?0:CreateEnginePhase.degrees(entity);
        renderShaft(entity,pose,buffers,light,overlay,angle);
    }
    public void renderPreview(BlockEntity entity,PoseStack pose,MultiBufferSource buffers,int light,int overlay,
                              double time,float angle,double strikeAge) {
        if(!(entity instanceof AssemblyAccess access))return;
        contents.renderPreview(access.assembly(),pose,buffers,light,overlay,time,strikeAge);
        renderShaft(entity,pose,buffers,light,overlay,angle);
    }
    private void renderShaft(BlockEntity entity,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float angle) {
        var facing=entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        if(facing==net.minecraft.core.Direction.SOUTH || facing==net.minecraft.core.Direction.EAST) angle=-angle;
        pose.pushPose();
        pose.translate(.5,.5,.5);
        pose.mulPose(Axis.YP.rotationDegrees(180-facing.toYRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-.5,-.5,-.5);
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(shaft()),ItemStack.EMPTY,light,overlay,
                pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        pose.popPose();
    }
}

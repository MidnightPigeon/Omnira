package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.MemoryCubeBlock;
import com.mcmagic.omnira.mire.MemoryCubeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Opaque floating cube; vanilla enchantment glyphs are emitted by its block animation. */
public final class MemoryCubeRenderer implements BlockEntityRenderer<MemoryCubeBlockEntity> {
    public MemoryCubeRenderer(BlockEntityRendererProvider.Context context){}
    public static ModelResourceLocation model(boolean peaceful){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira",
                peaceful?"block/memory_cube":"block/memory_cube_corrupted"));
    }
    @Override public void render(MemoryCubeBlockEntity cube,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(cube.getLevel()==null)return;
        boolean peaceful=cube.getBlockState().getValue(MemoryCubeBlock.PEACEFUL);
        double time=cube.getLevel().getGameTime()+partial+(cube.getBlockPos().asLong()&127);
        pose.pushPose();
        pose.translate(.5,.5+Math.sin(time*.08)*.07,.5);
        double motion=time*(peaceful?1:1.3);
        pose.mulPose(Axis.XP.rotationDegrees((float)(motion*.62)));
        pose.mulPose(Axis.YP.rotationDegrees((float)(motion*.91)));
        pose.mulPose(Axis.ZP.rotationDegrees((float)(motion*.43)));
        pose.translate(-.5,-.5,-.5);
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(peaceful)),ItemStack.EMPTY,
                light,overlay,pose,buffers.getBuffer(Sheets.solidBlockSheet()));
        pose.popPose();
    }
    @Override public int getViewDistance(){return 48;}
}

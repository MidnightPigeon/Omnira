package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Uses the assembled machine geometry for inventory, held items and recipe catalysts. */
public final class AdvancedForgeItemRenderer extends BlockEntityWithoutLevelRenderer {
    public AdvancedForgeItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.2,.5);pose.scale(.32F,.32F,.32F);
        AdvancedForgeRenderer.renderLocal(null,0,0,pose,buffers,light,overlay);
        pose.popPose();
    }
}

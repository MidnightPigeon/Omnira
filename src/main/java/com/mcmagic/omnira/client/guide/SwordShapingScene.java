package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.SwordShapingRitual;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

public final class SwordShapingScene {
    public static void render(GuiGraphics g,int x,int y,int ticks) {
        var mc=Minecraft.getInstance();var pose=g.pose();var buffers=mc.renderBuffers().bufferSource();
        g.flush();RenderSystem.enableDepthTest();pose.pushPose();pose.translate(x+62,y+76,160);
        pose.scale(15,-15,15);pose.mulPose(Axis.XP.rotationDegrees(28));pose.mulPose(Axis.YP.rotationDegrees(-35));pose.translate(-.5,0,-.5);
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
            pose.pushPose();pose.translate(dx,-1,dz);mc.getBlockRenderer().renderSingleBlock(Blocks.SMOOTH_STONE.defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);pose.popPose();
        }
        var offerings=new Item[]{ModItems.RESONANCE_CRYSTAL.get(),ModItems.SPIRITUAL_CRYSTAL.get(),DreamContent.LIGHT_CRYSTAL_CORE.get().asItem(),Items.BONE};
        for(int i=0;i<4;i++) {
            var p=SwordShapingRitual.ANCHORS[i];pose.pushPose();pose.translate(p[0],0,p[1]);
            mc.getBlockRenderer().renderSingleBlock(ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.translate(.5,1.1,.5);pose.scale(.6F,.6F,.6F);
            mc.getItemRenderer().renderStatic(new ItemStack(offerings[i]),ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);pose.popPose();
        }
        mc.getBlockRenderer().renderSingleBlock(ModBlocks.PURE_VESSEL.get().defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();
    }
}

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
    public static String render(GuiGraphics g,int x,int y,int ticks,float yaw,float pitch,int mouseX,int mouseY) {
        var mc=Minecraft.getInstance();var pose=g.pose();var buffers=mc.renderBuffers().bufferSource();
        g.flush();RenderSystem.enableDepthTest();var cursor=pose.last().pose().transformPosition(mouseX,mouseY,0,new org.joml.Vector3f());
        String tooltip="";
        pose.pushPose();pose.translate(x+62,y+76,160);
        pose.scale(15,-15,15);pose.mulPose(Axis.XP.rotationDegrees(28+pitch));pose.mulPose(Axis.YP.rotationDegrees(-35+yaw));pose.translate(-.5,0,-.5);
        int selected=mouseX>=x && mouseX<x+124 && mouseY>=y && mouseY<y+110?RitualLayout.pick(pose,cursor,SwordShapingRitual.ANCHORS):-1;
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
            pose.pushPose();pose.translate(dx,-1,dz);mc.getBlockRenderer().renderSingleBlock(Blocks.SMOOTH_STONE.defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);pose.popPose();
        }
        var recipe=mc.level==null?java.util.Optional.<com.mcmagic.omnira.recipe.SwordShapingRecipe>empty():GuideRecipes.swordExample(mc.level);
        for(int i=0;i<4;i++) {
            var p=SwordShapingRitual.ANCHORS[i];pose.pushPose();pose.translate(p[0],0,p[1]);
            mc.getBlockRenderer().renderSingleBlock(ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.translate(.5,1.1,.5);pose.scale(.6F,.6F,.6F);
            if(recipe.isPresent()){
                var choices=recipe.get().offerings().get(i).getItems();
                if(choices.length==0){pose.popPose();continue;}
                var offering=choices[Math.floorMod(ticks/25,choices.length)];
                if(i==selected){RitualLayout.highlight(pose,buffers);tooltip=offering.getHoverName().getString();}
                mc.getItemRenderer().renderStatic(offering,ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);
            }
            pose.popPose();
        }
        com.mcmagic.omnira.client.renderer.PureVesselRenderer.drawIntact(ModBlocks.PURE_VESSEL.get().defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();return tooltip;
    }
}

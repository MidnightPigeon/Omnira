package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.world.dimension.DreamRitual;
import com.mcmagic.omnira.client.renderer.DreamPortalRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Read-only 3D diagram using the exact blocks and offsets of the ritual. */
public final class DreamRitualScene {
    public static void render(GuiGraphics graphics,int x,int y,int ticks) {
        render(graphics,x,y,ticks,false);
    }
    public static void render(GuiGraphics graphics,int x,int y,int ticks,boolean resonance) {
        render(graphics,x,y,ticks,resonance,false);
    }
    public static void render(GuiGraphics graphics,int x,int y,int ticks,boolean resonance,boolean affinity) {
        render(graphics,x,y,ticks,resonance,affinity,false);
    }
    private static net.minecraft.client.model.geom.ModelPart mirrorFrame;
    public static void render(GuiGraphics graphics,int x,int y,int ticks,boolean resonance,boolean affinity,boolean corridor) {
        render(graphics,x,y,ticks,resonance,affinity,corridor,false);
    }
    public static void render(GuiGraphics graphics,int x,int y,int ticks,boolean resonance,boolean affinity,boolean corridor,boolean cleansing) {
        var mc=Minecraft.getInstance();
        boolean active=!resonance && !affinity && !corridor && !cleansing && ticks%200>=100;
        graphics.flush();
        RenderSystem.enableDepthTest();
        var pose=graphics.pose();
        var buffers=mc.renderBuffers().bufferSource();
        pose.pushPose();pose.translate(x+62,y+73,160);
        pose.scale(11,-11,11);pose.mulPose(Axis.XP.rotationDegrees(28));pose.mulPose(Axis.YP.rotationDegrees(-35));
        pose.translate(-.5,0,-.5);
        for(int dx=-3;dx<=3;dx++) for(int dz=-3;dz<=3;dz++) {
            pose.pushPose();pose.translate(dx,-1,dz);
            mc.getBlockRenderer().renderSingleBlock(Blocks.SMOOTH_STONE.defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        int offeringIndex=0;
        for(int[] p:DreamRitual.ANCHORS) {
            pose.pushPose();pose.translate(p[0],0,p[1]);
            mc.getBlockRenderer().renderSingleBlock(ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.translate(.5,com.mcmagic.omnira.client.renderer.CrystalPedestalRenderer.DISPLAY_HEIGHT,.5);
            float scale=com.mcmagic.omnira.client.renderer.CrystalPedestalRenderer.DISPLAY_SCALE;
            pose.scale(scale,scale,scale);
            mc.getItemRenderer().renderStatic(new ItemStack(corridor?com.mcmagic.omnira.spacetime.CorridorRitual.offering(offeringIndex++):affinity?com.mcmagic.omnira.world.dimension.AffinityRitual.ingredients().get(offeringIndex++):resonance?com.mcmagic.omnira.registry.ModItems.RESONANCE_CORE.get():cleansing?com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get():com.mcmagic.omnira.registry.ModItems.SPIRITUAL_CRYSTAL.get()),ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);
            pose.popPose();
        }
        mc.getBlockRenderer().renderSingleBlock((affinity || corridor?ModBlocks.ADVANCED_RITUAL_ENERGY_CORE:ModBlocks.RITUAL_ENERGY_CORE).get().defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        com.mcmagic.omnira.client.renderer.RitualEnergyCoreRenderer.cube(pose,buffers,ticks,OverlayTexture.NO_OVERLAY,affinity || corridor);
        if(corridor){
            if(mirrorFrame==null)mirrorFrame=com.mcmagic.omnira.client.renderer.DreamMirrorRenderer.createFrame();
            pose.pushPose();pose.translate(.5,1,.5);
            com.mcmagic.omnira.client.renderer.CorridorGatewayRenderer.draw(mirrorFrame,pose,buffers,ticks,Math.min(1,(ticks%400)/240F),LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        if(active) {
            pose.pushPose();pose.translate(.5,1.5,.5);
            pose.mulPose(Axis.YP.rotationDegrees(35));pose.mulPose(Axis.XP.rotationDegrees(-28));
            DreamPortalRenderer.swirl(pose,buffers,ticks);
            pose.popPose();
        }
        buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();
        if(resonance) {
            graphics.renderItem(new ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_KNOT.get()),x+54,y+108);
        }
        if(cleansing)graphics.renderItem(new ItemStack(com.mcmagic.omnira.registry.ModItems.PARADOX_DUST.get()),x+54,y+108);
        if(affinity)graphics.renderItem(com.mcmagic.omnira.mana.Affinity.byId(1+(ticks/60)%3).offering(),x+54,y+108);
        if(corridor)graphics.renderItem(new ItemStack(com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()),x+54,y+108);
        var label=net.minecraft.network.chat.Component.translatable(active?"guide.omnira.dream.active":"guide.omnira.dream.layout");
        graphics.drawString(mc.font,label,x+(124-mc.font.width(label))/2,y+132,0xFF514333,false);
    }
}

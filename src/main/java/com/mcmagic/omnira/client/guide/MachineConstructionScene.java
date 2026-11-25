package com.mcmagic.omnira.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Construction blocks come from formation rules, never from an independently authored picture. */
public final class MachineConstructionScene {
    public static boolean supports(String scene){return scene.equals("advanced_crystal_aggregation_ring")||scene.equals("large_alchemical_reaction_pool")||scene.equals("alchemical_catalysis_chamber")||scene.equals("spacetime_engine");}
    public static void render(GuiGraphics g,int x,int y,int ticks,String scene,float yaw,float pitch,boolean construction){
        if(construction){
            ConstructionPreview.draw(g,x,y,ticks,scene,yaw,pitch,-1,-1);
            return;
        }
        var mc=Minecraft.getInstance();var buffers=mc.renderBuffers().bufferSource();var pose=g.pose();
        net.minecraft.client.renderer.MultiBufferSource layers=type->buffers.getBuffer(
            type==net.minecraft.client.renderer.Sheets.translucentCullBlockSheet()||type==net.minecraft.client.renderer.Sheets.translucentItemSheet()
                ?com.mcmagic.omnira.client.renderer.CrystalGlassLayer.ATLAS:type);
        g.flush();RenderSystem.enableDepthTest();pose.pushPose();
        pose.translate(x+62,y+80,180);pose.scale(24,-24,24);
        pose.mulPose(Axis.XP.rotationDegrees(25+pitch));pose.mulPose(Axis.YP.rotationDegrees(-35+yaw+(float)Math.sin(ticks*.007)*20));
        boolean pool=scene.equals("large_alchemical_reaction_pool");
        boolean chamber=scene.equals("alchemical_catalysis_chamber");
        boolean engine=scene.equals("spacetime_engine");
        if(chamber)pose.translate(0,-.4,0);
        pose.pushPose();pose.translate(-.5,pool||chamber||engine?0:-.5,-.5);
        drawAssembled(scene,ticks,pose,layers);
        pose.popPose();buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();
    }
    static void drawAssembled(String scene,double ticks,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource layers){
        if(scene.equals("advanced_assembly_table"))com.mcmagic.omnira.client.renderer.AdvancedForgeRenderer.renderLocal(null,0,ticks,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        else if(scene.equals("spacetime_engine"))com.mcmagic.omnira.client.SpacetimeEngineClient.draw(null,ticks,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        else if(scene.equals("alchemical_catalysis_chamber"))com.mcmagic.omnira.client.CatalysisClient.draw(null,ticks,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        else if(scene.equals("large_alchemical_reaction_pool"))com.mcmagic.omnira.client.ReactionPoolClient.renderPreview(ticks,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        else com.mcmagic.omnira.client.AggregationClient.render(null,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
    }
    private MachineConstructionScene(){}
}

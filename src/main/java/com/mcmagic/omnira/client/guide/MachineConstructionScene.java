package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.alchemy.ReactionPoolStructure;
import com.mcmagic.omnira.registry.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;

/** Construction blocks come from formation rules, never from an independently authored picture. */
public final class MachineConstructionScene {
    public static boolean supports(String scene){return scene.equals("advanced_crystal_aggregation_ring")||scene.equals("large_alchemical_reaction_pool")||scene.equals("alchemical_catalysis_chamber")||scene.equals("spacetime_engine");}
    public static void render(GuiGraphics g,int x,int y,int ticks,String scene,float yaw,float pitch,boolean construction){
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
        if(!construction){
            pose.pushPose();pose.translate(-.5,pool||chamber||engine?0:-.5,-.5);
            if(engine)com.mcmagic.omnira.client.SpacetimeEngineClient.draw(null,ticks,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            else if(chamber)com.mcmagic.omnira.client.CatalysisClient.draw(null,ticks,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            else if(pool)com.mcmagic.omnira.client.ReactionPoolClient.renderPreview(ticks,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            else com.mcmagic.omnira.client.AggregationClient.render(null,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.popPose();buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();return;
        }
        int count=engine?10:chamber?3:pool?18:9;
        for(int part=0;part<count;part++){
            var block=engine?com.mcmagic.omnira.engine.SpacetimeEngineStructure.required(part):chamber?com.mcmagic.omnira.alchemy.catalysis.CatalysisStructure.required(part):pool?ReactionPoolStructure.required(part):ringBlock(part,ticks);
            if(block==Blocks.AIR)continue;
            int dx=chamber?0:part%3-1,dy=chamber?part:pool?part/9:1-part/3,dz=pool?(part%9)/3-1:0;
            var state=block.defaultBlockState();
            if(engine){var offset=com.mcmagic.omnira.engine.SpacetimeEngineBlock.offset(part);dx=offset.getX();dy=offset.getY();dz=offset.getZ();
                int side=com.mcmagic.omnira.engine.SpacetimeEngineBlock.side(part);
                if(side>=0)state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,com.mcmagic.omnira.engine.SpacetimeEngineBlock.OUTPUTS[side]);}
            pose.pushPose();pose.translate(dx-.5,dy,dz-.5);
            mc.getBlockRenderer().renderSingleBlock(state,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();
    }
    private static net.minecraft.world.level.block.Block ringBlock(int part,int ticks){
        if(part==4)return ModBlocks.CRYSTAL_PROCESSING_TABLE.get();
        if(part==1||part==7)return ticks/80%2==0?DreamContent.EXCITED_SPATIAL_CRYSTAL.get():com.mcmagic.omnira.aggregation.AggregationContent.SPATIAL_BLOCK.get();
        if(part==3||part==5)return ticks/80%2==0?DreamContent.LIVING_TEMPORAL_SILT.get():TimeNatureContent.LIVING_SILT.get();
        return ModBlocks.INFUSED_CRYSTAL_CASING.get();
    }
    private MachineConstructionScene(){}
}

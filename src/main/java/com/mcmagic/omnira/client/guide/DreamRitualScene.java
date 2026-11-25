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
    private static net.minecraft.client.model.geom.ModelPart mirrorFrame;
    public static String render(GuiGraphics graphics,int x,int y,int ticks,String scene,float yaw,float pitch,int mouseX,int mouseY) {
        boolean resonance=scene.equals("resonance_ritual"),affinity=scene.equals("affinity_ritual"),
                corridor=scene.equals("corridor_ritual"),cleansing=scene.equals("cleansing_ritual"),
                genesis=scene.equals("genesis_ritual"),worldForming=scene.equals("world_forming_ritual");
        var mc=Minecraft.getInstance();
        boolean active=!worldForming && !genesis && !resonance && !affinity && !corridor && !cleansing && ticks%200>=100;
        graphics.flush();
        int captionY=y+3;
        int modelTop=captionY+mc.font.lineHeight+3;
        com.mcmagic.omnira.client.screen.UiScissor.enable(graphics,x,modelTop,x+124,y+124);
        RenderSystem.enableDepthTest();
        var pose=graphics.pose();
        var buffers=mc.renderBuffers().bufferSource();
        var cursor=pose.last().pose().transformPosition(mouseX,mouseY,0,new org.joml.Vector3f());
        String tooltip="";
        pose.pushPose();pose.translate(x+62,y+73,160);
        pose.scale(11,-11,11);pose.mulPose(Axis.XP.rotationDegrees(28+pitch));pose.mulPose(Axis.YP.rotationDegrees(-35+yaw));
        pose.translate(-.5,0,-.5);
        int selected=mouseX>=x && mouseX<x+124 && mouseY>=modelTop && mouseY<y+110?RitualLayout.pick(pose,cursor,DreamRitual.ANCHORS):-1;
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
            var offering=new ItemStack(RitualLayout.offering(scene,offeringIndex));
            boolean hovered=selected==offeringIndex++;
            if(hovered || !worldForming || ticks%440<offeringIndex*com.mcmagic.omnira.myriad.WorldFormingRitual.ASPECT_TICKS){
                mc.getItemRenderer().renderStatic(offering,ItemDisplayContext.FIXED,
                        LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);
                if(hovered){RitualLayout.highlight(pose,buffers);tooltip=offering.getHoverName().getString();}
            }
            pose.popPose();
        }
        mc.getBlockRenderer().renderSingleBlock((worldForming || genesis || affinity || corridor?ModBlocks.ADVANCED_RITUAL_ENERGY_CORE:ModBlocks.RITUAL_ENERGY_CORE).get().defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        com.mcmagic.omnira.client.renderer.RitualEnergyCoreRenderer.cube(pose,buffers,ticks,OverlayTexture.NO_OVERLAY,worldForming || genesis || affinity || corridor);
        if(worldForming)com.mcmagic.omnira.client.renderer.WorldFormingRenderer.draw(ticks%440,ticks,index->index,pose,buffers,OverlayTexture.NO_OVERLAY);
        if(genesis)com.mcmagic.omnira.client.renderer.GenesisOfferingRenderer.render(pose,buffers,Math.min(10000,(ticks%240)*50),Math.max(0,ticks%240-200),OverlayTexture.NO_OVERLAY);
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
        graphics.disableScissor();
        if(resonance) {
            graphics.renderItem(new ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_KNOT.get()),x+54,y+108);
        }
        if(cleansing)graphics.renderItem(new ItemStack(com.mcmagic.omnira.registry.ModItems.PARADOX_DUST.get()),x+54,y+108);
        if(genesis){
            var fluid=com.mcmagic.omnira.world.dimension.GenesisRitual.offering();
            var appearance=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
            var sprite=mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(appearance.getStillTexture(fluid));
            int tint=appearance.getTintColor(fluid);
            graphics.setColor((tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,1);
            graphics.blit(x+25,y+108,0,16,16,sprite);graphics.setColor(1,1,1,1);
            var amount=fluid.getAmount()+" mB";
            float scale=Math.min(1,75F/Math.max(1,mc.font.width(amount)));
            graphics.pose().pushPose();graphics.pose().translate(x+47,y+112,0);graphics.pose().scale(scale,scale,1);
            graphics.drawString(mc.font,amount,0,0,0xFF405462,false);graphics.pose().popPose();
        }
        if(worldForming)graphics.renderItem(new ItemStack(com.mcmagic.omnira.myriad.WorldSeedContent.ACTIVE.get()),x+54,y+108);
        if(affinity)graphics.renderItem(com.mcmagic.omnira.mana.Affinity.VISIBLE.get((ticks/60)%com.mcmagic.omnira.mana.Affinity.VISIBLE.size()).offering(),x+54,y+108);
        if(corridor)graphics.renderItem(new ItemStack(com.mcmagic.omnira.spacetime.CorridorRitual.activator(ticks/80%2!=0)),x+54,y+108);
        var label=net.minecraft.network.chat.Component.translatable(active?"guide.omnira.dream.active":"guide.omnira.dream.layout");
        graphics.drawString(mc.font,label,x+(124-mc.font.width(label))/2,captionY,0xFF514333,false);
        return tooltip;
    }
}

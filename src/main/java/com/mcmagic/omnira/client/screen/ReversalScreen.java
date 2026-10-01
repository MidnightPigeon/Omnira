package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.reversal.ReversalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ReversalScreen extends AbstractContainerScreen<ReversalMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/gui/container/spacetime_reverser.png");
    private static final ResourceLocation FLOW=ResourceLocation.parse("omnira:textures/gui/reversal_flow.png");
    public ReversalScreen(ReversalMenu m,Inventory i,Component c){super(m,i,c);imageWidth=176;imageHeight=244;inventoryLabelY=148;}
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button){if(x>=left-29&&x<left-3&&y>=top+34&&y<top+135)return false;return super.hasClickedOutside(x,y,left,top,button);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(TEXTURE,leftPos,topPos,0,0,176,244,176,244);
        g.fill(leftPos-29,topPos+34,leftPos-3,topPos+135,0xE0355665);g.fill(leftPos-24,topPos+39,leftPos-8,topPos+105,0xFF152F39);
        int h=menu.energy();g.fill(leftPos-23,topPos+104-h,leftPos-9,topPos+104,0xFFA5EBC1);
        g.fill(leftPos-25,topPos+111,leftPos-7,topPos+129,0xFF263A49);g.fill(leftPos-24,topPos+112,leftPos-8,topPos+128,0xFF7D939B);
        g.flush();
        g.pose().pushPose();g.pose().translate(0,0,2);
        g.blit(FLOW,leftPos+76,topPos+63,0,0,24,12,24,24);
        int progress=24*menu.progress()/100;
        if(progress>0)g.blit(FLOW,leftPos+76,topPos+63,0,12,progress,12,24,24);
        g.pose().popPose();
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawCenteredString(font,title,88,4,0xE6F6F8);g.drawString(font,playerInventoryTitle,8,148,0xD6E8E9,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);if(x>=leftPos-29&&x<leftPos-3&&y>=topPos+34&&y<topPos+108)g.renderTooltip(font,Component.translatable("tooltip.omnira.reversal_energy",menu.energy(),64),x,y);}
}

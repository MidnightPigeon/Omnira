package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.aggregation.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class AggregationRingScreen extends ResponsiveContainerScreen<AggregationRingMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/gui/container/aggregation_ring.png");
    public AggregationRingScreen(AggregationRingMenu menu,Inventory player,Component title){super(menu,player,title);imageWidth=192;imageHeight=272;inventoryLabelX=15;inventoryLabelY=179;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(TEXTURE,leftPos,topPos+176,0,176,imageWidth,imageHeight-176,imageWidth,imageHeight);
        MachinePanelArt.aggregation(CrystalUi.pixels(g,leftPos,topPos),AggregationLayout.UI,AggregationLayout.COLORS,
                CrystalUi.time(partial),menu.progress()/(float)AggregationLayout.DURATION);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        float scale=Math.min(1,178F/font.width(title));
        g.pose().pushPose();g.pose().translate(96,8,0);g.pose().scale(scale,scale,1);g.drawCenteredString(font,title,0,0,0xF0F7FF);g.pose().popPose();
        g.drawString(font,playerInventoryTitle,15,179,0xE1EDF4,false);
    }
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial){
        super.renderContent(g,x,y,partial);
        CompositeSlotGhost.render(g,menu.getSlot(0),menu.getSlot(1),leftPos,topPos);
        renderTooltip(g,x,y);
        if(hoveredSlot!=null&&hoveredSlot.index<AggregationLayout.SLOTS&&!hoveredSlot.hasItem())
            g.renderTooltip(font,Component.translatable(hoveredSlot.index==1&&com.mcmagic.omnira.spell.SpellPattern.composite(menu.getSlot(0).getItem())?
                    "tooltip.omnira.microcore.composite":"gui.omnira.aggregation."+AggregationLayout.role(hoveredSlot.index)),x,y);
    }
}

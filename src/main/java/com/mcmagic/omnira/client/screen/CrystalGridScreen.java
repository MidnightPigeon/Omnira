package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.CrystalGridMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CrystalGridScreen extends AbstractContainerScreen<CrystalGridMenu> {
    private static final ResourceLocation VANILLA=ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private final ResourceLocation background;
    public CrystalGridScreen(CrystalGridMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
        imageWidth=176;imageHeight=240;inventoryLabelY=146;
        background=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/crystal_grid_"+menu.capacity+".png");
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.blit(background,leftPos,topPos,0,0,176,240,176,240);
        g.blit(VANILLA,leftPos+7,topPos+157,7,83,162,76,256,256);
        CrystalProcessingTableScreen.renderVanillaFrame(g,leftPos,topPos,176,240);
        for(int i=0;i<menu.capacity;i++) {
            int x=leftPos+CrystalGridMenu.slotX(menu.capacity,i)-1;
            int y=topPos+CrystalGridMenu.slotY(menu.capacity,i)-1;
            g.blit(VANILLA,x,y,7,83,18,18,256,256);
            if(i==menu.nextSlot()) {
                g.fill(x-1,y-1,x+19,y,0xFFD6AD51);
                g.fill(x-1,y+18,x+19,y+19,0xFFD6AD51);
                g.fill(x-1,y,x,y+18,0xFFD6AD51);
                g.fill(x+18,y,x+19,y+18,0xFFD6AD51);
            }
        }
        g.renderItem(menu.icon(),leftPos+80,topPos+72);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,(imageWidth-font.width(title))/2,7,0xFF3A2416,false);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF404040,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        super.render(g,x,y,partial);
        renderTooltip(g,x,y);
    }
}

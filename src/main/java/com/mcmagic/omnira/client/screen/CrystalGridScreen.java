package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.CrystalGridMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CrystalGridScreen extends ResponsiveContainerScreen<CrystalGridMenu> {
    private static final ResourceLocation VANILLA=ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private final ResourceLocation background;
    public CrystalGridScreen(CrystalGridMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
        imageWidth=176;imageHeight=240;inventoryLabelY=146;
        background=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/crystal_grid_"+menu.capacity+".png");
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.blit(background,leftPos,topPos+142,0,142,176,98,176,240);
        g.blit(VANILLA,leftPos+7,topPos+157,7,83,162,76,256,256);
        g.blit(VANILLA,leftPos,topPos+236,0,162,176,4,256,256);
        g.blit(VANILLA,leftPos,topPos+142,4,94,0F,50F,4,1,256,256);
        g.blit(VANILLA,leftPos+172,topPos+142,4,94,172F,50F,4,1,256,256);
        MachinePanelArt.grid(CrystalUi.pixels(g,leftPos,topPos),menu.capacity,menu.nextSlot(),CrystalUi.time(partial));
        g.renderItem(menu.icon(),leftPos+80,topPos+72);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        CrystalUi.title(g,font,title,88,7,166,0xFFE0F3EF);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF404040,false);
    }
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial) {
        super.renderContent(g,x,y,partial);
        renderTooltip(g,x,y);
    }
}

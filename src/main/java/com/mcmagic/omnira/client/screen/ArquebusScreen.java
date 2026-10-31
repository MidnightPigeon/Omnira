package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.ArquebusMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ArquebusScreen extends ResponsiveContainerScreen<ArquebusMenu> {
    private static final ResourceLocation VANILLA=ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    public ArquebusScreen(ArquebusMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFC6C6C6);
        CrystalProcessingTableScreen.renderVanillaFrame(g,leftPos,topPos,imageWidth,imageHeight);
        g.blit(VANILLA,leftPos+7,topPos+83,7,83,162,76,256,256);
        var pixels=CrystalUi.pixels(g,leftPos,topPos);
        MachinePanelArt.polygon(pixels,MachinePanelArt.RIM,80,26,96,26,105,35,105,51,96,60,80,60,71,51,71,35);
        MachinePanelArt.polygon(pixels,MachinePanelArt.DARK,81,28,95,28,103,36,103,50,95,58,81,58,73,50,73,36);
        MachinePanelArt.slot(pixels,88,43,MachinePanelArt.LIGHT);
        g.blit(VANILLA,leftPos+79,topPos+34,7,83,18,18,256,256);
    }
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial) {super.renderContent(g,x,y,partial);renderTooltip(g,x,y);}
}

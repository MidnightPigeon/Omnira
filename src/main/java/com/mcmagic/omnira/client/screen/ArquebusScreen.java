package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.ArquebusMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ArquebusScreen extends AbstractContainerScreen<ArquebusMenu> {
    private static final ResourceLocation VANILLA=ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    public ArquebusScreen(ArquebusMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFC6C6C6);
        CrystalProcessingTableScreen.renderVanillaFrame(g,leftPos,topPos,imageWidth,imageHeight);
        g.blit(VANILLA,leftPos+7,topPos+83,7,83,162,76,256,256);
        g.fill(leftPos+69,topPos+24,leftPos+107,topPos+62,0xFF779DA9);
        g.fill(leftPos+72,topPos+27,leftPos+104,topPos+59,0xFFCEE3EA);
        g.blit(VANILLA,leftPos+79,topPos+34,7,83,18,18,256,256);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {super.render(g,x,y,partial);renderTooltip(g,x,y);}
}

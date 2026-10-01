package com.mcmagic.omnira.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared crystal palette and scroll; each workstation retains its own material layout. */
final class CrystalWorkstationBackground {
    private static final ResourceLocation SCROLL=texture("crystal_workstation_scroll");
    private static final ResourceLocation VANILLA=ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/"+name+".png");}
    static void draw(GuiGraphics graphics,ResourceLocation texture,int x,int y){
        RenderSystem.enableBlend();
        graphics.blit(texture,x,y,0,0,176,240,176,240);
        graphics.blit(VANILLA,x+7,y+157,7,83,162,76,256,256);
        CrystalProcessingTableScreen.renderVanillaFrame(graphics,x,y+142,176,98);
    }
    static void labels(GuiGraphics graphics,Font font,Component title,Component inventory){
        RenderSystem.enableBlend();
        graphics.blit(SCROLL,29,8,0,0,118,20,118,20);
        WorkstationManaStatus.titleText(graphics,font,title,13,0xFF3A2416);
        graphics.drawString(font,inventory,8,146,0xFF404040,false);
    }
    private CrystalWorkstationBackground(){}
}

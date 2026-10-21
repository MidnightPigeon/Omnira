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
    static void inventory(GuiGraphics g,int x,int y,net.minecraft.world.inventory.AbstractContainerMenu menu){
        inventory(g,x,y,menu,17);
    }
    static void inventory(GuiGraphics g,int x,int y,net.minecraft.world.inventory.AbstractContainerMenu menu,int header){
        int left=Integer.MAX_VALUE,top=Integer.MAX_VALUE,right=0,bottom=0;
        for(var slot:menu.slots)if(slot.container instanceof net.minecraft.world.entity.player.Inventory){
            left=Math.min(left,slot.x);top=Math.min(top,slot.y);right=Math.max(right,slot.x+16);bottom=Math.max(bottom,slot.y+16);
        }
        if(left==Integer.MAX_VALUE)return;
        int width=right-left+16,height=bottom-top+header+8;
        g.blit(VANILLA,x+left-8,y+top-header,width,height,8F,72F,1,1,256,256);
        CrystalProcessingTableScreen.renderVanillaFrame(g,x+left-8,y+top-header,width,height);
        for(var slot:menu.slots)if(slot.container instanceof net.minecraft.world.entity.player.Inventory)
            g.blit(VANILLA,x+slot.x-1,y+slot.y-1,7,83,18,18,256,256);
    }
    static void draw(GuiGraphics graphics,ResourceLocation texture,int x,int y,String kind,float time,float progress,boolean active){
        RenderSystem.enableBlend();
        graphics.blit(texture,x,y+142,0,142,176,98,176,240);
        MachinePanelArt.workstation(CrystalUi.pixels(graphics,x,y),kind,time,progress,active);
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

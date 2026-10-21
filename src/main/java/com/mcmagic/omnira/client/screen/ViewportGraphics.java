package com.mcmagic.omnira.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A screen-local canvas: tooltip wrapping and positioning use the same coordinates as its contents. */
final class ViewportGraphics extends GuiGraphics {
    private final int width,height;
    ViewportGraphics(GuiGraphics source,int width,int height){
        super(Minecraft.getInstance(),source.bufferSource());this.width=width;this.height=height;
        pose().last().pose().set(source.pose().last().pose());
        pose().last().normal().set(source.pose().last().normal());
    }
    @Override public int guiWidth(){return width;}
    @Override public int guiHeight(){return height;}
}

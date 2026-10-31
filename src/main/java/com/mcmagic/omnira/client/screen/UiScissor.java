package com.mcmagic.omnira.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Convert local layout bounds before using the native GUI scissor stack. */
public final class UiScissor {
    private UiScissor() {}

    public static void enable(GuiGraphics graphics,int x,int y,int right,int bottom) {
        var bounds=UiViewport.scissor(graphics.pose().last().pose(),x,y,right,bottom);
        graphics.enableScissor(bounds[0],bounds[1],bounds[2],bounds[3]);
    }
}

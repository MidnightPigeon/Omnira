package com.mcmagic.omnira.client.screen;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;

/** Widget tooltips must be submitted before the screen's scaled pose is popped. */
final class ScaledTooltip {
    private List<FormattedCharSequence> text;
    private ClientTooltipPositioner positioner;
    void begin(){text=null;}
    void queue(List<FormattedCharSequence> lines,ClientTooltipPositioner anchor,boolean override){
        if(text==null||override){text=lines;positioner=anchor;}
    }
    void render(GuiGraphics g,Font font,int x,int y){if(text!=null)g.renderTooltip(font,text,positioner,x,y);}
}

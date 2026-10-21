package com.mcmagic.omnira.compat.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

/** Cyan is unused local potential; gold is occupied, together bounded by one capacity. */
final class PulseBar extends Element {
    private final double current, occupied, limit;
    private final String label;

    PulseBar(double current, double occupied, double limit) {
        this.current = current;
        this.occupied = occupied;
        this.limit = limit;
        label = String.format(java.util.Locale.ROOT, "%.0f | %.0f / %.0f MPU", occupied, current, limit);
    }
    @Override public Vec2 getSize() { return new Vec2(104, 11); }
    @Override public String getMessage() { return label; }
    @Override public void render(GuiGraphics g, float x, float y, float width, float height) {
        int left = (int)x, top = (int)y, barWidth = (int)Math.max(0,Math.min(getCachedSize().x,width-x));
        if(barWidth<3)return;int length=barWidth-2;
        com.mcmagic.omnira.client.screen.PulseMeterRenderer.bar(g,left,top,length+2,current,occupied,limit);
        var font = Minecraft.getInstance().font;
        float scale=Math.min(1,Math.max(0,length-4)/(float)Math.max(1,font.width(label)+1));
        g.pose().pushPose();g.pose().translate(left+barWidth/2F,top+5,0);g.pose().scale(scale,scale,1);
        g.drawString(font,label,-font.width(label)/2,-4,0xFFFFFFFF,true);g.pose().popPose();
    }
}

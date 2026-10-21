package com.mcmagic.omnira.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Tile the same authored facets as the offline pixel probe, without hundreds of GUI fill calls. */
public final class PulseMeterRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/gui/pulse_meter.png");
    public static void bar(GuiGraphics g,int x,int y,int width,double current,double occupied,double limit){
        if(width<3)return;
        int length=width-2;
        int filled=(int)Math.round(length*fraction(current+occupied,limit)),used=(int)Math.round(length*fraction(occupied,limit));
        g.fill(x,y,x+width,y+10,0xFFD0E5ED);
        segment(g,x+1,y+1,0,used,2);segment(g,x+1,y+1,used,filled,1);segment(g,x+1,y+1,filled,length,0);
    }
    private static void segment(GuiGraphics g,int x,int y,int start,int end,int layer){
        for(int at=start;at<end;){int u=at&15,n=Math.min(16-u,end-at);g.blit(TEXTURE,x+at,y,u,layer*8,n,8,16,24);at+=n;}
    }
    private static double fraction(double amount,double limit){return limit>0?Math.clamp(amount/limit,0,1):0;}
    private PulseMeterRenderer(){}
}

package com.mcmagic.omnira.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class CrystalUi {
    private CrystalUi(){}
    static MachinePanelArt.Pixels pixels(GuiGraphics g,int x,int y){return (a,b,c,d,color)->g.fill(x+a,y+b,x+c,y+d,color);}
    static double time(float partial){var level=Minecraft.getInstance().level;return level==null?0:level.getGameTime()+(double)partial;}
    static void label(GuiGraphics g,Font font,Component text,int x,int y,int width,float maximumScale,int color){
        float scale=Math.max(Math.min(.8F,maximumScale),Math.min(maximumScale,width/(float)Math.max(1,font.width(text))));
        var visible=fit(font,text,(int)(width/scale));
        if(!visible.equals(text))UiLabelTooltips.record(g,text,x,y,width,font.lineHeight);
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);g.drawString(font,visible,0,0,color,false);g.pose().popPose();
    }
    static void title(GuiGraphics g,Font font,Component text,int center,int y,int width,int color){
        float scale=Math.max(.8F,Math.min(1,width/(float)Math.max(1,font.width(text))));
        var visible=fit(font,text,(int)(width/scale));
        if(!visible.equals(text))UiLabelTooltips.record(g,text,center-width/2,y,width,font.lineHeight);
        g.pose().pushPose();g.pose().translate(center,y,0);g.pose().scale(scale,scale,1);g.drawString(font,visible,-font.width(visible)/2,0,color,false);g.pose().popPose();
    }
    private static Component fit(Font font,Component text,int width){
        return font.width(text)<=width?text:Component.literal(font.plainSubstrByWidth(text.getString(),Math.max(0,width-font.width("...")))+"...").withStyle(text.getStyle());
    }
}

package com.mcmagic.omnira.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class CrystalUi {
    private CrystalUi(){}
    static MachinePanelArt.Pixels pixels(GuiGraphics g,int x,int y){return (a,b,c,d,color)->g.fill(x+a,y+b,x+c,y+d,color);}
    static float time(float partial){var level=Minecraft.getInstance().level;return level==null?0:(level.getGameTime()%24000)+partial;}
    static void title(GuiGraphics g,Font font,Component text,int center,int y,int width,int color){
        float scale=Math.min(1,width/(float)Math.max(1,font.width(text)));g.pose().pushPose();
        g.pose().translate(center,y,0);g.pose().scale(scale,scale,1);g.drawString(font,text,-font.width(text)/2,0,color,false);g.pose().popPose();
    }
}

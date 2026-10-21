package com.mcmagic.omnira.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Keep recipe labels inside their allotted area without resizing the recipe page. */
final class JeiText {
    static void wrapped(GuiGraphics g,Component text,int x,int y,int width,int height,int color){
        var font=Minecraft.getInstance().font;var lines=font.split(text,width);int count=Math.min(lines.size(),height/font.lineHeight);
        for(int i=0;i<count;i++){
            if(i==count-1&&lines.size()>count)g.drawString(font,"...",x,y+i*font.lineHeight,color,false);
            else g.drawString(font,lines.get(i),x,y+i*font.lineHeight,color,false);
        }
    }
    private JeiText(){}
}

package com.mcmagic.omnira.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Compact crystal controls share focus, disabled and hover treatment. */
final class CrystalActionButton extends Button {
    CrystalActionButton(int x,int y,int width,int height,Component label,OnPress action){super(x,y,width,height,label,action,DEFAULT_NARRATION);}
    @Override protected void renderWidget(GuiGraphics g,int x,int y,float partial){
        MachinePanelArt.button(CrystalUi.pixels(g,getX(),getY()),getWidth(),getHeight(),active,isHoveredOrFocused());
        var font=Minecraft.getInstance().font;
        CrystalUi.title(g,font,getMessage(),getX()+getWidth()/2,getY()+(getHeight()-font.lineHeight)/2,getWidth()-8,active?getFGColor():0xFF9EADB3);
    }
}

package com.mcmagic.omnira.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TravelerManuscriptScreen extends Screen {
    private static final int PAGES=4, PANEL_WIDTH=188, PANEL_HEIGHT=218;
    private int page;
    private Button previous,next;

    public TravelerManuscriptScreen() {super(Component.translatable("item.omnira.traveler_manuscript"));}

    @Override protected void init() {
        int left=(width-PANEL_WIDTH)/2,top=(height-PANEL_HEIGHT)/2;
        previous=pageButton(left+22,top+186,"<",-1);
        next=pageButton(left+144,top+186,">",1);
        updateButtons();
    }

    private Button pageButton(int x,int y,String glyph,int direction) {
        return addRenderableWidget(new Button(x,y,22,18,Component.literal(glyph),button->turn(direction),supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
                int rim=active?0xFF648FA3:0xFF9AAFB7;
                int face=active?(isHoveredOrFocused()?0xFFC5EBF2:0xFFA8D7E2):0xFFCFDADC;
                graphics.fill(getX()+3,getY(),getX()+19,getY()+18,rim);
                graphics.fill(getX(),getY()+3,getX()+22,getY()+15,rim);
                graphics.fill(getX()+3,getY()+2,getX()+19,getY()+16,face);
                graphics.fill(getX()+2,getY()+4,getX()+20,getY()+14,face);
                graphics.fill(getX()+4,getY()+3,getX()+18,getY()+4,0xFFE6F7F7);
                graphics.drawCenteredString(font,getMessage(),getX()+11,getY()+5,active?0xFF315B70:0xFF879DA4);
            }
        });
    }

    private void turn(int direction) {
        page+=direction;
        if(page==PAGES-1)net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new com.mcmagic.omnira.network.ManuscriptReadPayload());
        updateButtons();
    }

    private void updateButtons() {
        previous.active=page>0;
        next.active=page<PAGES-1;
    }

    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {}

    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        graphics.fill(0,0,width,height,0xAA101821);
        int x=(width-PANEL_WIDTH)/2,y=(height-PANEL_HEIGHT)/2;
        graphics.fill(x+8,y,x+180,y+218,0xFF608CA2);
        graphics.fill(x+4,y+4,x+184,y+214,0xFF89B6C7);
        graphics.fill(x,y+8,x+188,y+210,0xFF89B6C7);
        graphics.fill(x+9,y+5,x+179,y+213,0xFFD4EAF0);
        graphics.fill(x+5,y+9,x+183,y+209,0xFFD4EAF0);
        graphics.fill(x+14,y+14,x+174,y+204,0xFFE9F4F3);
        graphics.fill(x+18,y+35,x+170,y+36,0xFF93BBC5);
        graphics.drawCenteredString(font,title,x+94,y+19,0xFF28445D);
        int lineY=y+45;
        for(var line:font.split(Component.translatable("gui.omnira.traveler_manuscript.page."+(page+1)),144)) {
            graphics.drawString(font,line,x+22,lineY,0xFF28445D,false);
            lineY+=11;
        }
        graphics.drawCenteredString(font,(page+1)+" / "+PAGES,x+94,y+191,0xFF4A7789);
        super.render(graphics,mouseX,mouseY,partialTick);
    }
}

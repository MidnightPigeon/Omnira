package com.mcmagic.omnira.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ResponsiveScreen extends Screen {
    private float uiScale=1;
    private final ScaledTooltip widgetTooltip=new ScaledTooltip();
    protected ResponsiveScreen(Component title){super(title);}
    protected abstract int minimumWidth();
    protected abstract int minimumHeight();
    @Override protected void init(){
        uiScale=UiViewport.fit(width,height,minimumWidth(),minimumHeight());
        width=Math.round(width/uiScale);height=Math.round(height/uiScale);
        super.init();
    }
    @Override public final void render(GuiGraphics g,int x,int y,float partial){
        widgetTooltip.begin();
        g.pose().pushPose();g.pose().scale(uiScale,uiScale,1);
        try{renderContent(g,(int)(x/uiScale),(int)(y/uiScale),partial);widgetTooltip.render(g,font,(int)(x/uiScale),(int)(y/uiScale));}finally{g.pose().popPose();}
    }
    @Override public void setTooltipForNextRenderPass(java.util.List<net.minecraft.util.FormattedCharSequence> text,net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner,boolean override){widgetTooltip.queue(text,positioner,override);}
    protected void renderContent(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);}
    @Override public final boolean mouseClicked(double x,double y,int b){return mouseClickedContent(x/uiScale,y/uiScale,b);}
    protected boolean mouseClickedContent(double x,double y,int b){return super.mouseClicked(x,y,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return mouseReleasedContent(x/uiScale,y/uiScale,b);}
    protected boolean mouseReleasedContent(double x,double y,int b){return super.mouseReleased(x,y,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return mouseDraggedContent(x/uiScale,y/uiScale,b,dx/uiScale,dy/uiScale);}
    protected boolean mouseDraggedContent(double x,double y,int b,double dx,double dy){return super.mouseDragged(x,y,b,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return super.mouseScrolled(x/uiScale,y/uiScale,dx,dy);}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/uiScale,y/uiScale);}
}

package com.mcmagic.omnira.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keep layout coordinates stable while drawing and input share one viewport. */
public abstract class ResponsiveContainerScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private float uiScale=1;
    private final ScaledTooltip widgetTooltip=new ScaledTooltip();
    private int physicalWidth,physicalHeight;
    protected ResponsiveContainerScreen(M menu,Inventory inventory,Component title){super(menu,inventory,title);}
    protected int leftExtension(){return 0;}
    protected int rightExtension(){return 0;}
    @Override protected void init(){
        physicalWidth=width;physicalHeight=height;
        uiScale=UiViewport.fit(width,height,imageWidth+leftExtension()+rightExtension(),imageHeight);
        width=Math.round(width/uiScale);height=Math.round(height/uiScale);
        super.init();
        leftPos=(width-imageWidth-leftExtension()-rightExtension())/2+leftExtension();
    }
    public float uiScale(){return uiScale;}
    /** GuiGraphics scissor coordinates do not inherit the current pose transform. */
    protected final void enableUiScissor(GuiGraphics g,int x,int y,int xx,int yy){
        var bounds=UiViewport.scissor(g.pose().last().pose(),x,y,xx,yy);
        g.enableScissor(bounds[0],bounds[1],bounds[2],bounds[3]);
    }
    public int physicalWidth(){return physicalWidth;}
    public int physicalHeight(){return physicalHeight;}
    public Rect2i physicalBounds(){return physicalRect(leftPos-leftExtension(),topPos,imageWidth+leftExtension()+rightExtension(),imageHeight);}
    public Rect2i physicalRect(int x,int y,int w,int h){
        int left=(int)Math.floor(x*uiScale),top=(int)Math.floor(y*uiScale);
        return new Rect2i(left,top,(int)Math.ceil((x+w)*uiScale)-left,(int)Math.ceil((y+h)*uiScale)-top);
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
    @Override public final boolean mouseReleased(double x,double y,int b){return mouseReleasedContent(x/uiScale,y/uiScale,b);}
    protected boolean mouseReleasedContent(double x,double y,int b){return super.mouseReleased(x,y,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged(x/uiScale,y/uiScale,b,dx/uiScale,dy/uiScale);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return super.mouseScrolled(x/uiScale,y/uiScale,dx,dy);}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/uiScale,y/uiScale);}
}

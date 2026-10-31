package com.mcmagic.omnira.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Native button backgrounds and input handling, with consistent pixel icons. */
public final class PixelButtons {
    private PixelButtons() {}
    public static Builder builder(Component message,Button.OnPress action){return new Builder(message,action);}
    public static final class Builder {
        private final Component message;private final Button.OnPress action;
        private int x,y,w,h;private Tooltip tooltip;
        private Builder(Component message,Button.OnPress action){this.message=message;this.action=action;}
        public Builder bounds(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
        public Builder tooltip(Tooltip tooltip){this.tooltip=tooltip;return this;}
        public Button build(){
            var button=new Button(x,y,w,h,message,action,supplier->supplier.get()){
                @Override public void renderString(GuiGraphics g,Font font,int color){
                    if(!PixelUiIcons.draw(g::fill,getMessage().getString(),getX()+getWidth()/2,getY()+getHeight()/2,color))super.renderString(g,font,color);
                }
            };
            if(tooltip==null){
                String key=switch(message.getString()){case "<"->"guide.omnira.previous";case ">"->"guide.omnira.next";case "X","x","×"->"codex.omnira.close";default->null;};
                if(key!=null)tooltip=Tooltip.create(Component.translatable(key));
            }
            button.setTooltip(tooltip);return button;
        }
    }
}

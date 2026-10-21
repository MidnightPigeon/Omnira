package com.mcmagic.omnira.client.guide;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class GuideFigureRenderer {
    public record Hover(ItemStack item,String text) {}
    private final java.util.Map<GuideFigure,java.util.List<java.util.List<String>>> linesByFigure=new java.util.IdentityHashMap<>();
    public void clear(){linesByFigure.clear();}
    public Hover render(GuiGraphics g,Font font,GuideFigure figure,int x,int y,int mouseX,int mouseY) {
        var wrapped=linesByFigure.computeIfAbsent(figure,value->value.labels().stream().map(label->GuideTextLayout.wrap(label.text(),label.width(),font::width)).toList());
        var hovered=ItemStack.EMPTY;
        String tooltip="";
        g.enableScissor(x,y,x+GuideFigure.WIDTH,y+GuideFigure.HEIGHT);
        for(var a:figure.areas())g.fill(x+a.x(),y+a.y(),x+a.x()+a.width(),y+a.y()+a.height(),a.color());
        for(var s:figure.strokes()) {
            line(g,x+s.x1(),y+s.y1(),x+s.x2(),y+s.y2(),s.color());
            if(s.arrow()) {
                double angle=Math.atan2(s.y2()-s.y1(),s.x2()-s.x1());
                for(double turn:new double[]{-.55,.55})line(g,x+s.x2(),y+s.y2(),x+s.x2()-(int)Math.round(4*Math.cos(angle+turn)),y+s.y2()-(int)Math.round(4*Math.sin(angle+turn)),s.color());
            }
        }
        for(var icon:figure.icons()) {
            var stack=GuideContents.icon(icon.item());
            if(stack.isEmpty())continue;
            stack.setCount(icon.count());g.renderItem(stack,x+icon.x(),y+icon.y());g.renderItemDecorations(font,stack,x+icon.x(),y+icon.y());
            if(mouseX>=x+icon.x()&&mouseX<x+icon.x()+16&&mouseY>=y+icon.y()&&mouseY<y+icon.y()+16)hovered=stack;
        }
        for(int index=0;index<figure.labels().size();index++) {
            var label=figure.labels().get(index);var lines=wrapped.get(index);
            if((lines.size()-1)*11+font.lineHeight>label.height()&&mouseX>=x+label.x()&&mouseX<x+label.x()+label.width()&&mouseY>=y+label.y()&&mouseY<y+label.y()+label.height())tooltip=label.text();
            g.enableScissor(x+label.x(),y+label.y(),x+label.x()+label.width(),y+label.y()+label.height());
            for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),x+label.x(),y+label.y()+i*11,label.color(),false);
            g.disableScissor();
        }
        g.disableScissor();return new Hover(hovered,tooltip);
    }
    private static void line(GuiGraphics g,int x,int y,int x2,int y2,int color) {
        int dx=Math.abs(x2-x),dy=-Math.abs(y2-y),sx=x<x2?1:-1,sy=y<y2?1:-1,error=dx+dy;
        while(true) {
            g.fill(x,y,x+1,y+1,color);if(x==x2&&y==y2)break;
            int e=error*2;if(e>=dy){error+=dy;x+=sx;}if(e<=dx){error+=dx;y+=sy;}
        }
    }
}

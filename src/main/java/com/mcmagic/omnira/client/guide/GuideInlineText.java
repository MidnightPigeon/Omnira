package com.mcmagic.omnira.client.guide;

import java.util.*;
import java.util.function.Function;
import net.minecraft.client.gui.*;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.*;

/** Item mentions are atomic during wrapping and expand back to the author's spelling for display. */
public final class GuideInlineText {
    private final Font font;
    public final GuideMentionLayout layout;
    public GuideInlineText(Font font){
        this.font=font;
        layout=new GuideMentionLayout(GuideItemCatalog.current(),font::width);
    }
    private record Icon(int index,int x,ItemStack item){}
    public final class Line {
        private final FormattedCharSequence glyphs;
        private final String plain;
        private final List<Icon> icons=new ArrayList<>();
        public final int width;
        private Line(String encoded,Function<String,FormattedCharSequence> style){
            var marked=style.apply(layout.expand(encoded,true));
            var text=new StringBuilder();
            float[] advance={0};
            marked.accept((i,s,cp)->{
                var t=layout.token(cp);
                if(t!=null)icons.add(new Icon(text.length(),Math.round(advance[0]),new ItemStack(t.item())));
                else text.appendCodePoint(cp);
                advance[0]+=font.getSplitter().stringWidth(FormattedCharSequence.forward(t!=null?"  ":Character.toString(cp),s));
                return true;
            });
            plain=text.toString();width=(int)Math.ceil(advance[0]);
            glyphs=icons.isEmpty()?marked:sink->marked.accept((i,s,cp)->layout.token(cp)==null?sink.accept(i,s,cp):
                    sink.accept(i,s,' ') && sink.accept(i,s,' '));
        }
        public String plain(){return plain;}
        public String segment(int start,int end){
            var result=new StringBuilder();
            int previous=start;
            for(var icon:icons)if(icon.index>=start && icon.index<end){
                result.append(plain,previous,icon.index).append("  ");
                previous=icon.index;
            }
            return result.append(plain,previous,end).toString();
        }
        public int offset(int index){
            int[] at={0};float[] x={0};
            glyphs.accept((i,s,cp)->{
                if(at[0]>=index)return false;
                x[0]+=font.getSplitter().stringWidth(FormattedCharSequence.forward(Character.toString(cp),s));
                at[0]+=Character.charCount(cp);return true;
            });
            return Math.round(x[0]);
        }
        public ItemStack iconAt(double localX,double localY){
            if(localY>=1 && localY<8)for(var icon:icons)
                if(localX>=icon.x && localX<icon.x+7)return icon.item;
            return ItemStack.EMPTY;
        }
        public boolean hasIcons(){return !icons.isEmpty();}
        public void draw(GuiGraphics g,int x,int y,int color){
            g.drawString(font,glyphs,x,y,color,false);
            for(var icon:icons){
                g.pose().pushPose();
                g.pose().translate(x+icon.x,y+1,0);
                g.pose().scale(7.0F/16,7.0F/16,1);
                g.renderItem(icon.item,0,0);
                g.pose().popPose();
            }
        }
    }
    public Line line(String encoded,Function<String,FormattedCharSequence> style){return new Line(encoded,style);}
}

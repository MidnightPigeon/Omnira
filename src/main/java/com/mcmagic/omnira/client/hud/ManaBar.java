package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.mana.ManaState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Shared, integer-pixel crystal-ended meter for the HUD and workstation menus. */
public final class ManaBar {
    private ManaBar() {}
    public static void render(GuiGraphics g, Font font, ManaState mana, int x, int y, int width) {
        render(g,font,mana,x,y,width,true);
    }
    public static void render(GuiGraphics g, Font font, ManaState mana, int x, int y, int width,boolean numbers) {
        ManaBarTooltips.record(g,mana,x,y,width);
        int color=0xFF000000|mana.color(), left=x+5, right=x+width-5;
        g.fill(left,y+1,right,y+12,0xFF343139);
        g.fill(left+1,y+2,right-1,y+11,0xFFB7B3BD);
        g.fill(left+2,y+3,right-2,y+10,0xFF24222A);
        int fill=(int)Math.round((right-left-4)*Mth.clamp(mana.current()/mana.maximum(),0,1));
        if(fill>0) {
            g.fill(left+2,y+3,left+2+fill,y+10,color);
            g.fill(left+2,y+9,left+2+fill,y+10,0xFF939098);
            if(mana.affinity()==com.mcmagic.omnira.mana.Affinity.DREAM.ordinal()) {
                double phase=net.minecraft.Util.getMillis()/180.0;
                for(int i=0;i<fill;i++) {
                    double wave=(Math.sin(i*.35-phase)+1)*.5;
                    int r=(int)(126+54*wave),green=(int)(155-23*wave),b=(int)(249-10*wave);
                    g.fill(left+2+i,y+3,left+3+i,y+10,0xFF000000|(r<<16)|(green<<8)|b);
                    int crest=y+4+(int)Math.round(wave*3);
                    g.fill(left+2+i,crest,left+3+i,crest+1,0xFFD4D5FF);
                }
            }
        }
        for(int tip : new int[]{x+2,x+width-3}) {
            g.fill(tip,y,tip+1,y+13,0xFF343139);
            g.fill(tip-1,y+2,tip+2,y+11,0xFF343139);
            g.fill(tip-2,y+4,tip+3,y+9,0xFF343139);
            g.fill(tip,y+2,tip+1,y+11,color);
            g.fill(tip-1,y+5,tip+2,y+8,color);
            switch(com.mcmagic.omnira.mana.Affinity.byId(mana.affinity())) {
                case LIGHT -> {
                    g.fill(tip-3,y+6,tip+4,y+7,0xFFFFE5A3);
                    g.fill(tip,y-1,tip+1,y+2,0xFFFFF4D8);
                    g.fill(tip,y+11,tip+1,y+14,0xFFFFF4D8);
                }
                case DARK -> {
                    g.fill(tip,y+3,tip+2,y+10,0xFF342744);
                    g.fill(tip-2,y+2,tip,y+4,0xFFD0B7EB);
                    g.fill(tip-2,y+9,tip,y+11,0xFFD0B7EB);
                }
                case ELEMENTAL -> {
                    g.fill(tip-1,y,tip+1,y+2,0xFFE5B76B);
                    g.fill(tip-3,y+5,tip-1,y+7,0xFF6CBCEC);
                    g.fill(tip+2,y+5,tip+4,y+7,0xFFF28D81);
                    g.fill(tip-1,y+11,tip+1,y+13,0xFFACDFB4);
                }
                case ARCANE -> { // Faceted violet focus with four cardinal contacts.
                    g.fill(tip-2,y+3,tip+3,y+10,0xFF62427F);
                    for(int d=-2;d<=2;d++){
                        int half=2-Math.abs(d);
                        g.fill(tip-half,y+6+d,tip+half+1,y+7+d,0xFFF4DFFF);
                    }
                    g.fill(tip,y+1,tip+1,y+3,color);g.fill(tip,y+10,tip+1,y+12,color);
                }
                case SPACETIME -> { // Two opposed wedges meeting at an hourglass waist.
                    for(int d=0;d<3;d++){
                        g.fill(tip-2+d,y+3+d,tip+3-d,y+4+d,0xFFBBF8A7);
                        g.fill(tip-2+d,y+9-d,tip+3-d,y+10-d,0xFF9AC9FF);
                    }
                    g.fill(tip,y+6,tip+1,y+7,0xFFF3FFFF);
                }
                case DREAM -> {
                    g.fill(tip-2,y+3,tip+2,y+5,0xFF88B5FF);
                    g.fill(tip-1,y+8,tip+3,y+10,0xFFC49DF0);
                    g.fill(tip,y+5,tip+1,y+8,0xFFE3DFFF);
                }
                default -> {}
            }
        }
        if(mana.current()>mana.maximum())g.fill(left+2,y+1,right-2,y+2,mana.pulseOverloaded()?0xFFF28282:0xFFF1C76B);
        if(!numbers)return;
        String label=(int)mana.current()+"/"+(int)mana.maximum();
        float scale=Math.min(.75F,(width-14F)/Math.max(1,font.width(label)));
        g.pose().pushPose();
        g.pose().translate(x+width/2F,y+3.5F,0);
        g.pose().scale(scale,scale,1);
        g.drawString(font,label,-font.width(label)/2,0,0xFFFFFFFF,true);
        g.pose().popPose();
    }
}

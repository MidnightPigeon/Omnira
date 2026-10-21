package com.mcmagic.omnira.client.screen;

/** The codex has its own binding and illumination, not a tinted guidebook texture. */
public final class CodexBookArt {
    public static final int[] ELEMENTS={0xFFC78A3F,0xFF488FC1,0xFFCB5A58,0xFF4C9C87,
            0xFFBA9A36,0xFF9363AD,0xFF77A052,0xFF6078B8};
    public static void spread(CodexControlArt.Pixels p){
        polygon(p,0xFF432D4E,3,15,28,7,132,7,160,17,188,7,292,7,317,15,317,217,291,209,188,209,160,219,132,209,29,209,3,217);
        polygon(p,0xFFA66876,6,16,29,10,132,10,160,20,188,10,291,10,314,16,314,214,291,206,188,206,160,216,132,206,29,206,6,214);
        polygon(p,0xFFE7C77D,9,17,30,12,132,12,160,22,188,12,290,12,311,17,311,210,290,204,188,204,160,213,132,204,30,204,9,210);
        polygon(p,0xFFF1F0E2,12,18,31,14,132,14,157,23,157,207,132,201,31,201,12,206);
        polygon(p,0xFFE8EFEB,163,23,188,14,289,14,308,18,308,206,289,201,188,201,163,207);
        p.fill(153,24,155,206,0xFFD9D2BB);p.fill(155,24,157,208,0xFFC2BBA8);
        p.fill(157,25,160,210,0xFF8A8690);p.fill(160,25,163,210,0xFFB9B9BC);
        p.fill(163,24,165,207,0xFFD6DFD7);
        for(int side=0;side<2;side++){
            int x=side*160;
            p.fill(x+23,38,x+138,39,0xFFBA9B62);
            p.fill(x+27,40,x+134,41,0x557DA5A5);
            corner(p,x+19,46,1,1);corner(p,x+143,46,-1,1);
            corner(p,x+19,199,1,-1);corner(p,x+143,199,-1,-1);
        }
        for(int i=0;i<8;i++){
            int x=i<4?7:312,y=48+(i%4)*43;
            gem(p,x,y,ELEMENTS[i]);
        }
        for(int y:new int[]{51,102,153}){p.fill(158,y,162,y+9,0xFFE0C273);p.fill(159,y+2,161,y+7,0xFFFFEDB2);}
        // Open-page constellations stay in the margins, clear of text, slots and buttons.
        for(int side=0;side<2;side++)for(int i=0;i<3;i++){
            int x=side*160+28+i*49,y=48+(i%2)*2;
            star(p,x,y,ELEMENTS[side*4+i]);
        }
        p.fill(18,210,42,212,0xFF4D9C9D);p.fill(278,210,302,212,0xFF9C6FA3);
    }
    private static void corner(CodexControlArt.Pixels p,int x,int y,int sx,int sy){
        for(int i=0;i<13;i++){p.fill(x+sx*i,y,x+sx*i+1,y+1,0xFFB9A56E);p.fill(x,y+sy*i,x+1,y+sy*i+1,0xFFB9A56E);}
        star(p,x+sx*5,y+sy*5,0xFF69A5A0);
    }
    private static void gem(CodexControlArt.Pixels p,int x,int y,int color){
        polygon(p,0xFFECD48E,x,y-5,x+4,y,x,y+5,x-4,y);
        polygon(p,color,x,y-3,x+2,y,x,y+3,x-2,y);p.fill(x-1,y-2,x,y,0xFFFFF5D2);
    }
    private static void star(CodexControlArt.Pixels p,int x,int y,int color){
        p.fill(x-2,y,x+3,y+1,color);p.fill(x,y-2,x+1,y+3,color);p.fill(x,y,x+1,y+1,0xFFFDF8D9);
    }
    private static void polygon(CodexControlArt.Pixels p,int color,int... xy){MachinePanelArt.polygon(p::fill,color,xy);}
    private CodexBookArt(){}
}

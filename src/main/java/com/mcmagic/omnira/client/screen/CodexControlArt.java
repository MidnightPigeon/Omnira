package com.mcmagic.omnira.client.screen;

/** Fixed pixel shapes are transformed as a whole; animation never rounds moving coordinates. */
public final class CodexControlArt {
    public enum Icon { SPHERE, CUBE, PLANE, EXECUTE, CLOSE }
    @FunctionalInterface public interface Pixels {void fill(int x,int y,int xx,int yy,int color);}
    private static final String[][] RUNES={
            {"  #  "," # # ","# # #","  #  "," # # ","#   #","  #  "},
            {"#   #"," # # ","  #  ","#####","  #  "," # # ","#   #"},
            {" ### ","#   #","  #  "," ### ","  #  ","#   #"," ### "},
            {"# # #","# # #"," ### ","  #  "," ### ","# # #","# # #"},
            {"  #  "," ### ","#   #","# # #","#   #"," ### ","  #  "},
            {"#####"," # # ","# # #","  #  ","# # #"," # # ","#####"},
            {"#   #","## ##"," # # ","  #  "," # # ","## ##","#   #"},
            {" ### ","  #  ","# # #"," ### ","# # #","  #  "," ### "}
    };
    public static final int RADIUS=58,RUNE_RADIUS=51;
    public static double angle(double ticks){return ticks*.16;}
    public static int runeColor(double ticks,int index){int alpha=(int)Math.round(190+45*Math.sin(ticks*.025+index*.7));return alpha<<24|(CodexBookArt.ELEMENTS[Math.floorMod(index,8)]&0xFFFFFF);}
    public static void ring(Pixels p){
        MachinePanelArt.ring(p::fill,0,0,RADIUS,RADIUS,0xB8AA8651);
        MachinePanelArt.ring(p::fill,0,0,RADIUS-2,RADIUS-2,0x50659499);
        for(int i=0;i<16;i++){
            double a=i*Math.PI/8;
            int x=(int)Math.round(Math.cos(a)*(RADIUS-6)),y=(int)Math.round(Math.sin(a)*(RADIUS-6));
            p.fill(x,y,x+1,y+1,0x70416C83);
        }
    }
    public static void rune(Pixels p,int index,int color){
        var glyph=RUNES[Math.floorMod(index,RUNES.length)];
        for(int y=0;y<7;y++)for(int x=0;x<5;x++)if(glyph[y].charAt(x)=='#')p.fill(x-2,y-3,x-1,y-2,color);
    }
    public static void button(Pixels p,int width,int height,boolean active,boolean hover,boolean selected){
        MachinePanelArt.frame(p::fill,0,0,width,height,!active?0xFF9DAEB8:selected?0xFFDCE8E5:hover?0xFFDCEAF2:0xFFBECFD9);
        p.fill(3,3,width-3,4,0xFFE8F4F3);
        p.fill(3,height-4,width-3,height-3,selected?0xFF4C8A8C:0xFF7F9CAA);
        if(selected){p.fill(2,5,3,height-5,0xFFBA9456);p.fill(width-3,5,width-2,height-5,0xFFBA9456);}
    }
    public static void icon(Pixels p,Icon icon,int color){
        switch(icon){
            case SPHERE->{MachinePanelArt.ring(p::fill,0,0,6,6,color);MachinePanelArt.ring(p::fill,0,0,3,6,color);MachinePanelArt.line(p::fill,-5,0,5,0,color);}
            case CUBE->{
                line(p,-6,-3,0,-6,color);line(p,0,-6,6,-3,color);line(p,6,-3,0,0,color);line(p,0,0,-6,-3,color);
                line(p,-6,-3,-6,4,color);line(p,6,-3,6,4,color);line(p,0,0,0,7,color);line(p,-6,4,0,7,color);line(p,0,7,6,4,color);
            }
            case PLANE->{line(p,-7,1,-2,-4,color);line(p,-2,-4,7,-1,color);line(p,7,-1,2,4,color);line(p,2,4,-7,1,color);line(p,-3,0,3,1,0xFF82A9B9);}
            case EXECUTE->{line(p,-5,0,4,0,color);line(p,1,-4,5,0,color);line(p,5,0,1,4,color);p.fill(-7,-2,-6,3,color);}
            case CLOSE->{line(p,-3,-3,3,3,color);line(p,-3,3,3,-3,color);}
        }
    }
    private static void line(Pixels p,int x,int y,int xx,int yy,int c){MachinePanelArt.line(p::fill,x,y,xx,yy,c);}
    public static void slot(Pixels p){
        p.fill(-2,-2,18,18,0xFF567584);p.fill(-1,-1,17,17,0xFF829DA9);
        p.fill(0,0,16,16,0xFFC7D8DF);p.fill(1,1,15,2,0xFF9EB7C2);
        p.fill(0,15,16,16,0xFFF0F6F3);p.fill(15,1,16,15,0xFFE3EFEC);
    }
    public static void checkbox(Pixels p,boolean checked){
        p.fill(0,0,10,10,0xFF466679);p.fill(1,1,9,9,0xFFE0ECEE);
        if(checked){line(p,2,5,4,7,0xFF326B72);line(p,4,7,8,2,0xFF326B72);}
    }
    private CodexControlArt(){}
}

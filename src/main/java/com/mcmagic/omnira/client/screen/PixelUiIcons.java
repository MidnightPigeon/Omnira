package com.mcmagic.omnira.client.screen;

/** Small, font-independent glyphs; coordinates describe a centered 9-pixel footprint. */
public final class PixelUiIcons {
    private PixelUiIcons() {}
    public static boolean draw(MachinePanelArt.Pixels p,String symbol,int cx,int cy,int color) {
        switch(symbol) {
            case "tree" -> {
                p.fill(cx-1,cy-1,cx+1,cy+5,color);
                p.fill(cx-4,cy-1,cx+4,cy+1,color);
                p.fill(cx-4,cy-4,cx-2,cy,color);p.fill(cx+2,cy-4,cx+4,cy,color);
                p.fill(cx-5,cy-5,cx-1,cy-3,color);p.fill(cx+1,cy-5,cx+5,cy-3,color);
                p.fill(cx-2,cy-5,cx+2,cy-2,color);
            }
            case "<", ">" -> {
                int direction=symbol.equals(">")?1:-1;
                for(int i=0;i<5;i++)p.fill(cx+direction*(2-i),cy-i,cx+direction*(2-i)+2,cy+i+1,color);
            }
            case "X", "x", "×" -> {
                for(int i=-3;i<=3;i++){p.fill(cx+i,cy+i,cx+i+1,cy+i+1,color);p.fill(cx+i,cy-i,cx+i+1,cy-i+1,color);}
            }
            case "home" -> {
                p.fill(cx-3,cy-2,cx-1,cy+4,color);p.fill(cx-3,cy+2,cx+4,cy+4,color);
                for(int i=0;i<3;i++)p.fill(cx-2-i,cy-4+i,cx-1+i,cy-3+i,color);
            }
            case "⇄" -> {
                p.fill(cx-4,cy-2,cx+4,cy-1,color);p.fill(cx-4,cy+2,cx+4,cy+3,color);
                for(int i=0;i<3;i++){
                    p.fill(cx+2+i,cy-4+i,cx+3+i,cy+1-i,color);
                    p.fill(cx-4+i,cy+2-i,cx-3+i,cy+3+i,color);
                }
            }
            case "||" -> {p.fill(cx-3,cy-3,cx-1,cy+4,color);p.fill(cx+1,cy-3,cx+3,cy+4,color);}
            default -> {return false;}
        }
        return true;
    }
    public static void arrow(MachinePanelArt.Pixels p,int x,int y,int width,int color) {
        p.fill(x,y-1,x+width-4,y+1,color);
        for(int i=0;i<5;i++)p.fill(x+width-5+i,y-4+i,x+width-4+i,y+5-i,color);
    }
}

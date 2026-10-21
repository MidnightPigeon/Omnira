package com.mcmagic.omnira.client.renderer;

import java.util.*;

/** Shared native dream-door pixels, also usable by the offline miniature preview. */
public final class DreamPortalPattern {
    public record Cell(int x,int y,double radius,double angle){}
    public static final List<Cell> CELLS=cells();
    private static List<Cell> cells(){
        var cells=new ArrayList<Cell>();for(int x=-8;x<8;x++)for(int y=-8;y<8;y++){
            double radius=Math.hypot(x+.5,y+.5);if(radius<=7.8)cells.add(new Cell(x,y,radius,Math.atan2(y+.5,x+.5)));
        }return List.copyOf(cells);
    }
    public static int color(Cell cell,double time){
        double wave=Math.sin(cell.angle*3-cell.radius*.85+time*.11);
        int alpha=(int)((wave>.15?.78:.20)*Math.min(1,(8-cell.radius)*1.6)*255);
        int color=cell.radius<1.6?0xF8F2FF:wave>.65?0xD6F7F4:wave>.15?0xAEA3DC:0x657D99;
        return alpha<<24|color;
    }
    private DreamPortalPattern(){}
}

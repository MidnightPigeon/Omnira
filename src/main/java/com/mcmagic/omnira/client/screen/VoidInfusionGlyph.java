package com.mcmagic.omnira.client.screen;

final class VoidInfusionGlyph {
    private static final String[] SHAPE={
        "     .++++.     ",
        "   .+++..+++.   ",
        "  .++.    .++.  ",
        " .++.  ++  .++. ",
        ".++.  +##+  .++.",
        ".+.  +####+  .+.",
        ".+.  +####+  .+.",
        ".++.  +##+  .++.",
        " .++.  ++  .++. ",
        "  .++.    .++.  ",
        "   .+++..+++.   ",
        "     .++++.     "
    };
    private static final int CORE=0xFF191326,EDGE=0xFF493658,RIM=0xFF9673B2,
            DORMANT=0xFF6D4D87,CHARGED=0xFFE0B6FA;

    static void draw(MachinePanelArt.Pixels pixels,int x,int y,int progress,int duration){
        int width=SHAPE[0].length();
        int filled=width*Math.clamp(progress,0,duration)/duration;
        for(int row=0;row<SHAPE.length;row++)for(int col=0;col<width;col++){
            int color=switch(SHAPE[row].charAt(col)){
                case '#'->CORE;
                case '.'->col<filled?RIM:EDGE;
                case '+'->col<filled?CHARGED:DORMANT;
                default->0;
            };
            if(color!=0)pixels.fill(x+col,y+row,x+col+1,y+row+1,color);
        }
    }
    private VoidInfusionGlyph(){}
}

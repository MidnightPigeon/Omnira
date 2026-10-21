package com.mcmagic.omnira.client.screen;

/** Shared pixel geometry for pulse readouts and headless layout checks. */
public final class PulseMeterArt {
    public static final int PRESENT=0xFF81D4FA, RESERVED=0xFFF1C76B;
    public static final int SHELL_X=38,SHELL_Y=14,SHELL_SIZE=100;
    private static final String[] FACETS={
        "2222333322223333", "1122332211223322", "0011222100112221", "0001121100011211",
        "0000111000001110", "1000010010000100", "2100000121000001", "1111000011110000"
    };
    private static final int[][] PALETTES={
        {0xFF182D3B,0xFF203B4A,0xFF2C4B59,0xFF416372},
        {0xFF347EAB,0xFF4BA4CC,PRESENT,0xFFCCF3FF},
        {0xFF9D6830,0xFFC28F42,RESERVED,0xFFFFEABD}
    };
    public static int texel(int layer,int x,int y){return PALETTES[layer][FACETS[y&7].charAt(x&15)-'0'];}
    @FunctionalInterface public interface Pixels { void fill(int x,int y,int right,int bottom,int color); }
    public static void bar(Pixels p,int width,double current,double occupied,double limit){
        int length=Math.max(0,width-2);
        p.fill(0,0,length+2,10,0xFFD0E5ED);p.fill(1,1,length+1,9,0xFF263C4A);
        int filled=(int)Math.round(length*fraction(current+occupied,limit)),used=(int)Math.round(length*fraction(occupied,limit));
        for(int x=0;x<length;x++)for(int y=0;y<8;y++)p.fill(x+1,y+1,x+2,y+2,texel(x<used?2:x<filled?1:0,x,y));
    }
    public static void ring(Pixels p,int radius,double amount,double limit,int color){
        int count=(int)Math.ceil(24*fraction(amount,limit));
        for(int i=0;i<24;i++){double a=i*Math.PI/12;int x=(int)Math.round(Math.cos(a)*radius),y=(int)Math.round(Math.sin(a)*radius);
            p.fill(x-1,y-1,x+1,y+1,i<count?color:0xFF456174);}
    }
    private static double fraction(double amount,double limit){return limit>0?Math.clamp(amount/limit,0,1):0;}
    private PulseMeterArt(){}
}

package com.mcmagic.omnira.client.hud;

/** Pixel-only moving filaments. Pure sampler is also used by offline previews. */
public final class ManaBandStyle {
    public static int pixel(boolean chaos,int x,int y,double seconds){
        double phase=seconds*(chaos?.85:.6);
        double wave=Math.sin(x*(chaos?.24:.18)-phase*2);
        double other=Math.sin(x*(chaos?.37:.18)+phase*1.4+(chaos?1.1:Math.PI));
        int a=(int)Math.round(2.5+wave*1.8),b=(int)Math.round(2.5+other*1.8);
        double blend=(Math.sin(x*.09-phase)+1)*.5;
        int base=mix(chaos?0x768F9C:0x5E8D96,chaos?0xB27F9C:0x6A9C83,blend);
        if(y==a)return chaos?0xFF9EE8E0:0xFFBBF8A7;
        if(y==b)return chaos?0xFFF5AEDC:0xFF9AC9FF;
        return 0xFF000000|base;
    }
    private static int mix(int a,int b,double t){
        return 0xFF000000|((int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16)
                |((int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8)|(int)((a&255)*(1-t)+(b&255)*t);
    }
    private ManaBandStyle(){}
}

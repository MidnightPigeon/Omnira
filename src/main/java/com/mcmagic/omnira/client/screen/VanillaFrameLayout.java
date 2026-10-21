package com.mcmagic.omnira.client.screen;

/** Vanilla corners stay unscaled; horizontal edges tile for non-176-wide containers. */
public final class VanillaFrameLayout {
    @FunctionalInterface public interface Blit {
        void draw(int x,int y,int width,int height,int u,int v,int sourceWidth,int sourceHeight);
    }
    public static void draw(int width,int height,Blit blit){
        if(width<8||height<8)throw new IllegalArgumentException("Container frame is too small");
        blit.draw(0,0,4,4,0,0,4,4);blit.draw(width-4,0,4,4,172,0,4,4);
        blit.draw(0,height-4,4,4,0,162,4,4);blit.draw(width-4,height-4,4,4,172,162,4,4);
        for(int x=4;x<width-4;x+=168){
            int size=Math.min(168,width-4-x);
            blit.draw(x,0,size,4,4,0,size,4);blit.draw(x,height-4,size,4,4,162,size,4);
        }
        blit.draw(0,4,4,height-8,0,50,4,1);blit.draw(width-4,4,4,height-8,172,50,4,1);
    }
    private VanillaFrameLayout(){}
}

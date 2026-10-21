package com.mcmagic.omnira.client.screen;

public final class UiViewport {
    public record Layout(float scale,int width,int height) {}
    public record Hud(float scale,int x,int y) {}
    public static Hud spellHud(int width,int height,int occupiedLeftHeight){
        if(width>=374)return new Hud(1,46,Math.max(44,height-92));
        int bottom=Math.max(20,height-occupiedLeftHeight-6);
        float scale=Math.min(1,Math.max(1,bottom-8)/124F);
        return new Hud(scale,46,Math.round(bottom/scale)-76);
    }
    public static Layout layout(int width,int height,int contentWidth,int contentHeight){
        float scale=fit(width,height,contentWidth,contentHeight);
        return new Layout(scale,Math.round(width/scale),Math.round(height/scale));
    }
    public static int[] scissor(org.joml.Matrix4f matrix,int x,int y,int xx,int yy){
        var a=matrix.transformPosition(new org.joml.Vector3f(x,y,0));
        var b=matrix.transformPosition(new org.joml.Vector3f(xx,yy,0));
        return new int[]{(int)Math.floor(Math.min(a.x,b.x)),(int)Math.floor(Math.min(a.y,b.y)),
                (int)Math.ceil(Math.max(a.x,b.x)),(int)Math.ceil(Math.max(a.y,b.y))};
    }
    public static float fit(int width,int height,int contentWidth,int contentHeight){
        return Math.max(.01F,Math.min(1,Math.min(Math.max(1,width-12)/(float)contentWidth,Math.max(1,height-12)/(float)contentHeight)));
    }
    private UiViewport(){}
}

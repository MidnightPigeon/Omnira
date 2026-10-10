package com.mcmagic.omnira.client.screen;

public final class UiViewport {
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

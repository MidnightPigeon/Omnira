package com.mcmagic.omnira.client.guide;

/** Isotropic map camera. Viewport shape never changes the circle's aspect ratio. */
public final class GuideTreeView {
    private double x,y,zoom=1;
    public double x(){return x;} public double y(){return y;} public double zoom(){return zoom;}
    public double screenX(double world,double center){return center+(world-x)*zoom;}
    public double screenY(double world,double center){return center+(world-y)*zoom;}
    public double worldX(double screen,double center){return x+(screen-center)/zoom;}
    public double worldY(double screen,double center){return y+(screen-center)/zoom;}
    public void pan(double dx,double dy){x-=dx/zoom;y-=dy/zoom;bound();}
    public void center(int stage){x=0;y=stage<0?0:stageY(stage);}
    public void focus(int stage){center(stage);zoom=.9;}
    public void fit(double width,double height,double radius){
        center(-1);
        // Include the outer fade of the circular artwork in the overview.
        zoom=Math.clamp(Math.min(width,height)/(2*radius*1.20+16),.08,2);
    }
    public void zoomAt(double factor,double px,double py,double cx,double cy){
        double wx=worldX(px,cx),wy=worldY(py,cy);zoom=Math.clamp(zoom*factor,.08,2);
        x=wx-(px-cx)/zoom;y=wy-(py-cy)/zoom;bound();
    }
    private void bound(){x=Math.clamp(x,-1800,1800);y=Math.clamp(y,-1800,1800);}
    public static double stageY(int stage){return new double[]{481.6,179.2,-134.4,-476}[Math.clamp(stage,0,3)];}
}

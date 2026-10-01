package com.mcmagic.omnira.client.renderer;

import java.util.ArrayList;
import java.util.List;

/** Cut planar solids and rectangular bones. All coordinates are vanilla model pixels. */
public final class NightHeronGeometry {
    public record Point(double x,double y,double z) {}
    public record Face(Point a,Point b,Point c,Point d) {}
    public record Part(double x,double y,double z,double width,double height,double depth,double angle,int material,List<Face> surface,List<Face> exposed) {
        public Part(double x,double y,double z,double width,double height,double depth,double angle,int material,List<Face> surface) {
            this(x,y,z,width,height,depth,angle,material,surface,surface);
        }
    }
    public static final int PALE=0,WING=1,LEGS=2,BILL=3,EYE=4,PUPIL=5,PLUME=6,CAP=7,NECK=8,FACE=9,NECK_BACK=10;
    public static final double BODY_LENGTH_SCALE=1.22;
    private static final double EYE_PIXEL=.3;
    private NightHeronGeometry() {}

    public static List<Part> parts(boolean upright,double time,double stretch,boolean breeding) {
        var parts=new ArrayList<Part>();
        double stride=upright?0:Math.sin(time*.18),bob=Math.abs(stride)*.12;
        double y=(upright?15.6:14.1)+bob,angle=upright?-55:0;
        // Flat-sided dorsal body and a separate white breast/belly, not rounded rings.
        prism(parts,.3,y,0,angle,CAP,5.2,new double[][]{
            {-3.8,1.7},{-1.8,3.1},{1.2,3.1},{4.7,.15},{3.7,-1.15},{.2,-2.1},{-3.6,-.9}});
        prism(parts,-.1,y-.35,0,angle,PALE,5.35,new double[][]{
            {-3.8,1.65},{-2.4,2.3},{3.9,-.65},{3.1,-1.8},{.2,-2.55},{-3.5,-1.15}});
        for(int side:new int[]{-1,1})prism(parts,.5,y+.05,side*1.5,angle,WING,3,new double[][]{
            {-3.45,1.1},{-1.8,2.35},{1.2,2},{4.6,-.35},{3.2,-1.65},{-2.9,-1.25}});

        double hx=(upright?-2.7:-4)-(upright?0:stretch*4.1);
        double hy=upright?20.5+bob+stretch*4.7:y+1.8;
        double shoulderX=upright?-1.9:-1.2,shoulderY=upright?y+2.6:hy;
        // Both ends terminate inside their adjoining solids, even at full extension.
        beam(parts,shoulderX,shoulderY,0,hx,hy,0,2.55,NECK);
        box(parts,hx-.225,hy-.2075,0,2.65,2.135,2.55,0,FACE);
        box(parts,hx,hy+1.08,0,3.14,.44,2.59,0,CAP);
        box(parts,hx-2.65,hy-.25,0,2.3,.7,.9,0,BILL);
        box(parts,hx-3.95,hy-.3,0,.3,.55,.75,0,BILL);
        for(int side:new int[]{-1,1}) {
            double eyeX=hx-.8,eyeY=hy-.15,eyeZ=side*1.305;
            box(parts,eyeX,eyeY,eyeZ,EYE_PIXEL,EYE_PIXEL,.06,0,PUPIL);
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0 || dy!=0)
                box(parts,eyeX+dx*EYE_PIXEL,eyeY+dy*EYE_PIXEL,eyeZ,EYE_PIXEL,EYE_PIXEL,.06,0,EYE);
            double footX=.15+side*stride*.85,footY=8.15+Math.max(0,side*stride)*.35;
            // Hip starts inside the torso in either pose; the knee follows the gait.
            double hipY=y-.65,kneeX=.55+side*stride*.2;
            beam(parts,.1,hipY,side*1.1,kneeX,10.25,side*1.1,.45,LEGS);
            beam(parts,kneeX,10.25,side*1.1,footX,footY,side*1.1,.35,LEGS);
            for(int toe=-1;toe<=1;toe++)beam(parts,footX,footY,side*1.1,footX-1.45,footY,side*1.1+toe*.55,.2,LEGS);
            if(breeding) {
                if(upright) {
                    double[][] curve={{1.58,.55,.45},{2.8,.35,.6},{4.1,-.35,.7},{5,-1.4,.8},{5.3,-2.35,.85}};
                    for(int i=1;i<curve.length;i++) {
                        double[] a=curve[i-1],b=curve[i];
                        beam(parts,hx+a[0],hy+a[1],side*a[2],hx+b[0],hy+b[1],side*b[2],.17-(i-1)*.015,PLUME);
                    }
                } else {
                    beam(parts,hx+1.58,hy+.55,side*.45,hx+4.5,hy+1.7,side*.7,.17,PLUME);
                    beam(parts,hx+4.5,hy+1.7,side*.7,hx+5.8,hy+1.4,side*.85,.14,PLUME);
                }
            }
        }
        int napeIndex=parts.size();
        box(parts,hx+1.335,hy-.2075,0,.47,2.135,2.59,0,CAP);
        for(int i=0;i<4;i++) {
            Part p=parts.get(i);var cut=i<2?HeronMeshCuts.taper(p):HeronMeshCuts.foldWing(p);
            var stretched=cut.stream().map(f->new Face(lengthen(f.a()),lengthen(f.b()),lengthen(f.c()),lengthen(f.d()))).toList();
            parts.set(i,new Part(p.x(),p.y(),p.z(),p.width()*BODY_LENGTH_SCALE,p.height(),p.depth(),p.angle(),p.material(),stretched));
        }
        Part neck=parts.get(4);
        parts.set(4,new Part(neck.x(),neck.y(),neck.z(),neck.width(),neck.height(),neck.depth(),neck.angle(),neck.material(),neck.surface(),
                HeronMeshCuts.exposedNeck(neck,List.of(parts.get(0),parts.get(1),parts.get(5),parts.get(6),parts.get(napeIndex)))));
        // Adjacent head colors share a volume boundary, not two translucent interface faces.
        for(int index:new int[]{5,6,napeIndex}) {
            Part p=parts.get(index);
            var neighbors=new ArrayList<Part>();
            for(int other:new int[]{5,6,napeIndex})if(other!=index)neighbors.add(parts.get(other));
            parts.set(index,new Part(p.x(),p.y(),p.z(),p.width(),p.height(),p.depth(),p.angle(),p.material(),p.surface(),
                    HeronMeshCuts.exposedHead(p,neighbors)));
        }
        // The breast and folded wings own shared cut planes, preventing coplanar colors.
        for(int i=0;i<2;i++) {
            Part p=parts.get(i);
            parts.set(i,new Part(p.x(),p.y(),p.z(),p.width(),p.height(),p.depth(),p.angle(),p.material(),p.surface(),
                    HeronMeshCuts.exposedBody(p,parts.subList(i+1,4))));
        }
        for(int i=2;i<4;i++) {
            Part p=parts.get(i);
            parts.set(i,new Part(p.x(),p.y(),p.z(),p.width(),p.height(),p.depth(),p.angle(),p.material(),p.surface(),
                    HeronMeshCuts.exposedBody(p,parts.subList(0,2))));
        }
        // Crystal surfaces reveal buried hip segments; retain only the leg surfaces outside the torso.
        for(int i=0;i<parts.size();i++) {
            Part p=parts.get(i);
            if(p.material()!=LEGS || p.y()<=10.25)continue;
            parts.set(i,new Part(p.x(),p.y(),p.z(),p.width(),p.height(),p.depth(),p.angle(),p.material(),p.surface(),
                    HeronMeshCuts.exposedHead(p,parts.subList(0,4))));
        }
        return List.copyOf(parts);
    }

    private static void box(List<Part> out,double x,double y,double z,double w,double h,double d,double angle,int material) {
        prism(out,x,y,z,angle,material,d,new double[][]{{-w/2,-h/2},{w/2,-h/2},{w/2,h/2},{-w/2,h/2}});
    }
    private static Point lengthen(Point p){return new Point(p.x()*BODY_LENGTH_SCALE,p.y(),p.z());}

    /** Extrudes an explicit, planar side silhouette. No radial/polygon-ring approximation. */
    private static void prism(List<Part> out,double x,double y,double z,double angle,int material,double depth,double[][] outline) {
        int n=outline.length;double area=0,width=0,height=0;
        for(int i=0;i<n;i++) {
            double[] a=outline[i],b=outline[(i+1)%n];area+=a[0]*b[1]-b[0]*a[1];
            width=Math.max(width,Math.abs(a[0])*2);height=Math.max(height,Math.abs(a[1])*2);
        }
        Point[] front=new Point[n],back=new Point[n];
        for(int i=0;i<n;i++) {
            double[] p=outline[area>0?i:n-1-i];front[i]=new Point(p[0],p[1],depth/2);back[i]=new Point(p[0],p[1],-depth/2);
        }
        var faces=new ArrayList<Face>();
        // Convex silhouettes are triangulated only on their flat side faces.
        for(int i=1;i<n-1;i++) {
            faces.add(new Face(front[0],front[i],front[i+1],front[i+1]));
            faces.add(new Face(back[0],back[i+1],back[i],back[i]));
        }
        for(int i=0;i<n;i++){int j=(i+1)%n;faces.add(new Face(front[i],back[i],back[j],front[j]));}
        out.add(new Part(x,y,z,width,height,depth,angle,material,List.copyOf(faces)));
    }

    /** Rectangular beam with a constant square cross-section and two rectangular ends. */
    private static void beam(List<Part> out,double x,double y,double z,double X,double Y,double Z,double width,int material) {
        double dx=X-x,dy=Y-y,dz=Z-z,length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        double[] axis={dx/length,dy/length,dz/length};
        double[] v=cross(axis,Math.abs(axis[1])<.95?new double[]{0,1,0}:new double[]{1,0,0});
        double norm=Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);for(int i=0;i<3;i++)v[i]/=norm;
        double[] w=cross(axis,v);Point[] p=new Point[8];int[][] corners={{-1,-1},{1,-1},{1,1},{-1,1}};
        for(int end=0;end<2;end++)for(int i=0;i<4;i++) {
            double t=(end-.5)*(length+.1),a=corners[i][0]*width/2,b=corners[i][1]*width/2;
            p[end*4+i]=new Point(axis[0]*t+v[0]*a+w[0]*b,axis[1]*t+v[1]*a+w[1]*b,axis[2]*t+v[2]*a+w[2]*b);
        }
        var faces=new ArrayList<Face>();faces.add(new Face(p[0],p[3],p[2],p[1]));faces.add(new Face(p[4],p[5],p[6],p[7]));
        for(int i=0;i<4;i++){int j=(i+1)%4;faces.add(new Face(p[i],p[j],p[4+j],p[4+i]));}
        out.add(new Part((x+X)/2,(y+Y)/2,(z+Z)/2,Math.abs(dx)+width+.1,Math.abs(dy)+width+.1,Math.abs(dz)+width+.1,0,material,List.copyOf(faces)));
    }
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    public static List<Face> faces(Part part){return part.surface();}
    public static List<Face> visibleFaces(Part part,boolean crystal) {
        return part.exposed();
    }
}

package com.mcmagic.omnira.client.renderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static com.mcmagic.omnira.client.renderer.NightHeronGeometry.*;

/** Planar mesh operations used for the torso's side cuts and buried neck surfaces. */
final class HeronMeshCuts {
    private static final double EPS=1e-7;
    private record TaperKey(double x,double z,List<Face> surface) {}
    private static final java.util.Map<TaperKey,List<Face>> TAPER_CACHE=new java.util.HashMap<>();
    private record BodyKey(double x,double z,double angle,List<Face> surface) {}
    private static final java.util.Map<BodyKey,List<Face>> BODY_CACHE=new java.util.HashMap<>();
    record Plane(Point n,double d) { double distance(Point p){return dot(n,p)-d;} }
    private HeronMeshCuts() {}

    static List<Face> taper(Part p) {
        return TAPER_CACHE.computeIfAbsent(new TaperKey(p.x(),p.z(),p.surface()),key->makeTaper(p));
    }
    private static List<Face> makeTaper(Part p) {
        List<Face> mesh=p.surface();
        for(int sign:new int[]{-1,1}) {
            mesh=cutSolid(mesh,new Plane(new Point(-.5,0,sign),3.35+.5*(p.x()-.3)-sign*p.z()));
            mesh=cutSolid(mesh,new Plane(new Point(.6,0,sign),3.6-.6*(p.x()-.3)-sign*p.z()));
        }
        return mesh;
    }

    static List<Face> exposedNeck(Part neck,List<Part> neighbors) {
        return exposed(neck,neighbors,2);
    }
    static List<Face> exposedHead(Part head,List<Part> neighbors) {
        return exposed(head,neighbors,0);
    }
    static List<Face> foldWing(Part p) {
        // A full tapered solid keeps the wing tips, with its inner volume buried in the torso.
        return TAPER_CACHE.computeIfAbsent(new TaperKey(p.x(),p.z(),p.surface()),key->{
            double sign=Math.signum(p.z());
            var mesh=cutSolid(p.surface(),new Plane(new Point(-.5,0,sign),3.6+.5*(p.x()-.3)-sign*p.z()));
            return cutSolid(mesh,new Plane(new Point(.6,0,sign),3.85-.6*(p.x()-.3)-sign*p.z()));
        });
    }
    static List<Face> exposedBody(Part body,List<Part> neighbors) {
        // These pieces share only a vertical bob; local cut surfaces depend on pose, not time.
        return BODY_CACHE.computeIfAbsent(new BodyKey(body.x(),body.z(),body.angle(),body.surface()),key->exposed(body,neighbors,0));
    }
    private static List<Face> exposed(Part neck,List<Part> neighbors,int firstFace) {
        List<List<Point>> polygons=new ArrayList<>();
        for(Face f:neck.surface().subList(firstFace,neck.surface().size()))polygons.add(points(f).stream().map(v->world(neck,v)).toList());
        for(Part neighbor:neighbors) {
            List<Plane> planes=new ArrayList<>();
            for(Face f:neighbor.surface()) {
                Point a=world(neighbor,f.a()),b=world(neighbor,f.b()),c=world(neighbor,f.c());
                Point n=unit(cross(sub(b,a),sub(c,a)));double d=dot(n,a)+EPS;
                if(planes.stream().noneMatch(q->dot(q.n(),n)>1-EPS && Math.abs(q.d()-d)<EPS))planes.add(new Plane(n,d));
            }
            List<List<Point>> outside=new ArrayList<>();
            for(List<Point> polygon:polygons) {
                List<Point> remaining=polygon;
                for(Plane plane:planes) {
                    if(remaining.size()<3)break;
                    List<Point> fragment=clip(remaining,new Plane(scale(plane.n(),-1),-plane.d()));
                    if(fragment.size()>=3)outside.add(fragment);
                    remaining=clip(remaining,plane);
                }
            }
            polygons=outside;
        }
        List<Face> out=new ArrayList<>();
        for(List<Point> polygon:polygons)triangulate(out,polygon.stream().map(v->local(neck,v)).toList());
        return List.copyOf(out);
    }

    private static List<Face> cutSolid(List<Face> mesh,Plane plane) {
        List<Face> out=new ArrayList<>();List<Point> rim=new ArrayList<>();
        boolean removed=false;
        for(Face face:mesh) {
            List<Point> polygon=points(face);
            if(polygon.stream().anyMatch(v->plane.distance(v)>EPS))removed=true;
            List<Point> clipped=clip(polygon,plane);
            for(Point p:clipped)if(Math.abs(plane.distance(p))<EPS*4 && rim.stream().noneMatch(q->length(sub(p,q))<EPS*4))rim.add(p);
            triangulate(out,clipped);
        }
        if(removed && rim.size()>=3) {
            Point center=scale(rim.stream().reduce(new Point(0,0,0),HeronMeshCuts::add),1.0/rim.size());
            Point n=unit(plane.n()),u=unit(sub(rim.get(0),center)),v=cross(n,u);
            rim.sort(Comparator.comparingDouble(p->Math.atan2(dot(sub(p,center),v),dot(sub(p,center),u))));
            triangulate(out,rim);
        }
        return List.copyOf(out);
    }
    private static List<Point> clip(List<Point> polygon,Plane plane) {
        List<Point> out=new ArrayList<>();if(polygon.isEmpty())return out;
        Point a=polygon.get(polygon.size()-1);double da=plane.distance(a);
        for(Point b:polygon) {
            double db=plane.distance(b);
            if((da<=0)!=(db<=0))out.add(add(a,scale(sub(b,a),da/(da-db))));
            if(db<=0)out.add(b);a=b;da=db;
        }
        List<Point> clean=new ArrayList<>();
        for(Point p:out)if(clean.isEmpty() || length(sub(p,clean.get(clean.size()-1)))>EPS)clean.add(p);
        if(clean.size()>1 && length(sub(clean.get(0),clean.get(clean.size()-1)))<EPS)clean.remove(clean.size()-1);
        return clean;
    }
    private static void triangulate(List<Face> out,List<Point> p) {
        for(int i=1;i<p.size()-1;i++)if(length(cross(sub(p.get(i),p.get(0)),sub(p.get(i+1),p.get(0))))>EPS)
            out.add(new Face(p.get(0),p.get(i),p.get(i+1),p.get(i+1)));
    }
    private static List<Point> points(Face f){return f.c().equals(f.d())?List.of(f.a(),f.b(),f.c()):List.of(f.a(),f.b(),f.c(),f.d());}
    private static Point world(Part p,Point v){double r=Math.toRadians(p.angle());return new Point(p.x()+v.x()*Math.cos(r)-v.y()*Math.sin(r),p.y()+v.x()*Math.sin(r)+v.y()*Math.cos(r),p.z()+v.z());}
    private static Point local(Part p,Point v){double r=Math.toRadians(-p.angle()),x=v.x()-p.x(),y=v.y()-p.y();return new Point(x*Math.cos(r)-y*Math.sin(r),x*Math.sin(r)+y*Math.cos(r),v.z()-p.z());}
    private static Point add(Point a,Point b){return new Point(a.x()+b.x(),a.y()+b.y(),a.z()+b.z());}
    private static Point sub(Point a,Point b){return new Point(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());}
    private static Point scale(Point p,double s){return new Point(p.x()*s,p.y()*s,p.z()*s);}
    private static double dot(Point a,Point b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
    private static Point cross(Point a,Point b){return new Point(a.y()*b.z()-a.z()*b.y(),a.z()*b.x()-a.x()*b.z(),a.x()*b.y()-a.y()*b.x());}
    private static double length(Point p){return Math.sqrt(dot(p,p));}
    private static Point unit(Point p){return scale(p,1/length(p));}
}

package com.mcmagic.omnira.engine;

import java.util.*;

/** Model units are one pixel (1/16 block); the controller is above (8, 8). */
public final class SpacetimeEngineGeometry {
    public record Box(double x,double y,double z,double xx,double yy,double zz,int color,boolean glow,int faces){
        public Box(double x,double y,double z,double xx,double yy,double zz,int color,boolean glow){this(x,y,z,xx,yy,zz,color,glow,63);}
        public Box tint(int color){return new Box(x,y,z,xx,yy,zz,color,glow,faces);}
    }
    public static final int GLASS=0x8098D5E1,FRAME=0xDBBCDADC,GREEN=0xBAA6E6CA;
    private static final int[] OUTPUT_COLORS={0xFF79C8FF,0xFFF0B966,0xFFC798F2,0xFF79D9A9};
    public static int outputColor(int relativeSide){return OUTPUT_COLORS[Math.floorMod(relativeSide,4)];}
    public static final List<Box> BASE=base(),ARRAY=array(),ROTOR=rotor(),FRONT=front(),DRIVE=drive();
    public static final Box SHAFT=new Box(-2,-2,-8,2,2,4,FRAME,false);
    public static final Box BEARING=new Box(-2,3,-2,2,5,2,GREEN,true);
    public static final Box NODE=new Box(-2,-1,-2,2,0,2,FRAME,false);
    public static final double ARRAY_RADIUS=19,ARRAY_HEIGHT=19;
    public static double arrayAngle(double ticks){return ticks*.012;}
    public static double cornerAngle(int slot){return slot*Math.PI*2/3;}
    private static List<Box> base(){
        var v=new Voxels();
        // One continuous chamfered chassis, not nine independent ingredient blocks.
        for(int x=-20;x<20;x++)for(int z=-20;z<20;z++){
            double ax=Math.abs(x+.5),az=Math.abs(z+.5);
            if(ax+az>30)continue;
            v.fill(x+8,0,z+8,x+9,2,z+9,0xD18EBAC9,false);
            if(ax+az<27&&ax<18&&az<18)v.fill(x+8,2,z+8,x+9,5,z+9,GLASS,false);
            if((Math.max(ax,az)>16||ax+az>26)&&Math.max(ax,az)<19)v.fill(x+8,5,z+8,x+9,6,z+9,FRAME,false);
        }
        // Four broad spokes and integral buttresses meet the central containment chamber.
        v.fill(4,5,-12,12,8,28,GLASS,false);v.fill(-12,5,4,28,8,12,GLASS,false);
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}){
            for(int r=7;r<=13;r++){
                int x=8+sx*r,z=8+sz*r,top=16-r;
                v.fill(x-1,5,z-1,x+1,top+3,z+1,FRAME,false);
            }
            int x=8+sx*6,z=8+sz*6;
            v.fill(x-1,6,z-1,x+1,24,z+1,FRAME,false);
        }
        for(int x=-7;x<7;x++)for(int z=-7;z<7;z++){
            int r=Math.max(Math.abs(x),Math.abs(z));
            if(Math.abs(x+.5)+Math.abs(z+.5)>11)continue;
            v.fill(x+8,6,z+8,x+9,9,z+9,0xC995CDBF,false);
            if(r>=5)v.fill(x+8,24,z+8,x+9,26,z+9,FRAME,false);
        }
        // Narrow crystal windows expose the elongated energy rotor without a cube-shaped core shell.
        v.fill(3,10,1,13,23,2,0x568EC8DC,false);v.fill(3,10,14,13,23,15,0x568EC8DC,false);
        v.fill(1,10,3,2,23,13,0x568EC8DC,false);v.fill(14,10,3,15,23,13,0x568EC8DC,false);
        return v.bake();
    }
    private static List<Box> rotor(){
        var v=new Voxels();
        for(int y=9;y<24;y++){
            int radius=y<11||y>21?2:3;
            for(int x=-radius;x<radius;x++)for(int z=-radius;z<radius;z++)
                if(Math.abs(x+.5)+Math.abs(z+.5)<=radius+1)v.fill(x,y,z,x+1,y+1,z+1,0xEF294B5A,false);
        }
        for(int y=10;y<23;y++){
            double a=(y-10)*.55;int x=(int)Math.round(Math.cos(a)*4),z=(int)Math.round(Math.sin(a)*4);
            v.fill(x,y,z,x+1,y+1,z+1,0xED9BCBEF,true);
            v.fill(-x-1,y,-z-1,-x,y+1,-z,0xEDD0F0CB,true);
        }
        return v.bake();
    }
    private static List<Box> front(){
        var v=new Voxels();
        v.fill(-4,17,-8,4,23,-7,0xDF3B687A,false);
        for(int y=18;y<22;y++)v.fill(-1,y,-9,1,y+1,-8,outputColor(0),true);
        v.fill(-3,20,-9,3,21,-8,outputColor(0),true);
        return v.bake();
    }
    private static List<Box> drive(){
        var v=new Voxels();
        for(int x=-5;x<5;x++)for(int y=-5;y<5;y++)if(Math.max(Math.abs(x+.5),Math.abs(y+.5))>=3){
            v.fill(x,y,-20,x+1,y+1,-18,FRAME,false);v.fill(x,y,-12,x+1,y+1,-10,FRAME,false);
        }
        for(int x:new int[]{-5,3})for(int y:new int[]{-5,3})v.fill(x,y,-18,x+2,y+2,-12,GLASS,false);
        return v.bake();
    }
    private static List<Box> array(){
        // A voxel outline with hidden shared faces removed by the renderer.
        var voxels=new LinkedHashSet<Long>();
        for(int side=0;side<6;side++){
            double a=side*Math.PI/3,c=(side+1)*Math.PI/3;
            for(int n=0;n<=40;n++){
                double t=n/40.;int x=(int)Math.round(ARRAY_RADIUS*(Math.cos(a)*(1-t)+Math.cos(c)*t));
                int z=(int)Math.round(ARRAY_RADIUS*(Math.sin(a)*(1-t)+Math.sin(c)*t));voxels.add(((long)x<<32)|(z&0xffffffffL));
            }
        }
        var b=new ArrayList<Box>();
        for(long p:voxels){int x=(int)(p>>32),z=(int)p;b.add(new Box(x-.5,-.5,z-.5,x+.5,.5,z+.5,GREEN,true));}
        return List.copyOf(b);
    }
    private SpacetimeEngineGeometry(){}

    private record Cell(int x,int y,int z){}
    private record Material(int color,boolean glow){}
    private record Plane(int face,int depth,Material material){}
    private record Pixel(int u,int v){}
    /** Greedy surface meshing removes buried faces and keeps every UV span within one 16px tile. */
    private static final class Voxels {
        private final Map<Cell,Material> cells=new LinkedHashMap<>();
        void fill(int x,int y,int z,int xx,int yy,int zz,int color,boolean glow){
            var m=new Material(color,glow);for(int a=x;a<xx;a++)for(int b=y;b<yy;b++)for(int c=z;c<zz;c++)cells.put(new Cell(a,b,c),m);
        }
        List<Box> bake(){
            int[][] neighbours={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
            var planes=new LinkedHashMap<Plane,Set<Pixel>>();
            for(var entry:cells.entrySet())for(int face=0;face<6;face++){
                var p=entry.getKey();var n=neighbours[face];if(cells.containsKey(new Cell(p.x+n[0],p.y+n[1],p.z+n[2])))continue;
                int depth=face<2?p.z+(face==1?1:0):face<4?p.x+(face==3?1:0):p.y+(face==4?1:0);
                var pixel=new Pixel(face<2||face>=4?p.x:p.z,face<4?p.y:p.z);
                planes.computeIfAbsent(new Plane(face,depth,entry.getValue()),ignored->new HashSet<>()).add(pixel);
            }
            var result=new ArrayList<Box>();
            for(var entry:planes.entrySet()){
                var p=entry.getKey();var pixels=entry.getValue();
                while(!pixels.isEmpty()){
                    var start=pixels.stream().min(Comparator.comparingInt(Pixel::v).thenComparingInt(Pixel::u)).orElseThrow();
                    int u=start.u,v=start.v,w=1,h=1;
                    while(w<16&&pixels.contains(new Pixel(u+w,v)))w++;
                    outer:while(h<16){for(int a=0;a<w;a++)if(!pixels.contains(new Pixel(u+a,v+h)))break outer;h++;}
                    for(int a=0;a<w;a++)for(int b=0;b<h;b++)pixels.remove(new Pixel(u+a,v+b));
                    int low=p.depth-((p.face==1||p.face==3||p.face==4)?1:0);
                    result.add(p.face<2?new Box(u,v,low,u+w,v+h,low+1,p.material.color,p.material.glow,1<<p.face)
                            :p.face<4?new Box(low,v,u,low+1,v+h,u+w,p.material.color,p.material.glow,1<<p.face)
                            :new Box(u,low,v,u+w,low+1,v+h,p.material.color,p.material.glow,1<<p.face));
                }
            }return List.copyOf(result);
        }
    }
}

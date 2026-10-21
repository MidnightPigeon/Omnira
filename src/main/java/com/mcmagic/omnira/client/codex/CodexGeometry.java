package com.mcmagic.omnira.client.codex;

import com.google.gson.*;
import com.mcmagic.omnira.client.MiniatureCodexScene;
import com.mcmagic.omnira.spell.visual.SpellVolumeGeometry;
import java.io.Reader;
import java.util.*;

/** Shared JSON geometry, palette and poses for the renderer and standalone preview. */
public final class CodexGeometry {
    public static final class Face {
        private final float[][] vertices;
        private final int tint;
        private final float nx,ny,nz,shade;
        public Face(float[][] vertices,int tint){
            this.vertices=vertices;this.tint=tint;
            float ax=vertices[1][0]-vertices[0][0],ay=vertices[1][1]-vertices[0][1],az=vertices[1][2]-vertices[0][2];
            float bx=vertices[2][0]-vertices[0][0],by=vertices[2][1]-vertices[0][1],bz=vertices[2][2]-vertices[0][2];
            float x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx;
            float length=(float)Math.sqrt(x*x+y*y+z*z);
            nx=x/length;ny=y/length;nz=z/length;shade=.72F+.28F*Math.max(0,ny);
        }
        public float[][] vertices(){return vertices;}
        public int tint(){return tint;}
        public float nx(){return nx;}
        public float ny(){return ny;}
        public float nz(){return nz;}
        public float shade(){return shade;}
    }
    public record Point(float x, float y, float z) {}
    public record RangeFace(Face geometry,boolean ring){}
    public record PaintedFace(Face geometry,int color) {}
    public record WorldMesh(List<PaintedFace> faces) {}
    private static final int[][] FACES = {{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static final String[] NAMES = {"north","south","west","east","up","down"};
    private static final WorldMesh[] WORLDS=new WorldMesh[33];
    private static final List<Face> HEART=buildHeart();
    private static final List<Face> PARTICLE=buildParticle();
    public static final int RANGE_SHELL_ALPHA=24,RANGE_RING_ALPHA=64;
    private static final List<List<RangeFace>> RANGES=List.of(buildRange(7),buildRange(9),buildRange(11),buildRange(13),buildRange(15));

    public static List<Face> model(Reader reader) {
        var json = JsonParser.parseReader(reader).getAsJsonObject();
        var result = new ArrayList<Face>();
        for (var element : json.getAsJsonArray("elements")) {
            var part = element.getAsJsonObject();
            float[] a = vector(part.getAsJsonArray("from")), b = vector(part.getAsJsonArray("to"));
            float[][] corners = corners(a[0],a[1],a[2],b[0],b[1],b[2]);
            if (part.has("rotation")) {
                var rotation = part.getAsJsonObject("rotation");
                float[] origin = vector(rotation.getAsJsonArray("origin"));
                int axis = "xyz".indexOf(rotation.get("axis").getAsString());
                double angle = Math.toRadians(rotation.get("angle").getAsDouble());
                for (var p : corners) {
                    int u = (axis+1)%3, v = (axis+2)%3;
                    double x = p[u]-origin[u], y = p[v]-origin[v];
                    p[u] = origin[u]+(float)(x*Math.cos(angle)-y*Math.sin(angle));
                    p[v] = origin[v]+(float)(x*Math.sin(angle)+y*Math.cos(angle));
                }
            }
            var faces = part.getAsJsonObject("faces");
            for (int side = 0; side < 6; side++) if (faces.has(NAMES[side])) {
                var face = faces.getAsJsonObject(NAMES[side]);
                float[][] vertices = new float[4][3];
                for (int i = 0; i < 4; i++) for (int n = 0; n < 3; n++) vertices[i][n] = corners[FACES[side][i]][n]/16;
                result.add(new Face(vertices, face.has("tintindex") ? face.get("tintindex").getAsInt() : 1));
            }
        }
        return List.copyOf(result);
    }
    public static List<Face> box(MiniatureCodexScene.Box b) {
        var vertices = corners(b.x()-b.width()/2,b.y()-b.height()/2,b.z()-b.depth()/2,
                b.x()+b.width()/2,b.y()+b.height()/2,b.z()+b.depth()/2);
        var faces = new ArrayList<Face>(6);
        for (var ids : FACES) faces.add(new Face(new float[][]{vertices[ids[0]],vertices[ids[1]],vertices[ids[2]],vertices[ids[3]]},-1));
        return faces;
    }
    public static List<Face> miniature(MiniatureCodexScene.Box box,List<MiniatureCodexScene.Box> boxes){
        var result=new ArrayList<Face>();
        for(var face:box(box)){
            var p=face.vertices();
            int side=p[0][0]==p[1][0]&&p[0][0]==p[2][0]&&p[0][0]==p[3][0]?(p[0][0]<box.x()?-1:1):0;
            if(side==0){result.add(face);continue;}
            for(var span:MiniatureCodexScene.sideSpans(box,boxes,side)){
                var copy=new float[4][3];for(int i=0;i<4;i++){copy[i]=p[i].clone();copy[i][1]=p[i][1]>box.y()?span.top():span.bottom();}
                result.add(new Face(copy,-1));
            }
        }return List.copyOf(result);
    }
    /** Bounded, shared pose cache: one eighth of a stage is below the tiny world's pixel scale. */
    public static WorldMesh world(float evolution){
        int index=Math.round(Math.clamp(evolution,0,4)*8);
        var mesh=WORLDS[index];
        if(mesh==null){
            var boxes=MiniatureCodexScene.boxes(index/8F,0);
            var faces=new ArrayList<PaintedFace>();
            for(var box:boxes)for(var face:miniature(box,boxes))faces.add(new PaintedFace(face,box.color()));
            mesh=new WorldMesh(List.copyOf(faces));WORLDS[index]=mesh;
        }
        return mesh;
    }
    public static int color(int tint, double time, boolean busy) {
        float p = (float)(.5+.5*Math.sin(time*(busy?.045:.008)));
        return switch (tint) {
            case 0 -> blend(0xFFB96875,0xFF348C82,p);
            case 1 -> 0xFFF0F1DD;
            case 2 -> blend(0xFFBCC8E9,0xFFF4D6E0,p);
            case 3 -> blend(0xA87DCED9,0xA8C0A5DC,p);
            case 4 -> busy?0xFFE5A3B6:0xFF526B71;
            case 5 -> 0xFFF5B583;
            case 6 -> 0xFF90C8F1;
            case 7 -> 0xFFE78791;
            case 8 -> 0xFFB6D894;
            case 9 -> 0xFFE6D387;
            case 10 -> 0xFFB4A0D4;
            default -> 0xFFFFFFFF;
        };
    }
    public static float opacity(int fade) { return 1-.1F*Math.clamp(fade,0,10); }
    public static List<RangeFace> range(int diameter) {
        return diameter<7?List.of():RANGES.get(Math.clamp((diameter-7)/2,0,4));
    }
    private static List<RangeFace> buildRange(int diameter) {
        int radius=(diameter-1)/2;var result=new ArrayList<RangeFace>();
        for(var quad:SpellVolumeGeometry.ball())result.add(rangeFace(quad,radius,-1));
        var ring=SpellVolumeGeometry.rangeRing(radius,1);
        for(int axis=0;axis<3;axis++)for(var quad:ring)result.add(rangeFace(quad,radius,axis));
        return List.copyOf(result);
    }
    private static RangeFace rangeFace(SpellVolumeGeometry.Quad quad,float radius,int axis){
        var vertices=new float[4][3];
        for(int i=0;i<4;i++){
            var p=quad.vertices()[i];float x=p[0],y=p[1],z=p[2];
            if(axis==1){y=-p[2];z=p[1];}else if(axis==2){x=-p[1];y=p[0];}
            vertices[i]=new float[]{.5F+x*radius,.5F+y*radius,.5F+z*radius};
        }
        return new RangeFace(new Face(vertices,1),axis>=0);
    }
    public static double bookY(double time, boolean busy) { return .83+Math.sin(time*(busy?.09:.035))*(busy?.045:.035); }
    public static double bookPitch(){return -10;}
    public static double homeHeartY(){return 1.58;}
    public static List<Face> heart(){return HEART;}
    private static List<Face> buildHeart(){
        int[][][] pixels=new int[16][16][16];
        for(int x=5;x<=10;x++)for(int y=5;y<=10;y++)for(int z=5;z<=10;z++){
            double dx=x-7.5,dy=y-7.5,dz=z-7.5;
            if(dx*dx+dy*dy+dz*dz>8)continue;
            int tint=y>=9?9:y<=6?10:x<=6?7:x>=9?8:z<=6?6:5;
            if(x==7&&y>=8&&z<=6)tint=1;
            pixels[x][y][z]=tint;
        }
        int[][] shards={{3,8,7},{12,8,8},{7,8,3},{8,8,12},{4,10,4},{11,5,4},{4,5,11},{11,10,11}};
        for(int i=0;i<shards.length;i++){
            var p=shards[i];pixels[p[0]][p[1]][p[2]]=5+i%6;
            pixels[p[0]][p[1]+1][p[2]]=i%2==0?1:5+i%6;
        }
        var faces=new ArrayList<Face>();
        int[][] neighbours={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        // One model unit per voxel; adjoining cells emit only their exposed exterior.
        for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++)if(pixels[x][y][z]!=0){
            var vertices=corners(x/16F,y/16F,z/16F,(x+1)/16F,(y+1)/16F,(z+1)/16F);
            for(int side=0;side<6;side++){
                var n=neighbours[side];int xx=x+n[0],yy=y+n[1],zz=z+n[2];
                if(xx>=0&&xx<16&&yy>=0&&yy<16&&zz>=0&&zz<16&&pixels[xx][yy][zz]!=0)continue;
                var ids=FACES[side];faces.add(new Face(new float[][]{vertices[ids[0]],vertices[ids[1]],vertices[ids[2]],vertices[ids[3]]},pixels[x][y][z]));
            }
        }
        return List.copyOf(faces);
    }
    public static List<Face> particle(){return PARTICLE;}
    private static List<Face> buildParticle(){
        var p=corners(0,0,0,1,1,1);var result=new ArrayList<Face>(6);
        for(var ids:FACES)result.add(new Face(new float[][]{p[ids[0]],p[ids[1]],p[ids[2]],p[ids[3]]},1));
        return List.copyOf(result);
    }
    public static int linkSamples(double length){return Math.clamp((int)Math.ceil(length/.25),1,128);}
    public static double linkFraction(int index,int samples){return (index+.5)/samples;}
    public static int linkAlpha(int alpha,double time,int index){return (int)Math.round(alpha*(.8+.2*Math.sin(time*.04+index*.7)));}
    public static double bookTilt(double time, boolean busy) { return Math.sin(time*(busy?.15:.02))*(busy?3:1); }
    public static float evolution(double time, boolean busy) { return (float)(2+2*Math.sin(time*(busy?.025:.004))); }
    public static Point onBook(float x,float y,float z,double time,boolean busy) {
        double a=Math.toRadians(bookTilt(time,busy)),pitch=Math.toRadians(bookPitch()),dx=x-.5,dz=z-.5;
        double xx=dx*Math.cos(a)-y*Math.sin(a),yy=dx*Math.sin(a)+y*Math.cos(a);
        return new Point(.5F+(float)xx,(float)(bookY(time,busy)+yy*Math.cos(pitch)-dz*Math.sin(pitch)),
                .5F+(float)(yy*Math.sin(pitch)+dz*Math.cos(pitch)));
    }
    private static int blend(int a,int b,float p) {
        int c=0;for(int s=0;s<=24;s+=8)c|=Math.round((a>>>s&255)*(1-p)+(b>>>s&255)*p)<<s;return c;
    }
    private static float[] vector(JsonArray a) { return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()}; }
    private static float[][] corners(float x,float y,float z,float X,float Y,float Z) {
        return new float[][]{{x,y,z},{X,y,z},{X,Y,z},{x,Y,z},{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
    }
    private CodexGeometry() {}
}

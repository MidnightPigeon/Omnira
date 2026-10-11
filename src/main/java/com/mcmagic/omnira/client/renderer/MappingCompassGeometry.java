package com.mcmagic.omnira.client.renderer;

import java.util.*;

/** Shared native mesh for rendering and offline silhouette inspection. */
public final class MappingCompassGeometry {
    private static final int[][] CORNERS={{0,0,0},{1,0,0},{1,1,0},{0,1,0},{0,0,1},{1,0,1},{1,1,1},{0,1,1}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    public record Quad(float[] xyz,int color,float alpha){}
    public static final List<Quad> PLATE=plateMesh(),NEEDLE=needleMesh();
    private static boolean plate(int x,int y){return (x-7.5)*(x-7.5)+(y-7.5)*(y-7.5)<=36;}
    private static void face(List<Quad> mesh,int f,float x,float y,float z,float sx,float sy,float sz,int color,float alpha){
        float[] points=new float[12];int i=0;
        for(int n:FACES[f]){points[i++]=x+CORNERS[n][0]*sx;points[i++]=y+CORNERS[n][1]*sy;points[i++]=z+CORNERS[n][2]*sz;}
        mesh.add(new Quad(points,color,alpha));
    }
    private static void cube(List<Quad> mesh,float x,float y,float z,float r,int color,float alpha){
        for(int f=0;f<6;f++)face(mesh,f,x-r,y-r,z-r,2*r,2*r,2*r,color,alpha);
    }
    private static List<Quad> plateMesh(){
        var mesh=new ArrayList<Quad>();
        for(int x=2;x<14;x++)for(int y=2;y<14;y++){
            if(!plate(x,y))continue;
            boolean rim=!plate(x-1,y)||!plate(x+1,y)||!plate(x,y-1)||!plate(x,y+1);
            boolean tick=(x==7||x==8||y==7||y==8)&&((x-7.5)*(x-7.5)+(y-7.5)*(y-7.5)>16);
            int color=tick?0xF2FBFF:rim?0x91DDEC:0x5AAEC9;float alpha=tick?.95F:rim?.8F:.38F;
            for(int f=0;f<6;f++){
                if(f==2&&plate(x-1,y)||f==3&&plate(x+1,y)||f==4&&plate(x,y+1)||f==5&&plate(x,y-1))continue;
                face(mesh,f,x/16F,y/16F,.46F,1F/16,1F/16,.08F,color,alpha);
            }
        }
        for(int x=6;x<=9;x++)for(int y=14;y<=15;y++)if(y==15||x==6||x==9)
            cube(mesh,(x+.5F)/16,(y+.5F)/16,.5F,1F/32,0xB9E8F0,.85F);
        return List.copyOf(mesh);
    }
    private static List<Quad> needleMesh(){
        var mesh=new ArrayList<Quad>();
        for(int half=0;half<2;half++)for(int f=0;f<6;f++){
            if(half==0&&f==4||half==1&&f==5)continue;
            face(mesh,f,7.5F/16,.25F+half*.25F,.55F,1F/16,.25F,.05F,half==1?0xDBF8FF:0x5568CA,.95F);
        }
        cube(mesh,.5F,.5F,.60F,.045F,0xFFFFFF,.9F);return List.copyOf(mesh);
    }
    public static float idleAngle(double time){return (float)(Math.sin(time*.018)*18+Math.sin(time*.043)*5);}
    public static void main(String[] args){
        double angle=Math.toRadians(args.length==0?0:Double.parseDouble(args[0]));
        for(var part:List.of(PLATE,NEEDLE))for(var q:part){
            var points=q.xyz().clone();
            if(part==NEEDLE)for(int i=0;i<points.length;i+=3){double x=points[i]-.5,y=points[i+1]-.5;points[i]=(float)(.5+x*Math.cos(angle)-y*Math.sin(angle));points[i+1]=(float)(.5+x*Math.sin(angle)+y*Math.cos(angle));}
            System.out.println("["+q.color()+","+q.alpha()+","+Arrays.toString(points)+"]");
        }
    }
    private MappingCompassGeometry(){}
}

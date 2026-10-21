package com.mcmagic.omnira.spell.visual;

import java.util.ArrayList;
import java.util.List;

/** The original burst's pixel shell and annulus, cached at unit radius for shared previews. */
public final class SpellVolumeGeometry {
    public record Quad(float[][] vertices){}
    private static final int[][] SIDES={{3,2,6,7},{0,4,5,1},{1,5,6,2},{0,3,7,4},{0,1,2,3},{4,7,6,5}};
    private static final int[][] NEIGHBOURS={{0,1,0},{0,-1,0},{1,0,0},{-1,0,0},{0,0,-1},{0,0,1}};
    private static final List<Quad> BALL=buildBall(),RING=buildRing(4,3),THIN_RING=buildRing(8,7);
    public static List<Quad> ball(){return BALL;}
    public static List<Quad> ring(){return RING;}
    public static List<Quad> thinRing(){return THIN_RING;}
    /** Unit-radius geometry with quarter-block pixels and a fixed world-space band width. */
    public static List<Quad> rangeRing(int radius,int width){
        if(radius<=0||width<=0||width>radius)throw new IllegalArgumentException("Invalid range band");
        int outer=radius*4,inner=(radius-width)*4;float unit=1F/outer;
        var result=new ArrayList<Quad>();
        // Merge contiguous pixels along each row without changing the stepped silhouette.
        for(int z=-outer;z<outer;z++)for(int x=-outer;x<outer;){
            if(!inBand(x,z,outer,inner)){x++;continue;}
            int start=x++;while(x<outer&&inBand(x,z,outer,inner))x++;
            result.add(new Quad(new float[][]{{start*unit,0,z*unit},{x*unit,0,z*unit},
                    {x*unit,0,(z+1)*unit},{start*unit,0,(z+1)*unit}}));
        }
        return List.copyOf(result);
    }
    private static boolean inBand(int x,int z,int outer,int inner){double d=(x+.5)*(x+.5)+(z+.5)*(z+.5);return d>=inner*inner&&d<=outer*outer;}
    private static boolean voxel(int x,int y,int z){return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=3;}
    private static List<Quad> buildBall(){
        var result=new ArrayList<Quad>();
        for(int x=-2;x<2;x++)for(int y=-2;y<2;y++)for(int z=-2;z<2;z++)if(voxel(x,y,z)){
            float a=x*.5F,b=y*.5F,c=z*.5F,d=a+.5F,e=b+.5F,f=c+.5F;
            float[][] p={{a,b,c},{d,b,c},{d,e,c},{a,e,c},{a,b,f},{d,b,f},{d,e,f},{a,e,f}};
            for(int side=0;side<6;side++){
                var n=NEIGHBOURS[side];if(voxel(x+n[0],y+n[1],z+n[2]))continue;
                var s=SIDES[side];result.add(new Quad(new float[][]{p[s[0]],p[s[1]],p[s[2]],p[s[3]]}));
            }
        }
        return List.copyOf(result);
    }
    private static List<Quad> buildRing(int radius,int inner){
        var result=new ArrayList<Quad>();
        float unit=1F/radius;
        for(int x=-radius;x<radius;x++)for(int z=-radius;z<radius;z++){
            double distance=(x+.5)*(x+.5)+(z+.5)*(z+.5);if(distance<inner*inner||distance>radius*radius)continue;
            result.add(new Quad(new float[][]{{x*unit,0,z*unit},{(x+1)*unit,0,z*unit},
                    {(x+1)*unit,0,(z+1)*unit},{x*unit,0,(z+1)*unit}}));
        }
        return List.copyOf(result);
    }
    private SpellVolumeGeometry(){}
}

package com.mcmagic.omnira.client;

import java.util.*;

/** Local-space panel faces only: no atlas, world, fluid or lighting references survive a draw. */
public final class ReactionPoolPanelMesh {
    private record Range(double low,double high){}
    private static final Map<Range,List<float[][]>> CACHE=new LinkedHashMap<>(64,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Range,List<float[][]>> entry){return size()>64;}
    };
    private static final int[][] NEIGHBOURS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    public static List<float[][]> faces(double low,double high){return CACHE.computeIfAbsent(new Range(low,high),ReactionPoolPanelMesh::build);}
    private static List<float[][]> build(Range range){
        double centerY=.75/16,centerZ=-.1/16,unit=1.95/4.1/16;
        var result=new ArrayList<float[][]>();
        for(int y=-4;y<4;y++)for(int x=-4;x<4;x++)for(int z=-4;z<4;z++){
            double yy=centerY+y*unit,lo=Math.max(range.low,yy),hi=Math.min(range.high,yy+unit);
            if(hi<=lo||!cell(x,y,z))continue;
            float a=(float)(x*unit),b=(float)lo,c=(float)(centerZ+z*unit),d=(float)((x+1)*unit),e=(float)hi,f=(float)(centerZ+(z+1)*unit);
            float[][] vertices={{a,b,c},{d,b,c},{d,e,c},{a,e,c},{a,b,f},{d,b,f},{d,e,f},{a,e,f}};
            for(int side=0;side<6;side++){
                var n=NEIGHBOURS[side];if(cell(x+n[0],y+n[1],z+n[2])&&!(side==4&&hi==range.high)&&!(side==5&&lo==range.low))continue;
                var face=FACES[side];result.add(new float[][]{vertices[face[0]],vertices[face[1]],vertices[face[2]],vertices[face[3]]});
            }
        }
        return List.copyOf(result);
    }
    private static boolean cell(int x,int y,int z){return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=4.1*4.1;}
    public static void reset(){CACHE.clear();}
    private ReactionPoolPanelMesh(){}
}

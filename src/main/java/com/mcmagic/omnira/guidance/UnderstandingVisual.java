package com.mcmagic.omnira.guidance;

import java.util.*;

/** Fixed pixel sphere and bounded outward motes, shared by rendering and geometry checks. */
public final class UnderstandingVisual {
    public record Vertex(float x,float y,float z,float shade) {}
    public record Mote(float x,float y,float z,float size,float alpha) {}
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final int[][][] FACES={{{1,0,0},{1,1,0},{1,1,1},{1,0,1}},{{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
            {{0,1,1},{1,1,1},{1,1,0},{0,1,0}},{{0,0,0},{1,0,0},{1,0,1},{0,0,1}},
            {{1,0,1},{1,1,1},{0,1,1},{0,0,1}},{{0,0,0},{0,1,0},{1,1,0},{1,0,0}}};
    public static final List<Vertex> CORE=mesh();
    public static final int MOTES=32;
    private UnderstandingVisual() {}
    private static boolean inside(int x,int y,int z){return x*x+y*y+z*z<=9;}
    private static List<Vertex> mesh(){
        var vertices=new ArrayList<Vertex>();
        for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)if(inside(x,y,z))
            for(int face=0;face<6;face++){
                var d=DIRECTIONS[face];if(inside(x+d[0],y+d[1],z+d[2]))continue;
                for(var p:FACES[face])vertices.add(new Vertex(.5F+(x+p[0]-.5F)/28,.5F+(y+p[1]-.5F)/28,
                        .5F+(z+p[2]-.5F)/28,face==2?1:face==3?.87F:.94F));
            }
        return List.copyOf(vertices);
    }
    public static Mote mote(int index,double time){
        double phase=(time*.013+index*.61803398875)%1;
        double latitude=1-2*(index+.5)/MOTES,angle=index*2.3999632297;
        double radius=.14+.08*phase,horizontal=Math.sqrt(1-latitude*latitude);
        return new Mote(.5F+(float)(Math.cos(angle)*horizontal*radius),.5F+(float)(latitude*radius),
                .5F+(float)(Math.sin(angle)*horizontal*radius),.007F,.8F*(1-(float)phase));
    }
}

package com.mcmagic.omnira.client.renderer;

import java.util.ArrayList;
import java.util.List;

/** Authored one-pixel plates, cached without baking a texture or a render buffer. */
final class GuidanceButtonMesh {
    record Vertex(float x,float y,float z,boolean rim) {}
    static final GuidanceButtonMesh DISC=disc();
    static final GuidanceButtonMesh PLATE=plate();
    private final Vertex[] vertices;
    private GuidanceButtonMesh(List<Vertex> vertices){this.vertices=vertices.toArray(Vertex[]::new);}
    int size(){return vertices.length;}
    Vertex vertex(int index){return vertices[index];}
    private static void rim(List<Vertex> out,float ax,float ay,float bx,float by){
        out.add(new Vertex(ax,ay,-1F/32,true));out.add(new Vertex(bx,by,-1F/32,true));
        out.add(new Vertex(bx,by,1F/32,true));out.add(new Vertex(ax,ay,1F/32,true));
    }
    private static GuidanceButtonMesh disc(){
        int[][] edge={{-2,-4},{2,-4},{2,-3},{3,-3},{3,-2},{4,-2},{4,2},{3,2},{3,3},{2,3},
                {2,4},{-2,4},{-2,3},{-3,3},{-3,2},{-4,2},{-4,-2},{-3,-2},{-3,-3},{-2,-3}};
        var out=new ArrayList<Vertex>(240);
        for(int i=0;i<edge.length;i++){
            float ax=edge[i][0]/16F,ay=edge[i][1]/16F,bx=edge[(i+1)%edge.length][0]/16F,by=edge[(i+1)%edge.length][1]/16F;
            rim(out,ax,ay,bx,by);
            for(int side=-1;side<=1;side+=2){
                float x0=side>0?ax:bx,y0=side>0?ay:by,x1=side>0?bx:ax,y1=side>0?by:ay,z=side/32F;
                out.add(new Vertex(0,0,z,false));out.add(new Vertex(x0,y0,z,false));
                out.add(new Vertex((x0+x1)/2,(y0+y1)/2,z,false));out.add(new Vertex(x1,y1,z,false));
            }
        }
        return new GuidanceButtonMesh(out);
    }
    private static GuidanceButtonMesh plate(){
        float x=12F/32,y=4F/32,c=1F/16;
        float[][] edge={{-x+c,-y},{x-c,-y},{x,-y+c},{x,y-c},{x-c,y},{-x+c,y},{-x,y-c},{-x,-y+c}};
        var out=new ArrayList<Vertex>(56);
        for(int i=0;i<edge.length;i++){
            var a=edge[i];var b=edge[(i+1)%edge.length];rim(out,a[0],a[1],b[0],b[1]);
        }
        int[][] caps={{0,1,2,7},{7,2,3,6},{6,3,4,5}};
        for(int side=-1;side<=1;side+=2)for(var cap:caps)for(int i=0;i<4;i++){
            var v=edge[cap[side>0?i:3-i]];out.add(new Vertex(v[0],v[1],side/32F,false));
        }
        return new GuidanceButtonMesh(out);
    }
}

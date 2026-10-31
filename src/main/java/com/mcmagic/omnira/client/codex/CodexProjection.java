package com.mcmagic.omnira.client.codex;

import com.mcmagic.omnira.codex.edit.EditGeometry;
import com.mcmagic.omnira.codex.edit.Operation;
import net.minecraft.core.Direction;
import java.util.*;

/** Cached exterior block grid, using the execution lattice without hollow filtering. */
public final class CodexProjection {
    private record Edge(int x,int y,int z,int axis) {}
    private static final List<List<CodexGeometry.Face>> MESHES=buildMeshes();
    public static List<CodexGeometry.Face> mesh(int diameter,int shape,int axis){
        if(!EditGeometry.validDiameter(diameter))return List.of();
        int variant=shape==1?1:shape==2?2+Math.clamp(axis,0,2):0;
        return MESHES.get((diameter-7)/2*5+variant);
    }
    public static boolean visibleRing(CodexGeometry.Face face,int shape,int axis){
        if(shape!=2)return true;
        for(var p:face.vertices())if(Math.abs(p[Math.clamp(axis,0,2)]-.5F)>.001F)return false;
        return true;
    }
    private static List<List<CodexGeometry.Face>> buildMeshes(){
        var meshes=new ArrayList<List<CodexGeometry.Face>>();
        for(int diameter=7;diameter<=15;diameter+=2)for(int variant=0;variant<5;variant++)
            meshes.add(build(diameter,variant));
        return List.copyOf(meshes);
    }
    private static List<CodexGeometry.Face> build(int diameter,int variant){
        int r=(diameter-1)/2;
        var op=variant==0?Operation.SHAPE:variant==1?Operation.INSCRIBED:Operation.PLANE;
        var axis=Direction.Axis.values()[variant<2?1:variant-2];
        var edges=new LinkedHashSet<Edge>();
        for(int x=-r;x<=r;x++)for(int y=-r;y<=r;y++)for(int z=-r;z<=r;z++){
            if(!EditGeometry.contains(op,r,axis,false,x,y,z))continue;
            for(var side:Direction.values()){
                if(EditGeometry.contains(op,r,axis,false,x+side.getStepX(),y+side.getStepY(),z+side.getStepZ()))continue;
                int normal=side.getAxis().ordinal(),u=(normal+1)%3,v=(normal+2)%3;
                int[] p={x,y,z};
                if(side.getAxisDirection()==Direction.AxisDirection.POSITIVE)p[normal]++;
                add(edges,p,u);add(edges,p,v);
                p[v]++;add(edges,p,u);p[v]--;p[u]++;add(edges,p,v);
            }
        }
        var faces=new ArrayList<CodexGeometry.Face>(edges.size()*2);
        // Crossed narrow strips remain visible from both sides without broad translucent fills.
        for(var edge:edges)for(int cross=1;cross<=2;cross++){
            float[] a={edge.x,edge.y,edge.z},b=a.clone();b[edge.axis]++;
            int width=(edge.axis+cross)%3;float half=1/32F;
            float[] c=b.clone(),d=a.clone();a[width]-=half;b[width]-=half;c[width]+=half;d[width]+=half;
            faces.add(new CodexGeometry.Face(new float[][]{a,b,c,d},1));
        }
        return List.copyOf(faces);
    }
    private static void add(Set<Edge> edges,int[] p,int axis){edges.add(new Edge(p[0],p[1],p[2],axis));}
    private CodexProjection(){}
}

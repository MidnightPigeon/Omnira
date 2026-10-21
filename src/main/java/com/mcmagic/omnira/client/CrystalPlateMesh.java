package com.mcmagic.omnira.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import java.util.*;

/** Build once: union pixel plates, discard buried faces, then merge coplanar material patches. */
final class CrystalPlateMesh {
    private record Cell(int x,int y,int z) {
        Cell offset(Direction d){return new Cell(x+d.getStepX(),y+d.getStepY(),z+d.getStepZ());}
        int axis(int axis){return axis==0?x:axis==1?y:z;}
    }
    private record Plane(Direction face,int position,int material) {}
    private record Pixel(int u,int v) {}
    private final Map<Cell,Integer> cells=new LinkedHashMap<>();
    private int[] skin;
    private final Map<Integer,Float> ySeams=new HashMap<>();
    private int innerLegSign;
    private float skinClearance=1F/32;

    CrystalPlateMesh skin(int x,int y,int z,int w,int h,int d){skin=new int[]{x,y,z,x+w,y+h,z+d};return this;}
    CrystalPlateMesh seamY(int y,float offset){ySeams.put(y,offset);return this;}
    CrystalPlateMesh leg(int sign){innerLegSign=sign;return this;}
    CrystalPlateMesh clearance(float value){skinClearance=value;return this;}
    private boolean covered(Cell cell){
        return skin!=null&&cell.x>=skin[0]&&cell.x<skin[3]&&cell.y>=skin[1]&&cell.y<skin[4]&&cell.z>=skin[2]&&cell.z<skin[5];
    }
    private float coordinate(int axis,float value,boolean normal){
        // A tiny real clearance prevents skin/armor depth ties without inflating the whole silhouette.
        float result=value;
        if(skin!=null&&normal){if(value==skin[axis])result-=skinClearance;if(value==skin[axis+3])result+=skinClearance;}
        if(axis==1&&ySeams.containsKey((int)value))result=value+ySeams.get((int)value);
        // Vanilla leg pivots are 1.9 pixels apart from the center, not 2: leave a center seam.
        if(axis==0&&value==innerLegSign*-2&&innerLegSign!=0)result=value+innerLegSign*3F/16;
        return result;
    }

    CrystalPlateMesh box(int material,int x,int y,int z,int w,int h,int d){
        for(int a=x;a<x+w;a++)for(int b=y;b<y+h;b++)for(int c=z;c<z+d;c++)cells.put(new Cell(a,b,c),material);
        return this;
    }
    ModelPart bake(float x,float y,float z){
        var planes=new LinkedHashMap<Plane,Set<Pixel>>();
        for(var entry:cells.entrySet())for(var face:Direction.values()){
            var cell=entry.getKey();if(cells.containsKey(cell.offset(face))||covered(cell.offset(face)))continue;
            int axis=face.getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;
            int position=cell.axis(axis)+(face.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0);
            planes.computeIfAbsent(new Plane(face,position,entry.getValue()),key->new HashSet<>()).add(new Pixel(cell.axis(u),cell.axis(v)));
        }
        var cubes=new ArrayList<ModelPart.Cube>();
        for(var entry:planes.entrySet()){
            var plane=entry.getKey();var pixels=entry.getValue();
            while(!pixels.isEmpty()){
                var first=pixels.stream().min(Comparator.comparingInt(Pixel::v).thenComparingInt(Pixel::u)).orElseThrow();
                int width=1,height=1;
                while(pixels.contains(new Pixel(first.u+width,first.v)))width++;
                outer:while(true){
                    for(int i=0;i<width;i++)if(!pixels.contains(new Pixel(first.u+i,first.v+height)))break outer;
                    height++;
                }
                for(int i=0;i<width;i++)for(int j=0;j<height;j++)pixels.remove(new Pixel(first.u+i,first.v+j));
                int axis=plane.face.getAxis().ordinal();float[] origin=new float[3],size=new float[3];
                origin[axis]=plane.position;origin[(axis+1)%3]=first.u;origin[(axis+2)%3]=first.v;
                size[(axis+1)%3]=width;size[(axis+2)%3]=height;
                for(int a=0;a<3;a++){
                    float end=coordinate(a,origin[a]+size[a],a==axis);origin[a]=coordinate(a,origin[a],a==axis);size[a]=end-origin[a];
                }
                cubes.add(new ModelPart.Cube(0,plane.material*32,origin[0],origin[1],origin[2],size[0],size[1],size[2],
                        0,0,0,false,64,128,Set.of(plane.face)));
            }
        }
        var part=new ModelPart(cubes,Map.of());part.setPos(x,y,z);return part;
    }
}

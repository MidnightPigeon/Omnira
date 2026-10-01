package com.mcmagic.omnira.aggregation;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static frame collision leaves the central aperture open despite visual bobbing. */
public final class AggregationCollision {
    private static final VoxelShape[][] SHAPES=new VoxelShape[9][4];
    static {
        for(int part=0;part<9;part++)for(var facing:Direction.Plane.HORIZONTAL){
            int px=part%3-1,py=part/3-1;
            var shape=Shapes.empty();
            for(int y=py*16-8;y<py*16+8;y++){
                int start=px*16-8,end=start+16;
                while(start<end){
                    if(!occupied(start,y)){start++;continue;}
                    int stop=start+1;
                    while(stop<end&&occupied(stop,y))stop++;
                    double a=start/16.0-px,b=stop/16.0-px;
                    var right=facing.getCounterClockWise();
                    double x1=.5+right.getStepX()*a-facing.getStepX()*.1875;
                    double x2=.5+right.getStepX()*b+facing.getStepX()*.1875;
                    double z1=.5+right.getStepZ()*a-facing.getStepZ()*.1875;
                    double z2=.5+right.getStepZ()*b+facing.getStepZ()*.1875;
                    shape=Shapes.or(shape,Shapes.box(Math.min(x1,x2),.5+y/16.0-py,Math.min(z1,z2),
                            Math.max(x1,x2),.5+(y+1)/16.0-py,Math.max(z1,z2)));
                    start=stop;
                }
            }
            SHAPES[part][facing.get2DDataValue()]=shape.optimize();
        }
    }
    private static boolean occupied(int x,int y){
        double u=Math.abs(x+.5),v=Math.abs(y+.5);
        if(Math.max(u,v)<=20.5&&u+v<=29&&!(Math.max(u,v)<18.5&&u+v<26))return true;
        for(var node:AggregationLayout.NODES)
            if(Math.hypot(x+.5-node[0]*16,y+.5-node[1]*16)<3)return true;
        return false;
    }
    public static VoxelShape shape(int part,Direction facing){return SHAPES[part][facing.get2DDataValue()];}
    private AggregationCollision(){}
}

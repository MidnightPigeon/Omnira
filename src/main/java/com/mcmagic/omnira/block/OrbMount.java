package com.mcmagic.omnira.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import org.joml.Quaternionf;

/** Local +Y is the direction away from the supporting face. */
public final class OrbMount {
    public static Quaternionf rotation(Direction face){
        return switch(face){
            case UP->new Quaternionf();
            case DOWN->new Quaternionf().rotationX((float)Math.PI);
            case NORTH->new Quaternionf().rotationX((float)-Math.PI/2);
            case SOUTH->new Quaternionf().rotationX((float)Math.PI/2);
            case EAST->new Quaternionf().rotationZ((float)-Math.PI/2);
            case WEST->new Quaternionf().rotationZ((float)Math.PI/2);
        };
    }
    public static Vec3 point(Direction face,Vec3 point){
        var p=point.subtract(.5,.5,.5).toVector3f();rotation(face).transform(p);
        return new Vec3(p.x+.5,p.y+.5,p.z+.5);
    }
    public static VoxelShape shape(Direction face,VoxelShape shape){
        var result=Shapes.empty();
        for(var box:shape.toAabbs()){
            double minX=1,minY=1,minZ=1,maxX=0,maxY=0,maxZ=0;
            for(double x:new double[]{box.minX,box.maxX})for(double y:new double[]{box.minY,box.maxY})for(double z:new double[]{box.minZ,box.maxZ}){
                var p=point(face,new Vec3(x,y,z));
                minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);minZ=Math.min(minZ,p.z);
                maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);maxZ=Math.max(maxZ,p.z);
            }
            result=Shapes.or(result,Shapes.box(Math.max(0,minX),Math.max(0,minY),Math.max(0,minZ),Math.min(1,maxX),Math.min(1,maxY),Math.min(1,maxZ)));
        }
        return result.optimize();
    }
    private OrbMount(){}
}

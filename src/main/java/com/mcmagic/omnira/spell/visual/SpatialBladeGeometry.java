package com.mcmagic.omnira.spell.visual;

import net.minecraft.world.phys.Vec3;

/** Local axes: transverse X, thickness Y, forward Z. Shared by rendering and geometry checks. */
public final class SpatialBladeGeometry {
    public static final double THICKNESS=1.0/32, TRAIL_LENGTH=2.4;
    private static final int[] EDGE={3,2,1,0,0,1,2,3};
    private SpatialBladeGeometry(){}
    public record Basis(Vec3 right,Vec3 up,Vec3 forward){
        public Vec3 point(double x,double y,double z){return right.scale(x).add(up.scale(y)).add(forward.scale(z));}
    }
    public static Basis basis(Vec3 direction){
        Vec3 forward=direction.lengthSqr()<1e-8?new Vec3(0,0,1):direction.normalize();
        Vec3 right=forward.cross(new Vec3(0,1,0));
        right=right.lengthSqr()<1e-8?new Vec3(1,0,0):right.normalize();
        return new Basis(right,right.cross(forward).normalize(),forward);
    }
    public static double front(int column){return -EDGE[column]*.125;}
    public static double back(int column){return front(column)-(column==0||column==7?.0625:.1875);}
    public static double trailLength(float age,int delay){return Math.min(TRAIL_LENGTH,Math.max(0,age-delay)*.4);}
}

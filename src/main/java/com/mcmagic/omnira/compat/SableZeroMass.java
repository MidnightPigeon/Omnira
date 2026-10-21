package com.mcmagic.omnira.compat;

import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Numerical rigid-body floor only; source block masses and the native self tracker remain zero. */
public final class SableZeroMass implements MassData {
    public static final double MASS=.001;
    private final Vector3d center=new Vector3d();
    private final Matrix3d inertia=new Matrix3d(),inverse=new Matrix3d();
    private int x0,y0,z0,x1,y1,z1;
    private boolean initialized;
    public SableZeroMass refresh(ServerSubLevel ship){
        var box=ship.getPlot().getBoundingBox();
        if(initialized&&x0==box.minX()&&y0==box.minY()&&z0==box.minZ()&&x1==box.maxX()&&y1==box.maxY()&&z1==box.maxZ())return this;
        initialized=true;x0=box.minX();y0=box.minY();z0=box.minZ();x1=box.maxX();y1=box.maxY();z1=box.maxZ();
        double w=Math.max(1,box.width()),h=Math.max(1,box.height()),d=Math.max(1,box.length());
        if(x0>x1||y0>y1||z0>z1){var anchor=ship.getPlot().getCenterBlock();center.set(anchor.getX()+.5,anchor.getY()+.5,anchor.getZ()+.5);}
        else center.set(((double)x0+x1+1)/2,((double)y0+y1+1)/2,((double)z0+z1+1)/2);
        double ix=MASS*(h*h+d*d)/12,iy=MASS*(w*w+d*d)/12,iz=MASS*(w*w+h*h)/12;
        inertia.scaling(ix,iy,iz);inverse.scaling(1/ix,1/iy,1/iz);return this;
    }
    public double getMass(){return MASS;}
    public double getInverseMass(){return 1/MASS;}
    public Matrix3dc getInertiaTensor(){return inertia;}
    public Matrix3dc getInverseInertiaTensor(){return inverse;}
    public Vector3dc getCenterOfMass(){return center;}
}

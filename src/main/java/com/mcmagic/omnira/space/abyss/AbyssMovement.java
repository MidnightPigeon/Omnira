package com.mcmagic.omnira.space.abyss;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AbyssMovement {
    public static Vec3 ceilingStep(Entity entity,Vec3 desired,Vec3 ordinary){
        float step=entity.maxUpStep();
        if(!AbyssGravity.inverted(entity)||step<=0||(!entity.onGround()&&!(desired.y>0&&ordinary.y!=desired.y))
                ||ordinary.horizontalDistanceSqr()>=desired.horizontalDistanceSqr()-1e-9)return ordinary;
        var box=entity.getBoundingBox();var level=entity.level();
        var hits=level.getEntityCollisions(entity,box.expandTowards(desired).inflate(step));
        var down=Entity.collideBoundingBox(entity,new Vec3(0,-step,0),box,level,hits);
        var horizontal=Entity.collideBoundingBox(entity,new Vec3(desired.x,0,desired.z),box.move(down),level,hits);
        if(horizontal.horizontalDistanceSqr()<=ordinary.horizontalDistanceSqr())return ordinary;
        var raised=Entity.collideBoundingBox(entity,new Vec3(0,-down.y+desired.y,0),box.move(down).move(horizontal),level,hits);
        return down.add(horizontal).add(raised);
    }
    public static Vec3 ceilingEdge(Entity entity,Vec3 desired){
        double x=desired.x,z=desired.z,step=entity.maxUpStep();
        while(x!=0&&entity.level().noCollision(entity,entity.getBoundingBox().move(x,step,0)))x=reduce(x);
        while(z!=0&&entity.level().noCollision(entity,entity.getBoundingBox().move(0,step,z)))z=reduce(z);
        while(x!=0&&z!=0&&entity.level().noCollision(entity,entity.getBoundingBox().move(x,step,z))){x=reduce(x);z=reduce(z);}
        return new Vec3(x,desired.y,z);
    }
    private static double reduce(double value){return Math.abs(value)<=.05?0:value-Math.copySign(.05,value);}
    private AbyssMovement(){}
}

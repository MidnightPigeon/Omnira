package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.vehicle.CruiseVortex;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.UUID;
import org.joml.Vector3d;

/** Forces act on the physics body, never on its constituent blocks or pose. */
public final class SableVortex {
    private SableVortex(){}
    public static void apply(ServerLevel level,Vec3 center,boolean expanding,double radius,Set<UUID> pushed){
        var container=SubLevelContainer.getContainer(level);if(container==null)return;
        var pipeline=container.physicsSystem().getPipeline();
        var search=new dev.ryanhcode.sable.companion.math.BoundingBox3d(new net.minecraft.world.phys.AABB(center,center).inflate(CruiseVortex.MAX_RADIUS));
        for(var candidate:container.queryIntersecting(search)){
            if(!(candidate instanceof ServerSubLevel ship) || ship.isRemoved())continue;
            var box=ship.boundingBox().toMojang();
            var nearest=new Vec3(Math.clamp(center.x,box.minX,box.maxX),Math.clamp(center.y,box.minY,box.maxY),Math.clamp(center.z,box.minZ,box.maxZ));
            if(nearest.distanceToSqr(center)>Math.pow(expanding?radius:CruiseVortex.MAX_RADIUS,2))continue;
            var delta=box.getCenter().subtract(center);if(delta.lengthSqr()<.001)continue;
            double factor=Math.min(1,Math.sqrt(512/Math.max(1,ship.getMassTracker().getMass())));
            if(expanding && !pushed.add(ship.getUniqueId()))continue;
            var direction=delta.normalize().scale(expanding?1:-1);
            var velocity=pipeline.getLinearVelocity(ship,new Vector3d());
            if(!expanding && velocity.dot(direction.x,direction.y,direction.z)>=1.5)continue;
            var impulse=direction.scale((expanding?.25:.025)*factor);
            pipeline.wakeUp(ship);
            pipeline.addLinearAndAngularVelocity(ship,new Vector3d(impulse.x,impulse.y,impulse.z),new Vector3d());
        }
    }
}

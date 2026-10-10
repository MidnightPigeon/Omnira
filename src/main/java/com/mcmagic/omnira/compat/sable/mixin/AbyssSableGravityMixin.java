package com.mcmagic.omnira.compat.sable.mixin;

import com.mcmagic.omnira.space.abyss.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counteracts native uniform gravity once per physics substep, keeping other forces intact. */
@Mixin(value=ServerSubLevel.class,remap=false)
abstract class AbyssSableGravityMixin {
    @Inject(method="prePhysicsTick",at=@At("HEAD"))
    private void omnira$gravity(SubLevelPhysicsSystem system,RigidBodyHandle handle,double dt,CallbackInfo ci){
        var ship=(ServerSubLevel)(Object)this;
        var point=ship.boundingBox().toMojang().getCenter();var level=ship.getLevel();
        int direction=AbyssGravity.direction(level,point);
        var correction=new Vector3d(DimensionPhysicsData.getGravity(level)).mul((direction-1)*dt);
        var pull=VoidAttraction.acceleration(level,point);
        // Vanilla entity acceleration is blocks/tick squared; Sable uses blocks/second squared.
        correction.add(pull.x*400*dt,pull.y*400*dt,pull.z*400*dt);
        if(correction.lengthSquared()>0)handle.addLinearAndAngularVelocity(correction,new Vector3d());
    }
}

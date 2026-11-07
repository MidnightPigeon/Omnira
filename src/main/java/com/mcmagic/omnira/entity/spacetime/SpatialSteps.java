package com.mcmagic.omnira.entity.spacetime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Loaded, dry, collision-free platforms only. All distances are three-dimensional. */
public final class SpatialSteps {
    public static boolean clear(Mob mob,Vec3 destination){
        var level=mob.level();var feet=BlockPos.containing(destination);
        if(!level.hasChunkAt(feet)||!level.getWorldBorder().isWithinBounds(feet))return false;
        var box=mob.getBoundingBox().move(destination.subtract(mob.position()));
        if(box.minY<level.getMinBuildHeight()||box.maxY>level.getMaxBuildHeight())return false;
        for(int cx=net.minecraft.util.Mth.floor(box.minX)>>4;cx<=(net.minecraft.util.Mth.floor(box.maxX)>>4);cx++)
            for(int cz=net.minecraft.util.Mth.floor(box.minZ)>>4;cz<=(net.minecraft.util.Mth.floor(box.maxZ)>>4);cz++)if(!level.hasChunk(cx,cz))return false;
        return level.noCollision(mob,box)&&!level.containsAnyLiquid(box);
    }
    public static boolean safe(Mob mob,Vec3 destination){
        var feet=BlockPos.containing(destination);
        return clear(mob,destination)&&mob.level().getBlockState(feet.below()).isFaceSturdy(mob.level(),feet.below(),Direction.UP);
    }
    public static boolean teleport(Mob mob,Vec3 destination,double limit) {
        if(mob.level().isClientSide||mob.position().distanceToSqr(destination)>limit*limit+.001||!safe(mob,destination))return false;
        var level=(ServerLevel)mob.level(); particles(level,mob.position());
        mob.getNavigation().stop();mob.teleportTo(destination.x,destination.y,destination.z);
        mob.setDeltaMovement(Vec3.ZERO);mob.fallDistance=0;particles(level,destination);return true;
    }
    private static void particles(ServerLevel level,Vec3 p){
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,p.x,p.y+.5,p.z,8,.25,.4,.25,.02);
    }
    public static Vec3 platform(Mob mob,int radius) {
        // Upper platforms are considered before lower ones; bounded local work, no chunk loads.
        var origin=mob.blockPosition();
        for(int sign:new int[]{1,-1})for(int y=1;y<=Math.min(radius,6);y++)for(int sample=0;sample<12;sample++){
            double angle=sample*Math.PI/6;int reach=2+sample%Math.max(1,radius-1);
            var p=origin.offset((int)Math.round(Math.cos(angle)*reach),y*sign,(int)Math.round(Math.sin(angle)*reach));
            var v=Vec3.atBottomCenterOf(p);
            if(mob.position().distanceToSqr(v)<=radius*radius&&safe(mob,v))return v;
        }
        return null;
    }
    private SpatialSteps(){}
}

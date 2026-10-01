package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.spacetime.SpacetimeStorm;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;

public final class SableStorm {
    private SableStorm(){}
    public static void erase(ServerLevel level,Vec3 center){
        var container=SubLevelContainer.getContainer(level);if(container==null)return;
        var search=new dev.ryanhcode.sable.companion.math.BoundingBox3d(new AABB(center,center).inflate(SpacetimeStorm.RADIUS));
        var ships=new java.util.ArrayList<ServerSubLevel>();
        for(var candidate:container.queryIntersecting(search))
            if(candidate instanceof ServerSubLevel ship && !ship.isRemoved() && SpacetimeStorm.intersects(center,ship.boundingBox().toMojang()))ships.add(ship);
        for(var ship:ships){
            var pose=ship.logicalPose();
            var local=pose.transformPositionInverse(center);
            var scale=pose.scale();
            int radius=(int)Math.ceil(SpacetimeStorm.RADIUS/Math.min(Math.abs(scale.x()),Math.min(Math.abs(scale.y()),Math.abs(scale.z()))))+1;
            var erased=new java.util.HashSet<BlockPos>();
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(local).offset(-radius,-radius,-radius),BlockPos.containing(local).offset(radius,radius,radius))){
                if(!ship.getPlot().contains(Vec3.atCenterOf(pos)) || !level.hasChunkAt(pos) || level.getBlockState(pos).isAir())continue;
                var nearest=new Vec3(Math.clamp(local.x,pos.getX(),pos.getX()+1),Math.clamp(local.y,pos.getY(),pos.getY()+1),Math.clamp(local.z,pos.getZ(),pos.getZ()+1));
                if(pose.transformPosition(nearest).distanceToSqr(center)<=SpacetimeStorm.RADIUS*SpacetimeStorm.RADIUS)erased.add(pos.immutable());
            }
            for(var pos:erased)SpacetimeStorm.disableDamagedForge(level,pos);
            for(var pos:erased)level.removeBlockEntity(pos);
            for(var pos:erased)level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            for(var entity:level.getAllEntities())if(ship.getPlot().contains(entity.position())){
                var point=pose.transformPosition(entity.position());
                if(SpacetimeStorm.intersects(center,entity.getBoundingBox().move(point.subtract(entity.position())))){
                    if(net.neoforged.fml.ModList.get().isLoaded("create") && CreateStorm.erase(entity,local))continue;
                    SpacetimeStorm.affect(level,entity);
                }
            }
        }
    }
}

package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TimeWarpPointBlockEntity extends BlockEntity {
    public TimeWarpPointBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.TIME_WARP_POINT.get(),pos,state);}
    public void serverTick(){
        if(!(level instanceof ServerLevel server))return;
        Vec3 center=Vec3.atCenterOf(worldPosition);
        for(var entity:server.getEntities((net.minecraft.world.entity.Entity)null,new AABB(worldPosition).inflate(TimeWarpPointBlock.RADIUS+2),
                e->e.isAlive() && !e.isSpectator())){
            var box=entity.getBoundingBox();
            double x=Math.clamp(center.x,box.minX,box.maxX),y=Math.clamp(center.y,box.minY,box.maxY),z=Math.clamp(center.z,box.minZ,box.maxZ);
            if(center.distanceToSqr(x,y,z)<=TimeWarpPointBlock.RADIUS*TimeWarpPointBlock.RADIUS)
                TimeWarpEffects.touch(entity,server.getGameTime());
        }
    }
}

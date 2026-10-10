package com.mcmagic.omnira.space;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public final class SpaceWarpBlockEntity extends BlockEntity {
    public SpaceWarpBlockEntity(BlockPos p,BlockState s){super(com.mcmagic.omnira.registry.ModBlockEntityTypes.SPACE_WARP_POINT.get(),p,s);}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel server)SpaceWarpIndex.get(server).add(worldPosition);}
    public static boolean touches(AABB box,Vec3 center){return center.distanceToSqr(Math.clamp(center.x,box.minX,box.maxX),Math.clamp(center.y,box.minY,box.maxY),Math.clamp(center.z,box.minZ,box.maxZ))<=25;}
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        long now=server.getGameTime();var seen=new java.util.HashSet<java.util.UUID>();
        for(var entity:server.getEntities((Entity)null,new AABB(worldPosition).inflate(7),e->e.isAlive()&&!e.isSpectator())){
            var root=entity.getRootVehicle();if(!seen.add(root.getUUID())||!com.mcmagic.omnira.world.dimension.DreamTransitRules.allowed(root))continue;
            if(root.getSelfAndPassengers().noneMatch(e->touches(e.getBoundingBox(),Vec3.atCenterOf(worldPosition))))continue;
            var data=root.getPersistentData();if(data.getLong("OmniraFoldCooldown")>now)continue;
            String dimension=server.dimension().location().toString();
            boolean ongoing=data.contains("OmniraFoldStart")&&dimension.equals(data.getString("OmniraFoldDimension"))&&now-data.getLong("OmniraFoldTouch")<=1;
            if(ongoing&&data.getLong("OmniraFoldSource")!=worldPosition.asLong())continue;
            if(!ongoing){data.putLong("OmniraFoldStart",now);data.putLong("OmniraFoldSource",worldPosition.asLong());data.putString("OmniraFoldDimension",dimension);}
            data.putLong("OmniraFoldTouch",now);
            if(now%10==0)server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,root.getX(),root.getY()+.5,root.getZ(),6,.35,.5,.35,.05);
            if(now-data.getLong("OmniraFoldStart")>=60){
                if(teleport(server,root,worldPosition))data.remove("OmniraFoldStart");
                else data.putLong("OmniraFoldStart",now-40);
            }
        }
    }
    public static boolean teleport(ServerLevel level,Entity root,BlockPos source){
        var candidates=SpaceWarpIndex.get(level).nearby(level,source);
        for(int attempt=0;attempt<8&&!candidates.isEmpty();attempt++){
            var target=candidates.remove(level.random.nextInt(candidates.size()));
            if(!level.getWorldBorder().isWithinBounds(target))continue;
            level.getChunkAt(target);
            if(!level.getBlockState(target).is(com.mcmagic.omnira.registry.ModBlocks.SPACE_WARP_POINT.get())){SpaceWarpIndex.get(level).remove(target);continue;}
            Vec3 landing=landing(level,root,target);if(landing==null)continue;
            for(var member:root.getSelfAndPassengers().toList())member.getPersistentData().putLong("OmniraFoldCooldown",level.getGameTime()+100);
            root.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(level,landing,Vec3.ZERO,root.getYRot(),root.getXRot(),e->{e.fallDistance=0;e.setDeltaMovement(Vec3.ZERO);}));
            return true;
        }
        return false;
    }
    public static Vec3 landing(ServerLevel level,Entity root,BlockPos target){
        var members=root.getSelfAndPassengers().toList();int start=level.random.nextInt(16);
        for(int radius=6;radius<=10;radius++)for(int i=0;i<16;i++){
            double angle=(i+start)*Math.PI/8;
            int x=target.getX()+(int)Math.round(Math.cos(angle)*radius),z=target.getZ()+(int)Math.round(Math.sin(angle)*radius);
            for(int dy=4;dy>=-6;dy--){
                var pos=new BlockPos(x,target.getY()+dy,z);if(!level.getWorldBorder().isWithinBounds(pos))continue;
                level.getChunkAt(pos);
                if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))continue;
                Vec3 point=Vec3.atBottomCenterOf(pos),delta=point.subtract(root.position());boolean safe=true;
                for(var member:members){
                    var box=member.getBoundingBox().move(delta);
                    if(box.minY<level.getMinBuildHeight()||box.maxY>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(box)||touches(box,Vec3.atCenterOf(target))){safe=false;break;}
                    if(!level.hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))||!level.noCollision(box)||level.containsAnyLiquid(box)){safe=false;break;}
                }
                if(safe)return point;
            }
        }
        return null;
    }
}

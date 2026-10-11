package com.mcmagic.omnira.space;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** Bounded searches never dig an arrival chamber or move entities into solid/liquid terrain. */
public final class FoldGateTransit {
    private FoldGateTransit(){}
    public static float unstableDamage(float maximumHealth,double roll){return (float)Math.ceil(maximumHealth*(.2+Math.clamp(roll,0,1)*.3));}
    public static Vec3 landing(ServerLevel level,Entity root,BlockPos origin,int horizontal,int vertical){
        for(int r=0;r<=horizontal;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            for(int dy=0;dy<=vertical*2;dy++){
                int y=origin.getY()+(dy%2==0?dy/2:-(dy+1)/2);var p=new BlockPos(origin.getX()+dx,y,origin.getZ()+dz);
                if(y<=level.getMinBuildHeight()||y+root.getBbHeight()>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(p))continue;
                level.getChunkAt(p);
                if(!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
                var point=Vec3.atBottomCenterOf(p);boolean safe=true;
                for(var member:root.getSelfAndPassengers().toList()){
                    var box=member.getBoundingBox().move(point.subtract(root.position()));
                    for(int x=net.minecraft.util.Mth.floor(box.minX)>>4;x<=net.minecraft.util.Mth.floor(box.maxX)>>4;x++)
                        for(int z=net.minecraft.util.Mth.floor(box.minZ)>>4;z<=net.minecraft.util.Mth.floor(box.maxZ)>>4;z++)level.getChunk(x,z);
                    if(box.minY<level.getMinBuildHeight()||box.maxY>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(box)||!level.noCollision(box)||level.containsAnyLiquid(box)){safe=false;break;}
                }
                if(safe)return point;
            }
        }
        return null;
    }
    public static boolean transfer(FoldGateBlockEntity gate,Entity root,boolean stable){
        if(!(gate.getLevel() instanceof ServerLevel source))return false;
        ServerLevel destination;Vec3 arrival=null;float yaw=root.getYRot();
        if(stable){
            if(gate.peer==null)return false;
            var other=FoldGateBlockEntity.resolve(source.getServer(),gate.peer);
            if(other==null||!FoldGateBlock.stable(other.getBlockState())||!other.identity.equals(gate.peerIdentity)||!gate.identity.equals(other.peerIdentity)
                    ||other.peer==null||!other.peer.equals(GlobalPos.of(source.dimension(),gate.getBlockPos()))){gate.unlink();return false;}
            destination=(ServerLevel)other.getLevel();var direction=other.getBlockState().getValue(FoldGateBlock.FACING);
            for(var side:new Direction[]{direction,direction.getOpposite()}){
                arrival=landing(destination,root,other.getBlockPos().relative(side,2),1,2);if(arrival!=null)break;
            }
            yaw=direction.toYRot();
        }else{
            var worlds=new java.util.ArrayList<ServerLevel>();source.getServer().getAllLevels().forEach(worlds::add);
            destination=worlds.get(source.random.nextInt(worlds.size()));
            double scale=source.dimensionType().coordinateScale()/destination.dimensionType().coordinateScale();
            double mappedX=gate.getBlockPos().getX()*scale,mappedZ=gate.getBlockPos().getZ()*scale;
            for(int attempt=0;attempt<16&&arrival==null;attempt++){
                double angle=source.random.nextDouble()*Math.PI*2,distance=128+source.random.nextDouble()*384;
                int x=net.minecraft.util.Mth.floor(mappedX+Math.cos(angle)*distance),z=net.minecraft.util.Mth.floor(mappedZ+Math.sin(angle)*distance);
                var origin=new BlockPos(x,Math.clamp(gate.getBlockPos().getY(),destination.getMinBuildHeight()+1,destination.getMaxBuildHeight()-3),z);
                arrival=landing(destination,root,origin,0,destination.getHeight());
            }
            if(arrival==null)return false;
            for(var member:root.getSelfAndPassengers().toList())if(member instanceof ServerPlayer p&&!p.isCreative()&&!p.isSpectator()){
                float damage=unstableDamage(p.getMaxHealth(),source.random.nextDouble());
                p.setHealth(Math.max(0,p.getHealth()-damage));
                com.mcmagic.omnira.space.phase.PhaseConcealment.afterDamage(p);
                if(p.getHealth()<=0){p.die(source.damageSources().genericKill());return false;}
            }
        }
        if(arrival==null)return false;
        long cooldown=source.getServer().overworld().getGameTime()+60;
        root.getSelfAndPassengers().forEach(e->e.getPersistentData().putLong("OmniraFoldGateCooldown",cooldown));
        return com.mcmagic.omnira.space.anchor.StabilityField.portalTravel(root,new DimensionTransition(destination,arrival,Vec3.ZERO,yaw,root.getXRot(),
                DimensionTransition.PLACE_PORTAL_TICKET.then(e->e.fallDistance=0)))!=null;
    }
}

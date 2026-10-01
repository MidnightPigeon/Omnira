package com.mcmagic.omnira.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.mcmagic.omnira.spell.SpellCasting;

/** One planar fan, shared by target selection and its expanding visual arc. */
public final class NailSlash {
    public static final double HALF_ANGLE=Math.toRadians(55);
    public static Vec3 right(Vec3 forward) {
        return forward.cross(Math.abs(forward.y)>.99?new Vec3(0,0,1):new Vec3(0,1,0)).normalize();
    }
    public static Vec3 point(Vec3 origin,Vec3 forward,double radius,double angle) {
        return origin.add(forward.scale(Math.cos(angle)*radius)).add(right(forward).scale(Math.sin(angle)*radius));
    }
    public static List<Entity> targets(ServerPlayer player,double reach) {
        Vec3 origin=player.getEyePosition(),forward=player.getLookAngle(),right=right(forward),up=right.cross(forward);
        List<Entity> result=new ArrayList<>();var seen=new HashSet<UUID>();
        for(var target:player.level().getEntities(player,new AABB(origin,origin).inflate(reach+1),
                e->e.isAlive() && !e.isSpectator() && e.isPickable() && e.isAttackable())) {
            var box=target.getBoundingBox();var delta=box.getCenter().subtract(origin);
            double ahead=delta.dot(forward);
            if(ahead<0 || Math.abs(delta.dot(right))>ahead*Math.tan(HALF_ANGLE)+target.getBbWidth()/2
                    || Math.abs(delta.dot(up))>.9+target.getBbHeight()/2)continue;
            Vec3 closest=new Vec3(Math.clamp(origin.x,box.minX,box.maxX),Math.clamp(origin.y,box.minY,box.maxY),Math.clamp(origin.z,box.minZ,box.maxZ));
            if(closest.distanceToSqr(origin)>reach*reach)continue;
            if(player.level().clip(new ClipContext(origin,box.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=HitResult.Type.MISS)continue;
            var living=SpellCasting.livingTarget(target);
            if(seen.add(living==null?target.getUUID():living.getUUID()))result.add(target);
        }
        result.sort(java.util.Comparator.comparingDouble(e->e.distanceToSqr(player)));
        return result;
    }
    public static void release(ServerPlayer player) {
        double reach=player.entityInteractionRange()*2;
        var weapon=player.getMainHandItem();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(player.serverLevel(),null,
                player.getX(),player.getY(),player.getZ(),64,
                new com.mcmagic.omnira.network.NailSlashPayload(player.getEyePosition(),player.getLookAngle(),(float)reach));
        player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                net.minecraft.sounds.SoundSource.PLAYERS,1,.8F);
        for(int i=-5;i<=5;i++){
            Vec3 point=point(player.getEyePosition(),player.getLookAngle(),reach,i*HALF_ANGLE/5);
            var hit=player.level().clip(new ClipContext(player.getEyePosition(),point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
            if(hit instanceof net.minecraft.world.phys.BlockHitResult block)
                com.mcmagic.omnira.spacetime.TimeWarpPointBlock.harvest(player.serverLevel(),block.getBlockPos(),player);
        }
        for(var target:targets(player,reach)) {
            if(!player.isAlive() || player.getMainHandItem()!=weapon)break;
            SwordActions.strike(player,target,true);
        }
    }
    private NailSlash() {}
}

package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.world.dimension.DreamTransitRules;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="omnira")
public final class VoidSeaEffects {
    public static boolean trueDamage(LivingEntity target,float damage,DamageSource source){
        if(!target.isAlive()||target.isSpectator()||target instanceof Player p&&p.isCreative())return false;
        if(source.getEntity() instanceof LivingEntity attacker)target.setLastHurtByMob(attacker);
        target.setHealth(Math.max(0,target.getHealth()-damage));
        if(target.level() instanceof ServerLevel server)server.broadcastDamageEvent(target,source);
        if(target.getHealth()<=0)target.die(source);
        return true;
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        Entity e=event.getEntity();
        if(!(e.level() instanceof ServerLevel level)||!e.isAlive()||SpatialImmunity.starflow(e)||!e.isInFluidType(VoidSeaContent.TYPE.get()))return;
        var data=e.getPersistentData();long now=level.getGameTime();
        if(data.contains("OmniraStarflowHit")&&now-data.getLong("OmniraStarflowHit")<20)return;
        data.putLong("OmniraStarflowHit",now);
        if(e instanceof LivingEntity living)trueDamage(living,1,living.damageSources().genericKill());
        else e.hurt(e.damageSources().genericKill(),1);
        if(!e.isAlive())return;
        Entity root=e.getRootVehicle();
        if(root.getPersistentData().getLong("OmniraStarflowTeleportUntil")>now||!DreamTransitRules.allowed(root))return;
        root.getPersistentData().putLong("OmniraStarflowTeleportUntil",now+20);
        teleport(level,root);
    }
    public static boolean teleport(ServerLevel level,Entity root){
        var members=root.getSelfAndPassengers().toList();
        // Do not let an unprotected passenger teleport an otherwise protected mount/group.
        if(members.stream().anyMatch(SpatialImmunity::starflow))return false;
        for(int attempt=0;attempt<16;attempt++){
            BlockPos p=root.blockPosition().offset(level.random.nextInt(33)-16,level.random.nextInt(17)-8,level.random.nextInt(33)-16);
            if(!level.hasChunkAt(p))continue;
            for(int down=0;down<16&&p.getY()>level.getMinBuildHeight()+1&&!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP);down++)p=p.below();
            if(!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
            Vec3 point=Vec3.atBottomCenterOf(p),delta=point.subtract(root.position());boolean safe=true;
            for(Entity member:members){
                AABB box=member.getBoundingBox().move(delta);
                if(box.minY<level.getMinBuildHeight()||box.maxY>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(box)
                        ||!level.hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))
                        ||!level.noCollision(box)||level.containsAnyLiquid(box)){safe=false;break;}
            }
            if(!safe)continue;
            level.sendParticles(ParticleTypes.PORTAL,root.getX(),root.getY()+.5,root.getZ(),12,.4,.4,.4,.1);
            root.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(level,point,Vec3.ZERO,root.getYRot(),root.getXRot(),e->{e.fallDistance=0;e.setDeltaMovement(Vec3.ZERO);}));
            return true;
        }
        return false;
    }
    private VoidSeaEffects(){}
}

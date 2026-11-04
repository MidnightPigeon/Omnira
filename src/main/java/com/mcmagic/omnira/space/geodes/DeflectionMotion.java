package com.mcmagic.omnira.space.geodes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="omnira")
public final class DeflectionMotion {
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){sample(event.getEntity());}
    public static void sample(Entity entity){
        if(!(entity.level() instanceof ServerLevel level)||!entity.isAlive()||entity.isSpectator())return;
        var data=entity.getPersistentData();
        int count=DeflectionSources.coverage(level,entity);
        boolean pending=data.getFloat("DeflectionPending")!=0;
        if((count>0||pending||entity instanceof ServerPlayer)&&com.mcmagic.omnira.space.anchor.StabilityField.contains(entity)){
            clearTrack(data);
            data.remove("DeflectionPending");
            if(entity instanceof ServerPlayer player&&!data.getBoolean("StabilityDeflectionInside")){
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new DeflectionPayload(0));
                data.putBoolean("StabilityDeflectionInside",true);
            }
            return;
        }
        if(data.contains("StabilityDeflectionInside"))data.remove("StabilityDeflectionInside");
        if(count==0&&!pending){clearTrack(data);return;}
        long now=level.getGameTime();
        boolean sameDimension=data.getString("DeflectionDimension").equals(level.dimension().location().toString());
        if(sameDimension&&data.contains("DeflectionSampleTick")&&data.getLong("DeflectionSampleTick")==now)return;
        Vec3 current=entity.position();
        boolean consecutive=data.contains("DeflectionSampleTick")&&data.getLong("DeflectionSampleTick")==now-1
                &&sameDimension;
        double moved=consecutive?current.distanceTo(new Vec3(data.getDouble("DeflectionX"),data.getDouble("DeflectionY"),data.getDouble("DeflectionZ"))):0;
        data.putLong("DeflectionSampleTick",now);data.putString("DeflectionDimension",level.dimension().location().toString());
        data.putDouble("DeflectionX",current.x);data.putDouble("DeflectionY",current.y);data.putDouble("DeflectionZ",current.z);
        if(!consecutive||moved>32){
            data.remove("DeflectionRemainder");data.remove("DeflectionCurveSign");
            if(data.getLong("DeflectionQueuedAt")!=now)data.remove("DeflectionPending");
            data.putBoolean("DeflectionInside",count>0);return;
        }
        if(moved<=0)return;
        advance(entity);

        if(count==0){data.remove("DeflectionCurveSign");data.remove("DeflectionRemainder");data.remove("DeflectionInside");return;}
        boolean inside=data.getBoolean("DeflectionInside");data.putBoolean("DeflectionInside",true);
        if(!inside)return;
        double progress=data.getDouble("DeflectionRemainder"),degrees=0;
        // Keep fractional steps smooth, but choose independently for every block travelled.
        while(moved>1e-9){
            if(!data.contains("DeflectionCurveSign"))data.putInt("DeflectionCurveSign",level.random.nextBoolean()?1:-1);
            double part=Math.min(moved,1-progress);
            degrees+=part*5*count*data.getInt("DeflectionCurveSign");progress+=part;moved-=part;
            if(progress>=1-1e-9){progress=0;data.remove("DeflectionCurveSign");}
        }
        data.putDouble("DeflectionRemainder",progress);
        if(degrees!=0)apply(entity,(float)degrees);
    }
    private static void clearTrack(net.minecraft.nbt.CompoundTag data){
        if(!data.contains("DeflectionSampleTick")&&!data.contains("DeflectionX"))return;
        data.remove("DeflectionSampleTick");data.remove("DeflectionDimension");
        data.remove("DeflectionX");data.remove("DeflectionY");data.remove("DeflectionZ");
        data.remove("DeflectionInside");data.remove("DeflectionCurveSign");data.remove("DeflectionRemainder");
    }
    public static int coverage(ServerLevel level,Entity entity){
        return com.mcmagic.omnira.space.anchor.StabilityField.contains(entity)?0:DeflectionSources.coverage(level,entity);
    }
    public static void turn(Entity entity,float degrees){
        if(com.mcmagic.omnira.space.anchor.StabilityField.contains(entity))return;
        var data=entity.getPersistentData();
        // A floor contact can occur before this tick's post-movement event, outside every cluster.
        // Preserve movement on the contact tick without sampling the approach in empty space.
        if(!data.contains("DeflectionSampleTick")){
            data.putLong("DeflectionSampleTick",entity.level().getGameTime()-1);
            data.putString("DeflectionDimension",entity.level().dimension().location().toString());
            data.putDouble("DeflectionX",entity.xo);data.putDouble("DeflectionY",entity.yo);data.putDouble("DeflectionZ",entity.zo);
        }
        data.putFloat("DeflectionPending",data.getFloat("DeflectionPending")+degrees);
        data.putLong("DeflectionQueuedAt",entity.level().getGameTime());
    }
    public static void advance(Entity entity){
        var data=entity.getPersistentData();float pending=data.getFloat("DeflectionPending");
        if(pending==0)return;
        if(!DeflectionBlock.active(entity,entity.blockPosition())){data.remove("DeflectionPending");return;}
        float step=Math.clamp(pending,-1,1);data.putFloat("DeflectionPending",pending-step);apply(entity,step);
    }
    private static void apply(Entity entity,float degrees){
        if(com.mcmagic.omnira.space.anchor.StabilityField.contains(entity))return;
        entity.setYRot(entity.getYRot()+degrees);
        if(entity instanceof LivingEntity living){living.setYHeadRot(living.getYHeadRot()+degrees);living.yBodyRot+=degrees;}
        if(entity instanceof ServerPlayer player)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new DeflectionPayload(degrees));
        else entity.setDeltaMovement(entity.getDeltaMovement().yRot((float)-Math.toRadians(degrees)));
    }
    private DeflectionMotion(){}
}

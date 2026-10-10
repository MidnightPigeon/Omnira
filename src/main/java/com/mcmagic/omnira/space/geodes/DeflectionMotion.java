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
        if(com.mcmagic.omnira.space.anchor.StabilityField.contains(entity)){
            var data=entity.getPersistentData();data.remove("DeflectionPending");data.remove("DeflectionRemainder");data.remove("DeflectionCurveSign");data.remove("DeflectionInside");data.remove("DeflectionSampleTick");
            if(!data.getBoolean("StabilityDeflectionInside")&&entity instanceof ServerPlayer player)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new DeflectionPayload(0));
            data.putBoolean("StabilityDeflectionInside",true);return;
        }
        entity.getPersistentData().remove("StabilityDeflectionInside");
        var data=entity.getPersistentData();long now=level.getGameTime();
        if(data.contains("DeflectionSampleTick")&&data.getLong("DeflectionSampleTick")==now)return;
        Vec3 current=entity.position();
        boolean consecutive=data.contains("DeflectionSampleTick")&&data.getLong("DeflectionSampleTick")==now-1
                &&data.getString("DeflectionDimension").equals(level.dimension().location().toString());
        double moved=consecutive?current.distanceTo(new Vec3(data.getDouble("DeflectionX"),data.getDouble("DeflectionY"),data.getDouble("DeflectionZ"))):0;
        data.putLong("DeflectionSampleTick",now);data.putString("DeflectionDimension",level.dimension().location().toString());
        data.putDouble("DeflectionX",current.x);data.putDouble("DeflectionY",current.y);data.putDouble("DeflectionZ",current.z);
        if(!consecutive||moved>32){
            data.remove("DeflectionRemainder");data.remove("DeflectionCurveSign");
            if(data.getLong("DeflectionQueuedAt")!=now)data.remove("DeflectionPending");
            data.putBoolean("DeflectionInside",coverage(level,entity)>0);return;
        }
        if(moved<=0)return;
        advance(entity);
        int count=coverage(level,entity);
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
    public static int coverage(ServerLevel level,Entity entity){
        if(com.mcmagic.omnira.space.anchor.StabilityField.contains(entity))return 0;
        var box=entity.getBoundingBox().inflate(5);int count=0;
        for(int cx=net.minecraft.util.Mth.floor(box.minX)>>4;cx<=net.minecraft.util.Mth.floor(box.maxX)>>4;cx++)
            for(int cz=net.minecraft.util.Mth.floor(box.minZ)>>4;cz<=net.minecraft.util.Mth.floor(box.maxZ)>>4;cz++){
                var chunk=level.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
                int minY=Math.max(level.getMinBuildHeight(),net.minecraft.util.Mth.floor(box.minY));
                int maxY=Math.min(level.getMaxBuildHeight()-1,net.minecraft.util.Mth.floor(box.maxY));
                for(int sy=minY>>4;sy<=maxY>>4;sy++){
                    var section=chunk.getSection(chunk.getSectionIndex(sy<<4));
                    if(!section.maybeHas(s->DeflectionContent.radius(s)>0))continue;
                    for(int x=Math.max(cx<<4,net.minecraft.util.Mth.floor(box.minX));x<=Math.min((cx<<4)+15,net.minecraft.util.Mth.floor(box.maxX));x++)
                        for(int z=Math.max(cz<<4,net.minecraft.util.Mth.floor(box.minZ));z<=Math.min((cz<<4)+15,net.minecraft.util.Mth.floor(box.maxZ));z++)
                            for(int y=Math.max(sy<<4,minY);y<=Math.min((sy<<4)+15,maxY);y++){
                                int radius=DeflectionContent.radius(section.getBlockState(x&15,y&15,z&15));if(radius==0)continue;
                                var center=Vec3.atCenterOf(new BlockPos(x,y,z));var bounds=entity.getBoundingBox();
                                if(center.distanceToSqr(Math.clamp(center.x,bounds.minX,bounds.maxX),Math.clamp(center.y,bounds.minY,bounds.maxY),Math.clamp(center.z,bounds.minZ,bounds.maxZ))<=radius*radius)count++;
                            }
                }
            }
        return count;
    }
    public static void turn(Entity entity,float degrees){
        if(com.mcmagic.omnira.space.anchor.StabilityField.contains(entity))return;
        var data=entity.getPersistentData();
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

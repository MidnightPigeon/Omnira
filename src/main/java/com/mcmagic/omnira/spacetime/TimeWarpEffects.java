package com.mcmagic.omnira.spacetime;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.WeakHashMap;

@EventBusSubscriber(modid="omnira")
public final class TimeWarpEffects {
    private static final ResourceLocation SPEED=ResourceLocation.fromNamespaceAndPath("omnira","time_warp_speed");
    private static final ResourceLocation ATTACK=ResourceLocation.fromNamespaceAndPath("omnira","time_warp_attack");
    private static final ResourceLocation MINE=ResourceLocation.fromNamespaceAndPath("omnira","time_warp_mining");
    private static final WeakHashMap<Entity,Exposure> ACTIVE=new WeakHashMap<>();
    private static final class Exposure {
        long entered,last;
        Vec3 unwarped=Vec3.ZERO;
        Exposure(long now){entered=now;last=now;}
    }
    private TimeWarpEffects(){}
    public static void touch(Entity entity,long now){
        var state=ACTIVE.computeIfAbsent(entity,e->new Exposure(now));state.last=now;
    }
    public static double speedFactor(long exposureTicks){
        return 1-.1*Math.min(9,Math.max(0,exposureTicks/8));
    }
    private static void attribute(LivingEntity living,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> type,
                                  ResourceLocation id,double amount){
        var instance=living.getAttribute(type);if(instance==null)return;
        var prior=instance.getModifier(id);
        if(prior!=null && Math.abs(prior.amount()-amount)<.0001)return;
        if(prior!=null)instance.removeModifier(id);
        if(amount!=0)instance.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
    private static void clear(Entity entity,Exposure state){
        if(entity instanceof LivingEntity living){
            attribute(living,Attributes.MOVEMENT_SPEED,SPEED,0);
            attribute(living,Attributes.ATTACK_SPEED,ATTACK,0);
            attribute(living,Attributes.BLOCK_BREAK_SPEED,MINE,0);
        }else if(state.unwarped!=Vec3.ZERO)entity.setDeltaMovement(state.unwarped);
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        var entity=event.getEntity();if(entity.level().isClientSide)return;
        var state=ACTIVE.get(entity);if(state==null)return;
        long now=entity.level().getGameTime();
        if(entity.isRemoved() || now-state.last>1){clear(entity,state);ACTIVE.remove(entity);return;}
        double factor=speedFactor(now-state.entered);
        if(entity instanceof LivingEntity living){
            attribute(living,Attributes.MOVEMENT_SPEED,SPEED,factor-1);
            attribute(living,Attributes.ATTACK_SPEED,ATTACK,factor-1);
            attribute(living,Attributes.BLOCK_BREAK_SPEED,MINE,factor-1);
        }else{
            state.unwarped=entity.getDeltaMovement();entity.setDeltaMovement(state.unwarped.scale(factor));
            entity.hasImpulse=true;
        }
    }
}

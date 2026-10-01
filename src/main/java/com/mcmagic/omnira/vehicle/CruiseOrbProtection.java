package com.mcmagic.omnira.vehicle;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="omnira")
public final class CruiseOrbProtection {
    private CruiseOrbProtection(){}
    public static CruiseOrbEntity cabin(Entity entity){return entity.getVehicle() instanceof CruiseOrbEntity orb && !orb.isRemoved()?orb:null;}
    public static boolean environmental(DamageSource source){
        return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.FREEZE) || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.FLY_INTO_WALL);
    }
    @SubscribeEvent public static void breathe(LivingBreatheEvent event){
        if(cabin(event.getEntity())!=null){event.setCanBreathe(true);event.setConsumeAirAmount(0);event.setRefillAirAmount(event.getEntity().getMaxAirSupply());}
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event){
        if(cabin(event.getEntity())!=null){event.getEntity().clearFire();event.getEntity().setTicksFrozen(0);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void protect(LivingIncomingDamageEvent event){
        var orb=cabin(event.getEntity());if(orb==null)return;
        var source=event.getSource();
        // Administrative/void deaths are deliberately not intercepted.
        if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        boolean attack=source.getEntity()!=null || source.getDirectEntity()!=null || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION);
        if(environmental(source) || attack){
            event.setCanceled(true);
            if(attack)orb.impact();
        }
    }
}

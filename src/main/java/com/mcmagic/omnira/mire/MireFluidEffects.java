package com.mcmagic.omnira.mire;

import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="omnira")
public final class MireFluidEffects {
    public static boolean affected(Entity entity){
        return entity.isInFluidType(MireContent.TYPE.get())&&!entity.isSpectator()&&!immune(entity);
    }
    public static boolean immune(Entity entity){return com.mcmagic.omnira.time.TemporalCreatureTags.timeImmune(entity)
            ||entity.getType().is(com.mcmagic.omnira.time.TemporalCreatureTags.DREAM);}
    public static Vec3 movement(Entity entity,Vec3 delta){return affected(entity)?delta.scale(.5):delta;}
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        var entity=event.getEntity();
        if(!entity.level().isClientSide&&entity instanceof LivingEntity living&&affected(entity)&&entity.tickCount%40==0)
            living.hurt(living.damageSources().wither(),1);
    }
    private MireFluidEffects(){}
}

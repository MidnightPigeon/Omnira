package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.StilledTime;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class StilledClientLevelMixin {
    @org.spongepowered.asm.mixin.Shadow private void tickPassenger(Entity mount,Entity passenger){throw new AssertionError();}
    private boolean omnira$paused(Entity entity){
        if(!StilledTime.stopped(entity)&&!com.mcmagic.omnira.archaeology.BrushStasis.held(entity))return false;
        for(var passenger:entity.getPassengers())tickPassenger(entity,passenger);
        return true;
    }
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void omnira$entity(Entity entity,CallbackInfo ci){if(omnira$paused(entity))ci.cancel();}
    @Inject(method="tickPassenger",at=@At("HEAD"),cancellable=true)
    private void omnira$passenger(Entity vehicle,Entity entity,CallbackInfo ci){if(omnira$paused(entity))ci.cancel();}
}

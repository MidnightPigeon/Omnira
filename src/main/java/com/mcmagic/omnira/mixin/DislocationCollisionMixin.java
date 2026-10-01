package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.TemporalDislocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class DislocationCollisionMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void omnira$noBodyPush(Entity other,CallbackInfo callback){
        if(TemporalDislocation.phased((Entity)(Object)this)||TemporalDislocation.phased(other))callback.cancel();
    }
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void omnira$noBodyCollision(Entity other,CallbackInfoReturnable<Boolean> result){
        if(TemporalDislocation.phased((Entity)(Object)this)||TemporalDislocation.phased(other))result.setReturnValue(false);
    }
}

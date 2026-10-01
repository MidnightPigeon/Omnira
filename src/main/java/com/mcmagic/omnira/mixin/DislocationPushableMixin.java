package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.TemporalDislocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DislocationPushableMixin {
    @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
    private void omnira$noSolidBody(CallbackInfoReturnable<Boolean> result){
        if(TemporalDislocation.phased((LivingEntity)(Object)this))result.setReturnValue(false);
    }
}

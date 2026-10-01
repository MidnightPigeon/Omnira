package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.FleetingTime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class FleetingTravelMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void omnira$oneTravel(Vec3 input,CallbackInfo ci){if(FleetingTime.bonus((LivingEntity)(Object)this))ci.cancel();}
}

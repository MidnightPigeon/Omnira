package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.abyss.AbyssGravity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class AbyssLivingMixin {
    @Inject(method="jumpFromGround",at=@At("TAIL"))
    private void omnira$jump(CallbackInfo ci){var e=(LivingEntity)(Object)this;if(AbyssGravity.inverted(e)&&e.getDeltaMovement().y>0)e.setDeltaMovement(e.getDeltaMovement().multiply(1,-1,1));}
}

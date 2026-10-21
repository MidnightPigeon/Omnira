package com.mcmagic.omnira.compat.create.mixin;

import com.mcmagic.omnira.compat.create.PulseOrbKinetics;
import com.simibubi.create.content.kinetics.KineticNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A rate conversion is a real load, but not an extra world block or saved member. */
@Mixin(value=KineticNetwork.class,remap=false)
public abstract class PulseStressLedgerMixin {
    @Inject(method="calculateStress",at=@At("RETURN"),cancellable=true)
    private void omnira$relayStress(CallbackInfoReturnable<Float> result){
        result.setReturnValue(result.getReturnValue()+PulseOrbKinetics.importedStress((KineticNetwork)(Object)this));
    }
}

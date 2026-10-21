package com.mcmagic.omnira.compat.sable.mixin;

import com.mcmagic.omnira.compat.SableZeroMass;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ServerSubLevel.class,remap=false)
abstract class ZeroMassSableMixin {
    @Unique private SableZeroMass omnira$zeroMass;
    @Inject(method="getMassTracker",at=@At("RETURN"),cancellable=true)
    private void omnira$finiteZeroBody(CallbackInfoReturnable<MassData> result){
        var raw=result.getReturnValue();
        if(raw!=null&&raw.getMass()==0){
            if(omnira$zeroMass==null)omnira$zeroMass=new SableZeroMass();
            result.setReturnValue(omnira$zeroMass.refresh((ServerSubLevel)(Object)this));
        }
    }
}

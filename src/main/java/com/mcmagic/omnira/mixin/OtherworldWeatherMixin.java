package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.world.dimension.OtherworldWeather;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Level.class)
abstract class OtherworldWeatherMixin {
    @Inject(method={"getRainLevel","getThunderLevel"},at=@At("HEAD"),cancellable=true)
    private void omnira$noWeatherStrength(float partial,CallbackInfoReturnable<Float> ci){
        if(OtherworldWeather.disabled((Level)(Object)this))ci.setReturnValue(0F);
    }
    @ModifyVariable(method={"setRainLevel","setThunderLevel"},at=@At("HEAD"),argsOnly=true)
    private float omnira$ignoreWeatherStrength(float strength){
        return OtherworldWeather.disabled((Level)(Object)this)?0:strength;
    }
    @Inject(method="prepareWeather",at=@At("HEAD"),cancellable=true)
    private void omnira$clearInitialWeather(CallbackInfo ci){
        var level=(Level)(Object)this;
        if(OtherworldWeather.disabled(level)){
            level.setRainLevel(0);level.setThunderLevel(0);ci.cancel();
        }
    }
}

package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.world.dimension.OtherworldWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class OtherworldServerWeatherMixin {
    @Inject(method="advanceWeatherCycle",at=@At("HEAD"),cancellable=true)
    private void omnira$noWeatherCycle(CallbackInfo ci){
        var level=(ServerLevel)(Object)this;
        if(OtherworldWeather.disabled(level)){
            // Never clear DerivedLevelData: its weather belongs to the overworld.
            level.setRainLevel(0);level.setThunderLevel(0);ci.cancel();
        }
    }
    @Inject(method="tickPrecipitation",at=@At("HEAD"),cancellable=true)
    private void omnira$noSnowOrFreezing(BlockPos pos,CallbackInfo ci){
        if(OtherworldWeather.disabled((ServerLevel)(Object)this))ci.cancel();
    }
    @Inject(method="setWeatherParameters",at=@At("HEAD"),cancellable=true)
    private void omnira$ignoreWeatherRequest(int clearTime,int weatherTime,boolean rain,boolean thunder,CallbackInfo ci){
        if(OtherworldWeather.disabled((ServerLevel)(Object)this))ci.cancel();
    }
}

package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.spacetime.SpacetimeSky;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class SpacetimeSkyLightMixin {
    @Shadow private int skyDarken;

    @Inject(method = "updateSkyBrightness", at = @At("HEAD"), cancellable = true)
    private void omnira$constantSkyLight(CallbackInfo ci) {
        if (SpacetimeSky.applies((Level)(Object)this)) {
            skyDarken = SpacetimeSky.DARKEN;
            ci.cancel();
        }
    }
}

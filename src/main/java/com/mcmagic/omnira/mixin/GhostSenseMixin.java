package com.mcmagic.omnira.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class GhostSenseMixin {
    @Inject(method="shouldEntityAppearGlowing",at=@At("HEAD"),cancellable=true)
    private void omnira$ghostSense(Entity entity,CallbackInfoReturnable<Boolean> result) {
        if(com.mcmagic.omnira.client.GhostSense.highlighted(entity))result.setReturnValue(true);
    }
}

package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class AffinityVerticalFlightMixin {
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float omnira$verticalSpeed(Abilities abilities,Operation<Float> original) {
        return original.call(abilities)*(((LocalPlayer)(Object)this).getData(ModAttachments.DREAM_AFFINITY)?.5F:1);
    }
}

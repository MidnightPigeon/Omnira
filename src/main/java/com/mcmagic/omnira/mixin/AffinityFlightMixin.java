package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class AffinityFlightMixin {
    @ModifyExpressionValue(method="getFlyingSpeed",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;isSprinting()Z"))
    private boolean omnira$noDreamFlightSprintBoost(boolean sprinting) {
        var player=(Player)(Object)this;
        return sprinting && !(player.getAbilities().flying && !player.isPassenger() && player.getData(ModAttachments.DREAM_AFFINITY));
    }
    @ModifyReturnValue(method="getFlyingSpeed",at=@At("RETURN"))
    private float omnira$flightSpeed(float original) {
        var player=(Player)(Object)this;
        return player.getAbilities().flying && !player.isPassenger() && player.getData(ModAttachments.DREAM_AFFINITY)?original*.5F:original;
    }
}

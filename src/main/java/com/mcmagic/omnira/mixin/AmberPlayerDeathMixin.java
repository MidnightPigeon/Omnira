package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.spacetime.TemporalAmber;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class AmberPlayerDeathMixin {
    // Reached only after the cancellable death hook has accepted the death.
    @Inject(method="die",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void omnira$capture(DamageSource source,CallbackInfo ci){TemporalAmber.beginDeath((ServerPlayer)(Object)this);}
    @Inject(method="die",at=@At("RETURN"))
    private void omnira$finish(DamageSource source,CallbackInfo ci){TemporalAmber.finishDeath((ServerPlayer)(Object)this);}
}

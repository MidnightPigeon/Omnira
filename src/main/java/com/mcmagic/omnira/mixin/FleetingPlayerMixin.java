package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.time.FleetingTime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
abstract class FleetingPlayerMixin {
    @WrapOperation(method="doTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;tick()V"))
    private void omnira$player(ServerPlayer player,Operation<Void> original){original.call(player);FleetingTime.extra(player,()->original.call(player),true);}
}

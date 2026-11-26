package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.storage.RemoteContainerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mekanism.common.network.to_server.frequency.PacketSetTileFrequency", remap = false)
abstract class RemoteMekanismFrequencyMixin {
    @Shadow public abstract BlockPos pos();

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void omnira$validate(IPayloadContext context, CallbackInfo callback, @Share("boundLevel") LocalRef<Level> bound) {
        var destination = RemoteContainerAccess.machineSettingLevel(context.player(), pos());
        if (destination == null) callback.cancel();
        else bound.set(destination);
    }

    // Mekanism uses this level for both its lookup and its native security check.
    @ModifyExpressionValue(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;level()Lnet/minecraft/world/level/Level;"))
    private Level omnira$level(Level level, @Share("boundLevel") LocalRef<Level> bound) {
        return bound.get() == null ? level : bound.get();
    }
}

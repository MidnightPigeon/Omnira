package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.storage.RemoteContainerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mekanism.common.network.PacketUtils", remap = false)
abstract class RemoteMekanismSettingsMixin {
    @Inject(method = "blockEntity(Lnet/neoforged/neoforge/network/handling/IPayloadContext;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true)
    private static void omnira$machine(IPayloadContext context, BlockPos pos, CallbackInfoReturnable<BlockEntity> result) {
        var player = context.player();
        var destination = RemoteContainerAccess.machineSettingLevel(player, pos);
        if (destination == null) result.setReturnValue(null);
        else if (destination != player.level()) result.setReturnValue(destination.getBlockEntity(pos));
    }

    @Inject(method = "blockEntity(Lnet/neoforged/neoforge/network/handling/IPayloadContext;J)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true)
    private static void omnira$machine(IPayloadContext context, long pos, CallbackInfoReturnable<BlockEntity> result) {
        omnira$machine(context, BlockPos.of(pos), result);
    }
}

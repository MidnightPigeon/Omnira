package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.client.RemoteContainerView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Mekanism lookup otherwise rejects tiles outside the client's loaded chunks. */
@Pseudo
@Mixin(targets = "mekanism.common.util.WorldUtils", remap = false)
abstract class RemoteMekanismMenuLookupMixin {
    @Inject(method = "getTileEntity(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void omnira$remoteTile(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockEntity> result) {
        var tile = RemoteContainerView.entity(pos);
        if (tile != null && level == Minecraft.getInstance().level) result.setReturnValue(tile);
    }
}

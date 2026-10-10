package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.client.RemoteContainerView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class RemoteMenuLevelMixin {
    @Inject(method="getBlockEntity",at=@At("HEAD"),cancellable=true)
    private void omnira$entity(BlockPos pos,CallbackInfoReturnable<BlockEntity> ci){if(((Level)(Object)this).isClientSide){var e=RemoteContainerView.entity(pos);if(e!=null)ci.setReturnValue(e);}}
    @Inject(method="getBlockState",at=@At("HEAD"),cancellable=true)
    private void omnira$state(BlockPos pos,CallbackInfoReturnable<BlockState> ci){if(((Level)(Object)this).isClientSide){var s=RemoteContainerView.state(pos);if(s!=null)ci.setReturnValue(s);}}
}

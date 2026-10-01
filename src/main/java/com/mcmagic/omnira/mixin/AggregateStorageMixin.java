package com.mcmagic.omnira.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
abstract class AggregateStorageMixin {
    @Inject(method="setChanged()V",at=@At("TAIL"))
    private void omnira$changedStorage(CallbackInfo ci){
        com.mcmagic.omnira.spacetime.AggregateStorage.changed((BlockEntity)(Object)this);
    }
}

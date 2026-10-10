package com.mcmagic.omnira.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Xaero skips MapColor.NONE entirely; vanilla maps benefit from the same material palette. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class OmniraMapColorMixin {
    @Inject(method="getMapColor",at=@At("RETURN"),cancellable=true)
    private void omnira$palette(BlockGetter level,BlockPos pos,CallbackInfoReturnable<MapColor> ci){
        if(ci.getReturnValue()==MapColor.NONE)
            ci.setReturnValue(com.mcmagic.omnira.world.OmniraMapColors.fallback((BlockState)(Object)this));
    }
}

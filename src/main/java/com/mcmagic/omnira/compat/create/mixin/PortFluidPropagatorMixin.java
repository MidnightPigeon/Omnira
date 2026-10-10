package com.mcmagic.omnira.compat.create.mixin;

import com.mcmagic.omnira.compat.PortFluidUpdates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="com.simibubi.create.content.fluids.FluidPropagator",remap=false)
abstract class PortFluidPropagatorMixin {
    @Inject(method="propagateChangedPipe",at=@At("HEAD"),cancellable=true)
    private static void omnira$remotePipes(LevelAccessor level,BlockPos pos,BlockState state,CallbackInfo ci){
        if(PortFluidUpdates.propagate(level,pos,state))ci.cancel();
    }
}

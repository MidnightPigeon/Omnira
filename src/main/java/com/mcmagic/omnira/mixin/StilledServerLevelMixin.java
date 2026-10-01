package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.StilledTime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class StilledServerLevelMixin {
    @org.spongepowered.asm.mixin.Shadow private void tickPassenger(Entity mount,Entity passenger){throw new AssertionError();}
    private boolean omnira$paused(Entity entity){
        if(!StilledTime.stopped(entity)&&!com.mcmagic.omnira.archaeology.BrushStasis.held(entity))return false;
        for(var passenger:entity.getPassengers())tickPassenger(entity,passenger);
        return true;
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="runBlockEvents",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;shouldTickBlocksAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean omnira$deferBlockEvents(ServerLevel level,BlockPos pos,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        return !StilledTime.stopped(level,pos)&&original.call(level,pos);
    }
    @Inject(method="tickBlock",at=@At("HEAD"),cancellable=true)
    private void omnira$block(BlockPos pos,Block block,CallbackInfo ci){
        var level=(ServerLevel)(Object)this;
        if(StilledTime.stopped(level,pos)){level.scheduleTick(pos,block,20);ci.cancel();}
    }
    @Inject(method="tickFluid",at=@At("HEAD"),cancellable=true)
    private void omnira$fluid(BlockPos pos,Fluid fluid,CallbackInfo ci){
        var level=(ServerLevel)(Object)this;
        if(StilledTime.stopped(level,pos)){level.scheduleTick(pos,fluid,20);ci.cancel();}
    }
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void omnira$entity(Entity entity,CallbackInfo ci){if(omnira$paused(entity))ci.cancel();}
    @Inject(method="tickPassenger",at=@At("HEAD"),cancellable=true)
    private void omnira$passenger(Entity vehicle,Entity entity,CallbackInfo ci){if(omnira$paused(entity))ci.cancel();}
}

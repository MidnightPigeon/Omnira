package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.time.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class FleetingServerLevelMixin {
    @WrapOperation(method="tickChunk",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void omnira$random(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,Operation<Void> original){
        if(StilledTime.stopped(level,pos))return;
        original.call(state,level,pos,random);
        if(!(state.getBlock() instanceof TimeSaplingBlock)&&!(state.getBlock() instanceof TimeLeavesBlock)&&FleetingTime.accelerated(level,pos)){
            var current=level.getBlockState(pos);if(current.isRandomlyTicking())original.call(current,level,pos,random);
        }
    }
    @WrapOperation(method="tickChunk",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void omnira$fluid(net.minecraft.world.level.material.FluidState state,net.minecraft.world.level.Level level,BlockPos pos,RandomSource random,Operation<Void> original){
        if(StilledTime.stopped(level,pos))return;
        original.call(state,level,pos,random);
        if(FleetingTime.accelerated(level,pos)){var current=level.getFluidState(pos);if(current.isRandomlyTicking())original.call(current,level,pos,random);}
    }
    @Inject(method="tickNonPassenger",at=@At("RETURN"))
    private void omnira$entity(Entity entity,CallbackInfo ci){
        FleetingTime.extra(entity,()->{
            if(!net.neoforged.neoforge.event.EventHooks.fireEntityTickPre(entity).isCanceled()){
                entity.tick();net.neoforged.neoforge.event.EventHooks.fireEntityTickPost(entity);
            }
        },!(entity instanceof ServerPlayer));
    }
    @Inject(method="tickPassenger",at=@At("RETURN"))
    private void omnira$passenger(Entity vehicle,Entity entity,CallbackInfo ci){
        if(entity.getVehicle()==vehicle)FleetingTime.extra(entity,entity::rideTick,!(entity instanceof ServerPlayer));
    }
}

package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.ticks.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelAccessor.class)
interface FleetingScheduledTickMixin {
    @Inject(method="createTick(Lnet/minecraft/core/BlockPos;Ljava/lang/Object;ILnet/minecraft/world/ticks/TickPriority;)Lnet/minecraft/world/ticks/ScheduledTick;",at=@At("RETURN"),cancellable=true)
    private <T> void omnira$scheduled(BlockPos pos,T type,int delay,TickPriority priority,CallbackInfoReturnable<ScheduledTick<T>> cir){omnira$halve(pos,type,delay,cir);}
    @Inject(method="createTick(Lnet/minecraft/core/BlockPos;Ljava/lang/Object;I)Lnet/minecraft/world/ticks/ScheduledTick;",at=@At("RETURN"),cancellable=true)
    private <T> void omnira$scheduledDefault(BlockPos pos,T type,int delay,CallbackInfoReturnable<ScheduledTick<T>> cir){omnira$halve(pos,type,delay,cir);}
    private <T> void omnira$halve(BlockPos pos,T type,int delay,CallbackInfoReturnable<ScheduledTick<T>> cir){
        if(this instanceof Level level&&!level.isClientSide&&delay>1&&!(type instanceof TimeSaplingBlock)&&FleetingTime.accelerated(level,pos)){
            var tick=cir.getReturnValue();cir.setReturnValue(new ScheduledTick<>(tick.type(),tick.pos(),tick.triggerTick()-delay/2,tick.priority(),tick.subTickOrder()));
        }
    }
}

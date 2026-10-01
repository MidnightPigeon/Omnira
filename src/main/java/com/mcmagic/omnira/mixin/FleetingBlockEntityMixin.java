package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.time.FleetingTime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
abstract class FleetingBlockEntityMixin {
    @WrapOperation(method="tickBlockEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void omnira$machine(TickingBlockEntity ticker,Operation<Void> original){
        if(com.mcmagic.omnira.time.StilledTime.stopped((Level)(Object)this,ticker.getPos()))return;
        original.call(ticker);var level=(Level)(Object)this;
        if(!level.isClientSide&&!ticker.isRemoved()&&FleetingTime.accelerated(level,ticker.getPos()))original.call(ticker);
    }
}

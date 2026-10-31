package com.mcmagic.omnira.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface StilledItemAccessor {
    @Accessor("pickupDelay") int omnira$getPickupDelay();
    @Accessor("pickupDelay") void omnira$setPickupDelay(int delay);
}

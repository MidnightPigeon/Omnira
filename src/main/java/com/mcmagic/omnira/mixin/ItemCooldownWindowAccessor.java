package com.mcmagic.omnira.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets="net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface ItemCooldownWindowAccessor {
    @Accessor("startTime") int omnira$start();
    @Accessor("endTime") int omnira$end();
    @Mutable @Accessor("startTime") void omnira$start(int tick);
    @Mutable @Accessor("endTime") void omnira$end(int tick);
}

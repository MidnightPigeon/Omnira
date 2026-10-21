package com.mcmagic.omnira.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingStandBlockEntity.class)
public interface WorldRotationBrewingAccessor {
    @Accessor("brewTime") int omnira$brewTime();
    @Accessor("brewTime") void omnira$brewTime(int time);
}

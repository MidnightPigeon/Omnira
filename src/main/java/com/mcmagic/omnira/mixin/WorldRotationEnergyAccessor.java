package com.mcmagic.omnira.mixin;

import net.neoforged.neoforge.energy.EnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=EnergyStorage.class,remap=false)
public interface WorldRotationEnergyAccessor {
    @Accessor("energy") void omnira$energy(int value);
}

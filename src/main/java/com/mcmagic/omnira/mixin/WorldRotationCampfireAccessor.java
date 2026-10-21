package com.mcmagic.omnira.mixin;

import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CampfireBlockEntity.class)
public interface WorldRotationCampfireAccessor {
    @Accessor("cookingProgress") int[] omnira$progress();
    @Accessor("cookingTime") int[] omnira$total();
}

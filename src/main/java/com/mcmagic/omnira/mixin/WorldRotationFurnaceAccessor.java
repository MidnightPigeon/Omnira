package com.mcmagic.omnira.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface WorldRotationFurnaceAccessor {
    @Accessor("cookingProgress") int omnira$progress();
    @Accessor("cookingProgress") void omnira$progress(int value);
    @Accessor("cookingTotalTime") int omnira$total();
}

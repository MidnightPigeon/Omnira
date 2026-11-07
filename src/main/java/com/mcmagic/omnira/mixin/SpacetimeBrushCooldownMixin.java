package com.mcmagic.omnira.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BrushableBlockEntity.class)
abstract class SpacetimeBrushCooldownMixin {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="brush",at=@At(value="CONSTANT",args="longValue=10"))
    private long omnira$fasterBrush(long original,long now,Player player,Direction direction){
        return player.getUseItem().getItem() instanceof com.mcmagic.omnira.archaeology.SpacetimeBrushItem?5L:original;
    }
}

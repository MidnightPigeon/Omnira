package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
abstract class BottleHopperMixin {
    @Inject(method="canPlaceItemInContainer",at=@At("HEAD"),cancellable=true)
    private static void omnira$rejectBottle(Container container,ItemStack stack,int slot,Direction side,CallbackInfoReturnable<Boolean> cir) {
        if(!com.mcmagic.omnira.item.PortableStorageRules.mayPlace(container,stack)) cir.setReturnValue(false);
    }
}

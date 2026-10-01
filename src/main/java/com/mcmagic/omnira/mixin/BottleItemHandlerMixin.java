package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ItemStackHandler.class,remap=false)
abstract class BottleItemHandlerMixin {
    @Inject(method="insertItem",at=@At("HEAD"),cancellable=true)
    private void omnira$rejectBottle(int slot,ItemStack stack,boolean simulate,CallbackInfoReturnable<ItemStack> cir) {
        if(PocketBottleItem.restricted(stack)) cir.setReturnValue(stack);
    }
    @Inject(method="isItemValid",at=@At("HEAD"),cancellable=true)
    private void omnira$invalidBottle(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(PocketBottleItem.restricted(stack)) cir.setReturnValue(false);
    }
}

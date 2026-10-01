package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
interface BottleContainerMixin {
    @Inject(method="canPlaceItem",at=@At("HEAD"),cancellable=true)
    private void omnira$rejectBottle(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(!com.mcmagic.omnira.item.PortableStorageRules.mayPlace((Container)(Object)this,stack)) cir.setReturnValue(false);
    }
}

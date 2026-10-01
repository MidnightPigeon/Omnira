package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
abstract class BottleSlotMixin {
    @Shadow @Final public Container container;
    @Inject(method="mayPlace",at=@At("HEAD"),cancellable=true)
    private void omnira$rejectBottle(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(!com.mcmagic.omnira.item.PortableStorageRules.mayPlace(container,stack)) cir.setReturnValue(false);
    }
}

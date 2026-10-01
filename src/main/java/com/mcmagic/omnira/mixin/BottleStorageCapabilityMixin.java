package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.bottle.BottleStorageHandler;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BlockCapability.class,ItemCapability.class},remap=false)
abstract class BottleStorageCapabilityMixin {
    @Inject(method="getCapability",at=@At("RETURN"),cancellable=true)
    private void omnira$protectStorage(CallbackInfoReturnable<Object> cir) {
        if(((Object)this==Capabilities.ItemHandler.BLOCK || (Object)this==Capabilities.ItemHandler.ITEM)
                && cir.getReturnValue() instanceof IItemHandler handler) cir.setReturnValue(BottleStorageHandler.wrap(handler));
    }
}

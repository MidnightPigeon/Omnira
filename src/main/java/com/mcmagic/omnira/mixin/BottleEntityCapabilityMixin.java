package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.bottle.BottleStorageHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=EntityCapability.class,remap=false)
abstract class BottleEntityCapabilityMixin {
    @Inject(method="getCapability",at=@At("RETURN"),cancellable=true)
    private void omnira$protectStorage(Entity entity,Object context,CallbackInfoReturnable<Object> cir) {
        if(!(entity instanceof Player) && ((Object)this==Capabilities.ItemHandler.ENTITY
                || (Object)this==Capabilities.ItemHandler.ENTITY_AUTOMATION)
                && cir.getReturnValue() instanceof IItemHandler handler) cir.setReturnValue(BottleStorageHandler.wrap(handler));
    }
}

package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.shop.MarisaMerchant;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MarisaMerchantMenuMixin {
    @Shadow @Final private Merchant trader;
    // Vanilla shift-trading casts merchants to Entity; this merchant is a block entity.
    @Inject(method="playTradeSound",at=@At("HEAD"),cancellable=true)
    private void omnira$blockMerchantSound(CallbackInfo ci){if(trader instanceof MarisaMerchant)ci.cancel();}
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at=@At("RETURN"))
    private void omnira$bindSession(CallbackInfo ci){
        if(trader instanceof MarisaMerchant merchant)merchant.bindMenu((MerchantMenu)(Object)this);
    }
}

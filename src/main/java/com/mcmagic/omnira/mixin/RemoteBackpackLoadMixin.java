package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.compat.SophisticatedBackpackMenus.PendingLoad;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** A node may see a freshly deserialized backpack before its chunk runs onLoad. */
@Pseudo
@Mixin(targets="net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity",remap=false)
abstract class RemoteBackpackLoadMixin implements PendingLoad {
    @Shadow private ItemStack pendingLoadedBackpack;
    @Shadow public abstract void setBackpack(ItemStack stack);

    @Shadow private net.neoforged.neoforge.fluids.capability.IFluidHandler externalFluidHandler;
    @Shadow private void invalidateHandlers() { throw new AssertionError(); }

    // The native capability getter caches EmptyFluidHandler even when the saved stack is still pending.
    @org.spongepowered.asm.mixin.injection.Inject(method="setBackpack",at=@org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void omnira$refreshPendingTank(ItemStack stack, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (externalFluidHandler == net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler.INSTANCE)
            invalidateHandlers();
    }

    @Override
    public void omnira$initializeStoredBackpack() {
        if (pendingLoadedBackpack != null) setBackpack(pendingLoadedBackpack);
    }
}

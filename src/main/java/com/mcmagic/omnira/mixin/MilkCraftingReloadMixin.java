package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.recipe.MilkCraftingIngredients;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableServerResources.class)
public abstract class MilkCraftingReloadMixin {
    @Inject(method = "updateRegistryTags", at = @At("TAIL"))
    private void omnira$expandWorkbenchMilk(CallbackInfo ci) {
        MilkCraftingIngredients.expand(((ReloadableServerResources) (Object) this).getRecipeManager());
    }
}

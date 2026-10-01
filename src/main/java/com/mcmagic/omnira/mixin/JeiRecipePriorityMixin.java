package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.compat.jei.RecipePriorities;
import mezz.jei.api.recipe.IFocusGroup;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="mezz.jei.gui.recipes.lookups.FocusedRecipes",remap=false)
public abstract class JeiRecipePriorityMixin<T> {
    @Shadow @Final private IFocusGroup focuses;
    @Shadow private List<T> recipes;
    @Unique private boolean omnira$sorted;
    @Inject(method="getRecipes",at=@At("RETURN"),cancellable=true)
    private void omnira$guaranteedFirst(CallbackInfoReturnable<List<T>> cir) {
        if(!omnira$sorted) {recipes=RecipePriorities.recipes(cir.getReturnValue(),focuses);omnira$sorted=true;}
        cir.setReturnValue(recipes);
    }
}

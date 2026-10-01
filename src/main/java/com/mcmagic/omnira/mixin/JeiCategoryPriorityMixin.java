package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.compat.jei.RecipePriorities;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="mezz.jei.gui.recipes.lookups.IngredientLookupState",remap=false)
public abstract class JeiCategoryPriorityMixin {
    @Shadow @Final @Mutable private List<IRecipeCategory<?>> recipeCategories;
    @Inject(method="<init>",at=@At("RETURN"))
    private void omnira$processingBeforeLoot(IRecipeManager manager,IFocusGroup focus,List<IRecipeCategory<?>> categories,CallbackInfo ci) {
        recipeCategories=RecipePriorities.categories(categories,manager,focus);
    }
}

package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mcmagic.omnira.compat.jei.RecipePriorities;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.common.config.RecipeSorterStage;
import mezz.jei.gui.recipes.lookups.IFocusedRecipes;
import java.util.Set;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="mezz.jei.gui.recipes.layouts.LazyRecipeLayoutList",remap=false)
public abstract class JeiChanceOrderMixin {
    // JEI's lazy craftable/bookmark promotion would otherwise undo deterministic-first order.
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static Set<RecipeSorterStage> omnira$keepProbabilityOrder(Set<RecipeSorterStage> stages,
            @Local(argsOnly=true) IFocusedRecipes<?> recipes,@Local(argsOnly=true) IFocusGroup focus) {
        return RecipePriorities.mixedChance(recipes.getRecipes(),focus)?Set.of():stages;
    }
}

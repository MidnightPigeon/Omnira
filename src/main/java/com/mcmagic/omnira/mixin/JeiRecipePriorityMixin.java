package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mcmagic.omnira.compat.jei.RecipePriorities;
import mezz.jei.api.recipe.IFocusGroup;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="mezz.jei.gui.recipes.lookups.FocusedRecipes",remap=false)
public abstract class JeiRecipePriorityMixin<T> {
    @Shadow @Final private IFocusGroup focuses;
    @Unique private List<T> omnira$input;
    @Unique private List<T> omnira$output;
    @ModifyReturnValue(method="getRecipes",at=@At("RETURN"))
    private List<T> omnira$guaranteedFirst(List<T> current) {
        // Snapshot contents, not just identity: other integrations may edit the same list.
        if(omnira$input==null || !omnira$input.equals(current)) {
            omnira$input=List.copyOf(current);
            omnira$output=RecipePriorities.recipes(omnira$input,focuses);
        }
        return omnira$output;
    }
}

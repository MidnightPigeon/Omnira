package com.mcmagic.omnira.compat.jei;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Isolated so JEI does not require Create. */
final class CreateRecipePriorities {
    static int rank(Object recipe,List<ItemStack> targets) {
        if(recipe instanceof ProcessingRecipe<?,?> processing) {
            for(var output:processing.getRollableResults())
                if(output.getChance()>=1 && RecipePriorities.matches(output.getStack(),targets))return 0;
            return 1;
        }
        if(recipe instanceof SequencedAssemblyRecipe assembly)return assembly.getOutputChance()>=1?0:1;
        return 0;
    }
}

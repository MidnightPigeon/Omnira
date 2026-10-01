package com.mcmagic.omnira.compat.jei;

import com.simibubi.create.content.processing.recipe.*;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import net.minecraft.world.item.*;
import java.util.List;

final class CreatePrioritySmoke {
    private static final class Params extends ProcessingRecipeParams {
        Params(){results.add(new ProcessingOutput(new ItemStack(Items.IRON_INGOT),1));results.add(new ProcessingOutput(new ItemStack(Items.DIAMOND),.1F));}
    }
    static void run() {
        var recipe=new CrushingRecipe(new Params());
        if(CreateRecipePriorities.rank(recipe,List.of(new ItemStack(Items.IRON_INGOT)))!=0
                || CreateRecipePriorities.rank(recipe,List.of(new ItemStack(Items.DIAMOND)))!=1)
            throw new IllegalStateException("Create byproduct probability must be evaluated for the queried item");
    }
}

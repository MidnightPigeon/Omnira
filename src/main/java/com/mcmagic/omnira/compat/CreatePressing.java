package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.recipe.AssemblyWork;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class CreatePressing {
    public static AssemblyWork find(Level level,Container inventory) {
        if(inventory.getItem(6).isEmpty()) return null;
        for(int i=0;i<6;i++) if(!inventory.getItem(i).isEmpty()) return null;
        RecipeType<PressingRecipe> type=AllRecipeTypes.PRESSING.getType();
        var match=SequencedAssemblyRecipe.getRecipe(level,inventory.getItem(6),type,PressingRecipe.class);
        if(match.isEmpty()) match=level.getRecipeManager().getRecipeFor(type,new SingleRecipeInput(inventory.getItem(6)),level);
        if(match.isEmpty()) return null;
        var holder=match.get();
        return new AssemblyWork("pressing/"+holder.id(),()->holder.value().rollResults(level.random));
    }
}

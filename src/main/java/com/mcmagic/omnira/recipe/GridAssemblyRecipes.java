package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.item.CrystalGridItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class GridAssemblyRecipes {
    private GridAssemblyRecipes() {}
    public static ItemStack upgrade(AssemblyRecipe.Input input,ItemStack result) {
        var source=input.getItem(6);
        if(source.getCount()!=1 || !(source.getItem() instanceof CrystalGridItem from)
                || !(result.getItem() instanceof CrystalGridItem to) || to.capacity<=from.capacity) return ItemStack.EMPTY;
        var contents=source.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        if(contents.getSlots()>from.capacity) return ItemStack.EMPTY;
        // Preserve slot indices, gaps, authored spells and cursor. The larger grid supplies trailing empty slots.
        return source.transmuteCopy(result.getItem(),1);
    }
}

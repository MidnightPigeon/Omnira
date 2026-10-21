package com.mcmagic.omnira.recipe;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** Preserve the authored grid when a crafting recipe adds runtime safety checks. */
public interface WrappedShapedRecipe extends CraftingRecipe {
    ShapedRecipe shaped();
}

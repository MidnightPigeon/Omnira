package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.recipe.WrappedShapedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI's default crafting extension only recognizes vanilla ShapedRecipe instances. */
public final class WrappedShapedJei implements ICraftingCategoryExtension<WrappedShapedRecipe> {
    @Override public void setRecipe(RecipeHolder<WrappedShapedRecipe> holder,IRecipeLayoutBuilder builder,ICraftingGridHelper grid,IFocusGroup focus){
        var recipe=holder.value().shaped();
        grid.createAndSetIngredients(builder,recipe.getIngredients(),recipe.getWidth(),recipe.getHeight());
        grid.createAndSetOutputs(builder,java.util.List.of(recipe.getResultItem(null)));
    }
    @Override public int getWidth(RecipeHolder<WrappedShapedRecipe> recipe){return recipe.value().shaped().getWidth();}
    @Override public int getHeight(RecipeHolder<WrappedShapedRecipe> recipe){return recipe.value().shaped().getHeight();}
}

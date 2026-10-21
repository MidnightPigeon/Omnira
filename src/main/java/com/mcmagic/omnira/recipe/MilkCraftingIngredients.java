package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;

/** Expand workbench ingredients after registry tags are bound, before recipe synchronization. */
public final class MilkCraftingIngredients {
    private MilkCraftingIngredients() {}

    public static void expand(RecipeManager manager) {
        for (var holder : manager.getRecipes()) {
            if (holder.id().toString().equals("omnira:milk_suspension")) continue;
            var recipe = holder.value();
            if (recipe.getType() != RecipeType.CRAFTING) continue;
            if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;
            expand(recipe.getIngredients());
        }
    }

    public static void expand(java.util.List<Ingredient> ingredients) {
        var milk = new ItemStack(Items.MILK_BUCKET);
        var suspension = new ItemStack(ModItems.MILK_SUSPENSION.get());
        for (int i = 0; i < ingredients.size(); i++) {
            var ingredient = ingredients.get(i);
            // Component-sensitive custom ingredients are not interchangeable with plain milk.
            if (ingredient.isSimple() && ingredient.test(milk) && !ingredient.test(suspension)) {
                ingredients.set(i, CompoundIngredient.of(ingredient, Ingredient.of(suspension)));
            }
        }
    }
}

package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.recipe.AnalysisRecipe;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.*;

/** Stable partitions, evaluated once per lookup. Never builds recipe layouts to sort them. */
public final class RecipePriorities {
    private RecipePriorities(){}
    public static boolean loot(net.minecraft.resources.ResourceLocation id) {
        return id.getNamespace().equals("ali") || id.getPath().contains("loot");
    }
    public static List<ItemStack> outputs(IFocusGroup focus) {
        return focus.getItemStackFocuses(RecipeIngredientRole.OUTPUT).map(f->f.getTypedValue().getIngredient()).toList();
    }
    public static int rank(Object recipe,List<ItemStack> targets) {
        if(targets.isEmpty())return 0;
        if(recipe instanceof RecipeHolder<?> holder)recipe=holder.value();
        if(recipe instanceof AnalysisRecipe analysis) {
            if(matches(analysis.result(),targets))return 0;
            var secondary=analysis.secondaryOutput();
            if(secondary.min()>0 && !secondary.choices().isEmpty() && secondary.choices().stream().allMatch(s->matches(s,targets)))return 0;
            return 1;
        }
        if(net.neoforged.fml.ModList.get().isLoaded("create"))return CreateRecipePriorities.rank(recipe,targets);
        return 0;
    }
    static boolean matches(ItemStack stack,List<ItemStack> targets) {
        return !stack.isEmpty() && targets.stream().anyMatch(t->ItemStack.isSameItemSameComponents(stack,t));
    }
    public static <T> List<T> recipes(List<T> recipes,IFocusGroup focus) {
        var targets=outputs(focus);if(targets.isEmpty() || recipes.size()<2)return recipes;
        var guaranteed=new ArrayList<T>();var chance=new ArrayList<T>();
        for(var recipe:recipes)(rank(recipe,targets)==0?guaranteed:chance).add(recipe);
        if(guaranteed.isEmpty() || chance.isEmpty())return recipes;
        guaranteed.addAll(chance);return List.copyOf(guaranteed);
    }
    public static boolean mixedChance(List<?> recipes,IFocusGroup focus) {
        var targets=outputs(focus);if(targets.isEmpty())return false;
        boolean guaranteed=false,chance=false;
        for(var recipe:recipes) {
            if(rank(recipe,targets)==0)guaranteed=true;else chance=true;
            if(guaranteed && chance)return true;
        }
        return false;
    }
    public static List<IRecipeCategory<?>> categories(List<IRecipeCategory<?>> categories,IRecipeManager manager,IFocusGroup focus) {
        var targets=outputs(focus);var ranks=new IdentityHashMap<IRecipeCategory<?>,Integer>();
        for(var category:categories) {
            var id=category.getRecipeType().getUid();int rank=0;
            if(loot(id))rank=2;
            else if(!targets.isEmpty() && (id.equals(OmniraJeiPlugin.ANALYSIS.getUid()) || id.getNamespace().equals("create")))
                rank=categoryRank(category,manager,focus,targets);
            ranks.put(category,rank);
        }
        return categories.stream().sorted(Comparator.comparingInt(ranks::get)).toList();
    }
    private static <T> int categoryRank(IRecipeCategory<T> category,IRecipeManager manager,IFocusGroup focus,List<ItemStack> targets) {
        return manager.createRecipeLookup(category.getRecipeType()).limitFocus(focus.getAllFocuses()).get().anyMatch(r->rank(r,targets)==0)?0:1;
    }
}

package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.recipe.AnalysisRecipe;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.gui.recipes.lookups.*;
import mezz.jei.library.focus.Focus;
import mezz.jei.common.ingredients.TypedIngredient;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import java.lang.reflect.Proxy;
import java.util.*;

/** Development-only smoke test: exercises the transformed JEI lookup classes, not a copy. */
public final class JeiPrioritySmoke {
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type,java.lang.reflect.InvocationHandler handler) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);
    }
    @SuppressWarnings("unchecked") private static IRecipeCategory<AnalysisRecipe> category(String namespace,String path) {
        var type=RecipeType.create(namespace,path,AnalysisRecipe.class);
        return proxy(IRecipeCategory.class,(p,m,a)->m.getName().equals("getRecipeType")?type:null);
    }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    public static void run() {
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CreatePrioritySmoke.run();
        var focus=new Focus<>(RecipeIngredientRole.OUTPUT,TypedIngredient.createUnvalidated(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,new ItemStack(Items.DIAMOND)));
        var guaranteed=new AnalysisRecipe(Ingredient.of(Items.STONE),1,new ItemStack(Items.DIAMOND),0,0);
        var chance=new AnalysisRecipe(Ingredient.of(Items.STONE),1,new ItemStack(Items.IRON_INGOT),0,0,
                new AnalysisRecipe.Byproduct(List.of(new ItemStack(Items.DIAMOND)),0,1),0);
        var ordered=List.of(chance,guaranteed);
        IRecipeManager manager=proxy(IRecipeManager.class,(p,m,a)-> {
            if(!m.getName().equals("createRecipeLookup"))throw new UnsupportedOperationException(m.getName());
            return proxy(IRecipeLookup.class,(q,n,b)->n.getName().equals("get")?ordered.stream():q);
        });
        var processing=category("omnira","analysis");var loot=category("ali","chest_loot");
        var state=IngredientLookupState.create(manager,focus,List.of(loot,processing),null);
        check(state.getRecipeCategories().getFirst()==processing,"JEI processing still sorted behind loot");
        var recipes=FocusedRecipes.create(focus,manager,processing);
        check(recipes.getRecipes().equals(List.of(guaranteed,chance)),"JEI deterministic outputs not first");
        check(recipes.getRecipes()==recipes.getRecipes(),"JEI lookup sorting was not cached");
        var layouts=new mezz.jei.gui.recipes.layouts.LazyRecipeLayoutList<>(java.util.EnumSet.allOf(mezz.jei.common.config.RecipeSorterStage.class),null,recipes,null,manager,null,null,focus);
        check(layouts.size()==2,"Mixed-probability layout failed");
        var input=new Focus<>(RecipeIngredientRole.INPUT,TypedIngredient.createUnvalidated(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,new ItemStack(Items.STONE)));
        check(IngredientLookupState.create(manager,input,List.of(loot,processing),null).getRecipeCategories().getFirst()==processing,"Usage lookup did not prioritize processing");
        check(RecipePriorities.recipes(ordered,input)==ordered,"Input lookup should not sort by unrelated outputs");
        com.mojang.logging.LogUtils.getLogger().info("OMNIRA_JEI_PRIORITY_SMOKE_PASSED: transformed lookups, target-aware chance order and caching");
    }
}

package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.compat.jei.WrappedShapedJei;
import com.mcmagic.omnira.compat.jei.WorldInteractionJei;
import com.mcmagic.omnira.forging.AdvancedForgeRecipe;
import com.mcmagic.omnira.myriad.*;
import com.mcmagic.omnira.recipe.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.reversal.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;

/** Validate displayed examples against the real matching/assembly code, not a second recipe table. */
@GameTestHolder("omnira_recipe_display")
@PrefixGameTestTemplate(false)
public final class JeiRecipeContractGameTests {
    private static SimpleContainer samples(List<Ingredient> ingredients){
        var input=new SimpleContainer(ingredients.size());
        for(int i=0;i<ingredients.size();i++)if(!ingredients.get(i).isEmpty())
            input.setItem(i,ingredients.get(i).getItems()[0].copyWithCount(1));
        return input;
    }
    private static void count(String kind,int recipes,int examples){
        com.mcmagic.omnira.Omnira.LOGGER.info("JEI contract {}: {} recipes, {} examples",kind,recipes,examples);
    }
    @GameTest(template="spell_arena") public static void assemblyDisplaysHaveActualResults(GameTestHelper h){
        int recipes=0,examples=0;
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get())){
            var source=holder.value();var displays=source.displayRecipes();recipes++;
            h.assertTrue(!displays.isEmpty(),"No executable display: "+holder.id());
            for(var display:displays){
                var input=samples(display.slots());
                for(int slot=0;slot<7;slot++){
                    var previous=input.getItem(slot);
                    var choices=display.slots().get(slot).isEmpty()?new ItemStack[]{ItemStack.EMPTY}:display.slots().get(slot).getItems();
                    for(var choice:choices){
                        input.setItem(slot,choice.copyWithCount(1));examples++;
                        var actual=new AssemblyRecipe.Input(input);
                        h.assertTrue(source.matches(actual,h.getLevel()),"Displayed assembly input rejected: "+holder.id()+" slot "+slot+" "+choice);
                        h.assertTrue(ItemStack.matches(source.assemble(actual,h.getLevel().registryAccess()),display.result()),"Displayed assembly result differs: "+holder.id()+" slot "+slot);
                    }
                    input.setItem(slot,previous);
                }
            }
        }
        count("assembly",recipes,examples);h.succeed();
    }
    @GameTest(template="spell_arena") public static void advancedForgeDisplaysHaveActualResults(GameTestHelper h){
        int examples=0;
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ADVANCED_FORGE_TYPE.get());
        for(var holder:recipes){
            var recipe=holder.value();var input=new AdvancedForgeRecipe.Input(samples(recipe.slots()));examples++;
            h.assertTrue(recipe.matches(input,h.getLevel()),"Forge display inputs rejected: "+holder.id());
            h.assertTrue(ItemStack.matches(recipe.assemble(input,h.getLevel().registryAccess()),recipe.displayResult()),"Forge display result differs: "+holder.id());
        }
        count("advanced forge",recipes.size(),examples);h.succeed();
    }
    @GameTest(template="spell_arena") public static void singleInputDisplaysHonorCounts(GameTestHelper h){
        int examples=0,recipes=0;
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get())){
            var recipe=holder.value();recipes++;
            var choices=recipe.advanced()?recipe.ingredient().getItems():new ItemStack[]{ItemStack.EMPTY};
            h.assertTrue(choices.length>0,"Empty condensation display ingredient: "+holder.id());
            for(var choice:choices){
                var stack=choice.copyWithCount(recipe.advanced()?recipe.inputCount():0);examples++;
                h.assertTrue(recipe.matches(new SingleRecipeInput(stack),h.getLevel()),"Condensation display count rejected: "+holder.id());
            }
        }
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ANALYSIS_TYPE.get())){
            var recipe=holder.value();recipes++;
            for(var choice:recipe.ingredient().getItems()){
                var input=new SingleRecipeInput(choice.copyWithCount(recipe.count()));examples++;
                h.assertTrue(recipe.matches(input,h.getLevel()),"Analysis display input rejected: "+holder.id());
                h.assertTrue(ItemStack.matches(recipe.assemble(input,h.getLevel().registryAccess()),recipe.result()),"Analysis result differs: "+holder.id());
            }
        }
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.STARLIGHT_TYPE.get())){
            var recipe=holder.value();recipes++;
            for(var choice:recipe.ingredient().getItems()){
                var input=new SingleRecipeInput(choice.copyWithCount(1));examples++;
                h.assertTrue(recipe.matches(input,h.getLevel())&&ItemStack.matches(recipe.assemble(input,h.getLevel().registryAccess()),recipe.result()),"Starlight display differs: "+holder.id());
            }
        }
        count("single input",recipes,examples);h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionDisplaysExecuteWithExactInputs(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get());
        for(var holder:recipes){
            var recipe=holder.value();var items=new net.neoforged.neoforge.items.ItemStackHandler(9);
            for(int i=0;i<recipe.items().size();i++){
                var material=recipe.items().get(i);
                h.assertTrue(material.ingredient().getItems().length>0,"Empty reaction ingredient: "+holder.id());
                items.setStackInSlot(i,material.ingredient().getItems()[0].copyWithCount(material.count()));
            }
            var fluids=new PoolFluids(()->{});
            for(var fluid:recipe.fluidInputs())h.assertTrue(fluids.fill(fluid,FluidAction.EXECUTE)==fluid.getAmount(),"Reaction fluid display does not fit");
            h.assertTrue(recipe.support().isEmpty()||recipe.support().getItems().length>0,"Empty support display: "+holder.id());
            h.assertTrue(recipe.matches(new ReactionRecipe.Input(items,fluids,ingredient->true),h.getLevel()),"Exact reaction display inputs rejected: "+holder.id());
            for(var rolled:recipe.results())h.assertTrue(recipe.completeOutputs(rolled).size()==recipe.guaranteedResults().size()+1,"Random/guaranteed reaction outputs merged");
        }
        count("reaction",recipes.size(),recipes.size());h.succeed();
    }
    @GameTest(template="spell_arena") public static void catalysisDisplaysResolveToActualPlans(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(CatalysisContent.TYPE.get());int examples=0;
        for(var holder:recipes){
            var recipe=holder.value();
            for(var soil:recipe.substrate().getItems())for(var seed:recipe.seed().getItems()){
                var plan=CatalysisGrowth.resolve(h.getLevel(),soil,seed,recipe.fluid());examples++;
                h.assertTrue(plan!=null,"Catalysis display cannot resolve: "+holder.id());
                if(recipe.yield()==CatalysisGrowth.Yield.FIXED)h.assertTrue(ItemStack.matches(plan.product(),recipe.result()),"Catalysis display overridden by different output: "+holder.id());
            }
        }
        count("catalysis",recipes.size(),examples);h.succeed();
    }
    @GameTest(template="spell_arena") public static void reversalDisplaysAndFuelsAgree(GameTestHelper h){
        int recipes=0,examples=0;
        for(var type:List.of(ReversalContent.TYPE.get(),ReversalContent.INFUSION_TYPE.get()))
            for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(type)){
                var recipe=holder.value();recipes++;
                for(var stack:recipe.ingredient().getItems()){
                    h.assertTrue(recipe.matches(new SingleRecipeInput(stack.copyWithCount(recipe.count())),h.getLevel()),"Reversal display rejected: "+holder.id());examples++;
                    h.assertTrue(!recipe.outputs(stack).isEmpty(),"Reversal output missing");
                }
            }
        for(boolean spatial:new boolean[]{false,true})for(var fuel:ReversalFuels.all(spatial))
            h.assertTrue(!fuel.stack().isEmpty()&&ReversalFuels.value(fuel.stack(),spatial)==fuel.energy(),"Fuel display disagrees with execution: "+fuel.item());
        count("reversal",recipes,examples);h.succeed();
    }
    @GameTest(template="spell_arena") public static void evolutionMaterialExamplesAreAccepted(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(MiniatureWorldContent.TYPE.get());int examples=0;
        for(var holder:recipes){
            var recipe=holder.value();var stacks=new ArrayList<ItemStack>();
            for(var material:recipe.materials()){
                h.assertTrue(!material.examples().isEmpty(),"Evolution material has no display: "+holder.id());
                stacks.add(material.examples().getFirst());
                for(var example:material.examples()){
                    h.assertTrue(material.test(example)&&example.getCount()==material.requiredCount(example),"Evolution example type/count differs: "+holder.id());examples++;
                }
            }
            h.assertTrue(recipe.matches(new WorldDevelopmentRecipe.Input(stacks),h.getLevel()),"Evolution displayed material set rejected: "+holder.id());
        }
        count("evolution",recipes.size(),examples);h.succeed();
    }
    @GameTest(template="spell_arena") public static void wrappedCraftingKeepsAuthoredGrid(GameTestHelper h){
        var adapter=new WrappedShapedJei();int examples=0;
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)){
            if(!(holder.value() instanceof WrappedShapedRecipe recipe))continue;examples++;
            var wrapped=new RecipeHolder<WrappedShapedRecipe>(holder.id(),recipe);var shaped=recipe.shaped();
            h.assertTrue(adapter.getWidth(wrapped)==shaped.getWidth()&&adapter.getHeight(wrapped)==shaped.getHeight(),"Wrapped recipe shown as shapeless: "+holder.id());
            var input=CraftingInput.of(shaped.getWidth(),shaped.getHeight(),shaped.getIngredients().stream().map(i->i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy()).toList());
            h.assertTrue(recipe.matches(input,h.getLevel()),"Wrapped displayed grid rejected: "+holder.id());
            h.assertTrue(ItemStack.matches(recipe.assemble(input,h.getLevel().registryAccess()),recipe.getResultItem(h.getLevel().registryAccess())),"Wrapped output differs: "+holder.id());
        }
        h.assertTrue(examples==3,"Wrapped recipe coverage changed; update the audit");count("wrapped crafting",examples,examples);h.succeed();
    }
    @GameTest(template="spell_arena") public static void worldTargetsAreNotInventedIngredients(GameTestHelper h){
        for(var entry:WorldInteractionJei.entries()){
            if(entry.note().equals("deflection")||entry.note().equals("mist"))
                h.assertTrue(entry.targetRole()==mezz.jei.api.recipe.RecipeIngredientRole.RENDER_ONLY,"Symbolic target registered as consumed material");
            if(entry.note().equals("campfire"))h.assertTrue(entry.targetRole()==mezz.jei.api.recipe.RecipeIngredientRole.CATALYST,"Campfire retention lost");
        }
        for(boolean permanent:new boolean[]{false,true})h.assertTrue(com.mcmagic.omnira.spacetime.CorridorRitual.acceptsActivator(
                new ItemStack(com.mcmagic.omnira.spacetime.CorridorRitual.activator(permanent))),"Corridor display activator rejected");
        h.succeed();
    }
    private JeiRecipeContractGameTests(){}
}

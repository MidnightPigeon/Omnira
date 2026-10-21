package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.ModRecipes;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** Actual recipe data: distinguish failed chemistry from automatic competing reactions. */
@GameTestHolder("omnira_genesis_reactions")
@PrefixGameTestTemplate(false)
public final class GenesisReactionConcurrencyGameTests {
    private static final List<String> TARGETS=List.of("boundary_crystal_membrane","geothermal_ember","living_embryoplasm","phantasm_germinal_shard","strata_skeleton");
    private static ReactionRecipe recipe(GameTestHelper h,String id){return (ReactionRecipe)h.getLevel().getRecipeManager()
            .byKey(ResourceLocation.parse("omnira:reaction/"+id)).orElseThrow().value();}
    private static ReactionPoolBlockEntity pool(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        return ReactionPoolBlock.controller(h.getLevel(),pos);
    }
    private static void prepare(GameTestHelper h,ReactionPoolBlockEntity pool,ReactionRecipe r,boolean enoughForBoth){
        h.assertTrue(!pool.reacting(),"Previous reaction still active");
        for(int i=0;i<9;i++)pool.items.setStackInSlot(i,ItemStack.EMPTY);
        for(var f:pool.fluids.snapshot())pool.manualFluid.drain(f,FluidAction.EXECUTE);
        h.getLevel().setBlockAndUpdate(pool.getBlockPos().below(),Block.byItem(r.support().getItems()[0].getItem()).defaultBlockState());
        for(int i=0;i<r.items().size();i++)pool.items.setStackInSlot(i,r.items().get(i).ingredient().getItems()[0].copyWithCount(r.items().get(i).count()));
        for(var fluid:r.fluidInputs())h.assertTrue(pool.manualFluid.fill(fluid,FluidAction.EXECUTE)==fluid.getAmount(),"Fluid input rejected");
        if(enoughForBoth){var extra=recipe(h,"time_space_microcores").fluid();h.assertTrue(pool.manualFluid.fill(extra,FluidAction.EXECUTE)==extra.getAmount(),"Extra refined spacetime rejected");}
    }
    private static int count(ReactionPoolBlockEntity pool,Item item){int count=0;for(int i=0;i<9;i++)if(pool.items.getStackInSlot(i).is(item))count+=pool.items.getStackInSlot(i).getCount();return count;}
    private static List<ReactionRecipe> available(GameTestHelper h){return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream().map(r->r.value()).toList();}
    private static void bothCatalysts(GameTestHelper h,ReactionPoolBlockEntity pool){
        h.getLevel().setBlockAndUpdate(pool.getBlockPos().offset(1,-1,0),com.mcmagic.omnira.registry.ModBlocks.ROUGH_ALCHEMICAL_ENERGY_CORE.get().defaultBlockState());
    }
    @GameTest(template="spell_arena") public static void enhancedCatalystSeparatesFiveRecipesFromMicrocoreDissociation(GameTestHelper h){
        var pool=pool(h);var micro=recipe(h,"time_space_microcores");
        h.assertTrue(micro.support().test(new ItemStack(com.mcmagic.omnira.registry.ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get()))
                &&!micro.support().test(new ItemStack(com.mcmagic.omnira.alchemy.AlchemyMaterials.ENHANCED_CORE_ITEM.get())),"Microcore catalyst contract incorrect");
        for(String id:TARGETS)for(boolean reverse:new boolean[]{false,true}){
            var r=recipe(h,id);prepare(h,pool,r,false);var all=new ArrayList<>(available(h));if(reverse)Collections.reverse(all);
            pool.startAvailable(all);h.assertTrue(pool.reactionCount()==1,"Microcore reaction still started under enhanced core: "+id);
            for(int i=0;i<250;i++)pool.advance(8);
            h.assertTrue(!pool.reacting()&&pool.fluids.total()==0&&count(pool,r.allItemResults().getFirst().getItem())==1
                    &&micro.allItemResults().stream().allMatch(s->count(pool,s.getItem())==0),"Catalyst-isolated target failed: "+id);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void fiveRecipesCompleteAloneAndAfterReload(GameTestHelper h){
        var pool=pool(h);
        for(String id:TARGETS){
            var r=recipe(h,id);prepare(h,pool,r,false);h.assertTrue(pool.start(r),"Solo recipe rejected: "+id);
            for(int i=0;i<5;i++)pool.advance(200);
            var copy=new ReactionPoolBlockEntity(pool.getBlockPos(),pool.getBlockState());copy.setLevel(h.getLevel());
            copy.loadWithComponents(pool.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            pool.advance(1000);copy.advance(1000);
            var product=r.allItemResults().getFirst();
            for(var completed:List.of(pool,copy))h.assertTrue(!completed.reacting()&&completed.fluids.total()==0&&count(completed,product.getItem())==product.getCount(),"Lost output or incorrect consumption: "+id);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void mixedCatalystsRetainSharedFluidFailureRules(GameTestHelper h){
        var pool=pool(h);var micro=recipe(h,"time_space_microcores");
        bothCatalysts(h,pool);
        for(String id:TARGETS)for(boolean reverse:new boolean[]{false,true}){
            var r=recipe(h,id);prepare(h,pool,r,false);var recipes=new ArrayList<>(available(h));if(reverse)Collections.reverse(recipes);
            pool.startAvailable(recipes);h.assertTrue(pool.reactionCount()==2,"Expected target plus automatic microcore job: "+id);
            for(int i=0;i<5;i++)pool.advance(200);
            h.assertTrue(pool.fluids.amount(r.fluid())==0,"Shared 1B refined spacetime not exhausted at half progress: "+id);
            for(var additional:r.additionalFluids())h.assertTrue(pool.fluids.amount(additional)==additional.getAmount()/2,"Secondary fluid was not half consumed: "+id);
            pool.advance(200);
            h.assertTrue(!pool.reacting()&&count(pool,r.allItemResults().getFirst().getItem())==0
                    &&micro.allItemResults().stream().allMatch(s->count(pool,s.getItem())==0),"Missing-fluid failure changed: "+id);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void enoughSharedFluidProducesBothReactions(GameTestHelper h){
        var pool=pool(h);var micro=recipe(h,"time_space_microcores");
        bothCatalysts(h,pool);
        for(String id:TARGETS){
            var r=recipe(h,id);prepare(h,pool,r,true);pool.startAvailable(available(h));
            h.assertTrue(pool.reactionCount()==2,"Adequately supplied parallel reactions rejected: "+id);
            for(int i=0;i<10;i++)pool.advance(200);
            h.assertTrue(!pool.reacting()&&pool.fluids.total()==0&&count(pool,r.allItemResults().getFirst().getItem())==1
                    &&micro.allItemResults().stream().allMatch(s->count(pool,s.getItem())==1),"Adequately supplied parallel reaction lost output: "+id);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void recipeControlIsolatesEveryTargetWithIncrementalCapabilityInput(GameTestHelper h){
        var pool=pool(h);
        bothCatalysts(h,pool);
        for(String id:TARGETS){
            var r=recipe(h,id);var plate=new ItemStack(com.mcmagic.omnira.registry.ModItems.RECIPE_CONTROL_UPGRADE.get());
            var settings=com.mcmagic.omnira.block.entity.OrbUpgrades.settings(plate);settings.putBoolean("Whitelist",true);
            plate.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(settings));
            com.mcmagic.omnira.block.entity.OrbUpgrades.setFilter(plate,0,r.allItemResults().getFirst(),h.getLevel().registryAccess());pool.upgrades.setStackInSlot(0,plate);
            prepare(h,pool,r,false);var items=new ArrayList<ItemStack>();for(int i=0;i<9;i++){items.add(pool.items.getStackInSlot(i).copy());pool.items.setStackInSlot(i,ItemStack.EMPTY);}
            var fluids=pool.fluids.snapshot();for(var fluid:fluids)pool.manualFluid.drain(fluid,FluidAction.EXECUTE);
            for(var fluid:fluids){h.assertTrue(pool.fluidInput.fill(fluid,FluidAction.EXECUTE)==fluid.getAmount(),"Capability input rejected");pool.startAvailable(available(h));}
            for(var item:items)if(!item.isEmpty()){
                h.assertTrue(net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(pool.itemInput,item,false).isEmpty(),"Capability item input rejected");pool.startAvailable(available(h));
            }
            h.assertTrue(pool.reactionCount()==1,"Whitelisted recipe did not stay isolated: "+id);
            for(int i=0;i<250;i++)pool.advance(8);
            h.assertTrue(!pool.reacting()&&pool.fluids.total()==0&&count(pool,r.allItemResults().getFirst().getItem())==1,"Whitelisted reaction lost output: "+id);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void allFiveAndMicrocoresFinishTogether(GameTestHelper h){
        var pool=pool(h);var recipes=TARGETS.stream().map(id->recipe(h,id)).toList();
        bothCatalysts(h,pool);
        h.getLevel().setBlockAndUpdate(pool.getBlockPos().below(),Block.byItem(recipes.getFirst().support().getItems()[0].getItem()).defaultBlockState());
        for(var r:recipes){
            for(var item:r.items())h.assertTrue(net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(pool.itemInput,
                    item.ingredient().getItems()[0].copyWithCount(item.count()),false).isEmpty(),"Parallel item input rejected");
            for(var fluid:r.fluidInputs())h.assertTrue(pool.fluidInput.fill(fluid,FluidAction.EXECUTE)==fluid.getAmount(),"Parallel fluid input rejected");
        }
        var micro=recipe(h,"time_space_microcores");h.assertTrue(pool.fluidInput.fill(micro.fluid(),FluidAction.EXECUTE)==1000,"Microcore fluid input rejected");
        pool.startAvailable(available(h));h.assertTrue(pool.reactionCount()==6,"Expected five targets and one microcore job");
        for(int i=0;i<250;i++)pool.advance(8);
        h.assertTrue(!pool.reacting()&&pool.fluids.total()==0,"Parallel jobs did not settle exact fluid amounts");
        for(var r:recipes)h.assertTrue(count(pool,r.allItemResults().getFirst().getItem())==1,"Missing simultaneous output: "+r.allItemResults().getFirst());
        for(var output:micro.allItemResults())h.assertTrue(count(pool,output.getItem())==1,"Missing simultaneous microcore output");
        h.succeed();
    }
}

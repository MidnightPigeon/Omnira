package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.workstation.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;

@GameTestHolder("omnira_machine_execution")
@PrefixGameTestTemplate(false)
public final class MachineRecipeExecutionGameTests {
    @GameTest(template="spell_arena") public static void allRegisteredRecipeIngredientsResolve(GameTestHelper h){
        int count=0;var types=new TreeMap<String,Integer>();
        for(var holder:h.getLevel().getRecipeManager().getRecipes()){
            var recipe=holder.value();
            if(!holder.id().getNamespace().equals("omnira")&&!net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()).getNamespace().equals("omnira"))continue;
            var inputs=new ArrayList<net.minecraft.world.item.crafting.Ingredient>();inputs.addAll(recipe.getIngredients());
            if(recipe instanceof ReactionRecipe r){for(var item:r.items())inputs.add(item.ingredient());inputs.add(r.support());}
            if(recipe instanceof com.mcmagic.omnira.recipe.AssemblyRecipe r)inputs.addAll(r.slots());
            if(recipe instanceof com.mcmagic.omnira.forging.AdvancedForgeRecipe r)inputs.addAll(r.slots());
            if(recipe instanceof com.mcmagic.omnira.recipe.AnalysisRecipe r)inputs.add(r.ingredient());
            if(recipe instanceof com.mcmagic.omnira.recipe.CondensationRecipe r)inputs.add(r.ingredient());
            if(recipe instanceof com.mcmagic.omnira.reversal.ReversalRecipe r)inputs.add(r.ingredient());
            if(recipe instanceof com.mcmagic.omnira.alchemy.catalysis.CatalysisRecipe r){inputs.add(r.substrate());inputs.add(r.seed());}
            if(recipe instanceof com.mcmagic.omnira.recipe.StarlightConversionRecipe r)inputs.add(r.ingredient());
            if(recipe instanceof com.mcmagic.omnira.recipe.VesselConversionRecipe r)inputs.add(r.ingredient());
            if(recipe instanceof com.mcmagic.omnira.recipe.SwordShapingRecipe r)inputs.addAll(r.offerings());
            if(recipe instanceof com.mcmagic.omnira.myriad.WorldDevelopmentRecipe r)for(var item:r.materials())item.ingredient().ifPresent(inputs::add);
            for(var ingredient:inputs)if(!ingredient.isEmpty())h.assertTrue(ingredient.getItems().length>0,"Unresolvable ingredient/tag in "+holder.id());
            types.merge(recipe.getType().toString(),1,Integer::sum);count++;
        }
        h.assertTrue(count>100,"Registered recipe inventory unexpectedly small");
        org.slf4j.LoggerFactory.getLogger("OmniraRecipeAudit").info("Resolved {} registered Omnira-owned/custom recipe inputs: {}",count,types);h.succeed();
    }
    private static ReactionPoolBlockEntity pool(GameTestHelper h,ReactionRecipe recipe){
        var pos=h.absolutePos(new BlockPos(5,4,5));
        var pool=new ReactionPoolBlockEntity(pos,ModBlocks.REACTION_POOL.get().defaultBlockState());pool.setLevel(h.getLevel());
        if(!recipe.support().isEmpty()){
            var block=Arrays.stream(recipe.support().getItems()).filter(s->s.getItem() instanceof BlockItem).findFirst().orElseThrow();
            h.getLevel().setBlockAndUpdate(pos.below(),((BlockItem)block.getItem()).getBlock().defaultBlockState());
        }
        for(int i=0;i<recipe.items().size();i++){
            var input=recipe.items().get(i);pool.items.setStackInSlot(i,input.ingredient().getItems()[0].copyWithCount(input.count()));
        }
        for(var fluid:recipe.fluidInputs())h.assertTrue(pool.manualFluid.fill(fluid.copy(),FluidAction.EXECUTE)==fluid.getAmount(),"Recipe input does not fit shared tank");
        return pool;
    }
    @GameTest(template="spell_arena") public static void furnaceExecutesResolvedSmeltingCatalogue(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();int tested=0;
        for(var holder:manager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)){
            var ingredient=holder.value().getIngredients().getFirst();
            if(ingredient.getItems().length==0)continue;
            var input=ingredient.getItems()[0].copyWithCount(1);
            var recipe=manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,new net.minecraft.world.item.crafting.SingleRecipeInput(input),h.getLevel()).orElseThrow().value();
            var work=new CrystalWorkstationEntity(h.absolutePos(new BlockPos(5,4,5)),CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState());work.setLevel(h.getLevel());
            work.setItem(0,input.copy());
            for(int tick=0;tick<Math.max(1,recipe.getCookingTime()/2);tick++)work.tick(true,16);
            var expected=recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(input),h.getLevel().registryAccess());
            h.assertTrue(ItemStack.matches(work.getItem(2),expected)&&work.getItem(0).isEmpty(),"Smelting execution failed for "+holder.id());tested++;
        }
        h.assertTrue(tested>20,"Smelting execution coverage unexpectedly small");
        org.slf4j.LoggerFactory.getLogger("OmniraRecipeAudit").info("Executed {} smelting inputs using their resolved recipe, with continuous mechanical power",tested);h.succeed();
    }
    private static boolean outputMatches(ReactionPoolBlockEntity pool,List<ItemStack> outputs){
        var remaining=new ArrayList<ItemStack>();for(var output:outputs)remaining.add(output.copy());
        for(int slot=0;slot<9;slot++){
            var actual=pool.items.getStackInSlot(slot);int count=actual.getCount();
            for(var expected:remaining)if(ItemStack.isSameItemSameComponents(actual,expected)){
                int take=Math.min(count,expected.getCount());count-=take;expected.shrink(take);
            }
            if(count!=0)return false;
        }return remaining.stream().allMatch(ItemStack::isEmpty);
    }
    @GameTest(template="spell_arena") public static void everyRegisteredReactionExecutesAcrossReload(GameTestHelper h){
        int tested=0;
        for(var holder:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get())){
            var recipe=holder.value();var pool=pool(h,recipe);
            h.assertTrue(pool.start(recipe),"Registered reaction cannot start: "+holder.id());
            pool.advance(recipe.duration()*.37);
            var restored=new ReactionPoolBlockEntity(pool.getBlockPos(),pool.getBlockState());restored.setLevel(h.getLevel());
            restored.loadWithComponents(pool.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            restored.advance(recipe.duration());
            h.assertTrue(restored.reactionCount()==0,"Registered reaction did not finish: "+holder.id());
            h.assertTrue(recipe.outputOptions().stream().anyMatch(option->outputMatches(restored,option)),"Reaction item mass mismatch: "+holder.id());
            h.assertTrue(restored.fluids.total()==recipe.fluidResult().getAmount(),"Reaction fluid mass mismatch: "+holder.id());
            if(!recipe.fluidResult().isEmpty())h.assertTrue(restored.fluids.amount(recipe.fluidResult())==recipe.fluidResult().getAmount(),"Wrong product fluid: "+holder.id());
            var done=restored.saveWithFullMetadata(h.getLevel().registryAccess());restored.advance(recipe.duration());
            h.assertTrue(done.equals(restored.saveWithFullMetadata(h.getLevel().registryAccess())),"Finished reaction repeated: "+holder.id());tested++;
        }
        h.assertTrue(tested>10,"Reaction execution coverage unexpectedly small");
        org.slf4j.LoggerFactory.getLogger("OmniraRecipeAudit").info("Executed {} registered reactions through matching, consumption, partial work, reload and final outputs",tested);
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void starvedPulseFurnaceCannotFallbackToFuel(GameTestHelper h){
        var pos=new BlockPos(5,4,5);
        h.setBlock(pos,CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        PulseRelayTests.engine(h,pos.north(),Direction.SOUTH);
        var source=((com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getBlockEntity(pos.north())).engineState();source.control(0);
        var work=CrystalWorkstations.work(h.getBlockEntity(pos));work.setItem(0,new ItemStack(Items.IRON_ORE));work.setItem(1,new ItemStack(Items.COAL));
        var tag=new net.minecraft.nbt.CompoundTag();work.writeWork(tag,h.getLevel().registryAccess());tag.putInt("Burn",100);tag.putInt("Progress",20);work.readWork(tag,h.getLevel().registryAccess());
        for(int i=0;i<10;i++)work.tick(false,0);
        h.assertTrue(work.pulseConnected&&!work.pulsePowered,"Unpowered pulse source was not connected");
        h.assertTrue(work.progress==20&&work.burn==100&&work.getItem(1).getCount()==1,"Fuel bypassed pulse shutdown");
        source.control(0);work.tick(false,0);
        h.assertTrue(work.pulsePowered&&work.progress==21,"Pulse restoration did not resume furnace");h.succeed();
    }
    @GameTest(template="spell_arena") public static void mixedParallelRecipesConserveAcrossRepeatedReloads(GameTestHelper h){
        var random=new Random(9301);var inputs=List.of(Items.STONE,Items.DIRT,Items.STICK,Items.COBBLESTONE);
        var outputs=List.of(Items.DIAMOND,Items.EMERALD,Items.QUARTZ,Items.GOLD_NUGGET);
        for(int scenario=0;scenario<24;scenario++){
            var pos=h.absolutePos(new BlockPos(5,4,5));
            var pool=new ReactionPoolBlockEntity(pos,ModBlocks.REACTION_POOL.get().defaultBlockState());pool.setLevel(h.getLevel());
            pool.manualFluid.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,5000),FluidAction.EXECUTE);
            var recipes=new ArrayList<ReactionRecipe>();
            for(int i=0;i<4;i++){
                pool.items.setStackInSlot(i,new ItemStack(inputs.get(i)));
                recipes.add(new ReactionRecipe(List.of(new ReactionRecipe.InputItem(net.minecraft.world.item.crafting.Ingredient.of(inputs.get(i)),1)),
                        new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,101+i*17),net.minecraft.world.item.crafting.Ingredient.EMPTY,
                        List.of(new ItemStack(outputs.get(i)),new ItemStack(outputs.get(i),2)),net.neoforged.neoforge.fluids.FluidStack.EMPTY,11+i*13));
            }
            Collections.shuffle(recipes,random);pool.startAvailable(recipes);
            h.assertTrue(pool.reactionCount()==4,"Mixed recipes failed to start");
            int steps=0;
            while(pool.reacting()&&steps++<100){
                pool.advance(.25+random.nextInt(8));
                if(steps%3==0){
                    var saved=pool.saveWithFullMetadata(h.getLevel().registryAccess());
                    var restored=new ReactionPoolBlockEntity(pos,pool.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());
                    h.assertTrue(saved.getList("Reactions",10).equals(restored.saveWithFullMetadata(h.getLevel().registryAccess()).getList("Reactions",10)),"Reload rerolled or changed parallel jobs");pool=restored;
                }
            }
            h.assertTrue(!pool.reacting()&&pool.fluids.total()==4494,"Mixed work lost fluid or failed to finish");
            for(var item:outputs){int amount=0;for(int i=0;i<9;i++)if(pool.items.getStackInSlot(i).is(item))amount+=pool.items.getStackInSlot(i).getCount();
                h.assertTrue(amount==1||amount==2,"Mixed work duplicated or lost product");}
        }h.succeed();
    }
}

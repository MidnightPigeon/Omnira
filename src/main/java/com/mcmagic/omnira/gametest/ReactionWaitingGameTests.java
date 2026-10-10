package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.*;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class ReactionWaitingGameTests {
    private static final IFluidHandler.FluidAction EXECUTE=IFluidHandler.FluidAction.EXECUTE;
    private static RoughAlchemyCauldronBlockEntity vessel(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        return (RoughAlchemyCauldronBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static ReactionPoolBlockEntity pool(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(12,3,12));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        return ReactionPoolBlock.controller(h.getLevel(),pos);
    }
    private static void ticks(GameTestHelper h,RoughAlchemyCauldronBlockEntity v,int count){
        for(int i=0;i<count;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),v.getBlockPos(),v.getBlockState(),v);
    }
    private static RoughAlchemyCauldronBlockEntity reload(GameTestHelper h,RoughAlchemyCauldronBlockEntity v){
        var copy=new RoughAlchemyCauldronBlockEntity(v.getBlockPos(),v.getBlockState());
        copy.loadWithComponents(v.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(copy);return copy;
    }
    private static ReactionPoolBlockEntity reload(GameTestHelper h,ReactionPoolBlockEntity p){
        var copy=new ReactionPoolBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(p.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());return copy;
    }
    private static ReactionRecipe itemRecipe(Item input,Item output){
        return new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(input),1)),FluidStack.EMPTY,
                Ingredient.EMPTY,List.of(new ItemStack(output)),FluidStack.EMPTY,2000);
    }
    @GameTest(template="spell_arena") public static void catalystOnlyProduction(GameTestHelper h){
        var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(ResourceLocation.parse("omnira:reaction/spiritual_crystal"))).findFirst().orElseThrow().value();
        var v=vessel(h);var p=pool(h);
        h.assertTrue(!v.start(recipe)&&!p.start(recipe),"Catalyst-only reaction started without its catalyst");
        var catalyst=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("omnira:primordial_spell_core"));
        h.getLevel().setBlockAndUpdate(v.getBlockPos().below(),catalyst.defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.getBlockPos().below(),catalyst.defaultBlockState());
        h.assertTrue(recipe.items().isEmpty()&&recipe.fluidInputs().isEmpty()&&v.start(recipe)&&p.start(recipe),"Empty-input recipe failed to start");
        ticks(h,v,2000);p.advance(2000);
        h.assertTrue(v.items.getStackInSlot(0).is(ModItems.SPIRITUAL_CRYSTAL.get())&&v.items.getStackInSlot(0).getCount()==1
                &&p.items.getStackInSlot(0).is(ModItems.SPIRITUAL_CRYSTAL.get())&&p.items.getStackInSlot(0).getCount()==1,"Wrong catalyst-only yield");
        h.assertTrue(h.getLevel().getBlockState(v.getBlockPos().below()).is(catalyst)&&h.getLevel().getBlockState(p.getBlockPos().below()).is(catalyst),"Catalyst consumed");
        h.assertTrue(v.start(recipe)&&p.start(recipe),"Continuous production failed to restart");
        v.items.setStackInSlot(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get(),64));p.items.setStackInSlot(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get(),64));
        ticks(h,v,2000);p.advance(2000);
        h.assertTrue(v.working()&&v.progress()==2000&&p.reacting()&&p.progressPercent()==100,"Full crystal stack did not hold completed production");
        boolean rejected=false;try{new ReactionRecipe(List.of(),FluidStack.EMPTY,Ingredient.EMPTY,List.of(new ItemStack(Items.DIAMOND)),FluidStack.EMPTY,2000);}
        catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Unconditional empty-input recipe accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cauldronItemWaitSurvivesReload(GameTestHelper h){
        var v=vessel(h);var recipe=new ReactionRecipe(List.of(),new FluidStack(Fluids.WATER,1000),Ingredient.EMPTY,
                List.of(new ItemStack(Items.DIAMOND),new ItemStack(Items.EMERALD)),FluidStack.EMPTY,2000);
        v.tank.fill(recipe.fluid(),EXECUTE);h.assertTrue(v.start(recipe),"Reaction failed to start");
        for(int i=0;i<4;i++)v.items.setStackInSlot(i,new ItemStack(Items.STONE,64));
        ticks(h,v,2500);h.assertTrue(v.working()&&v.progress()==2000&&v.tank.isEmpty(),"Full inventory failed to wait at completion");
        var saved=v.saveWithFullMetadata(h.getLevel().registryAccess());var rolled=ItemStack.parseOptional(h.getLevel().registryAccess(),saved.getCompound("Output"));
        v=reload(h,v);ticks(h,v,10);h.assertTrue(v.working()&&v.progress()==2000,"Reload lost completion wait");
        v.items.setStackInSlot(0,ItemStack.EMPTY);ticks(h,v,1);
        h.assertTrue(!v.working()&&ItemStack.isSameItemSameComponents(rolled,v.items.getStackInSlot(0))&&v.items.getStackInSlot(0).getCount()==1,"Waiting rerolled, lost or duplicated output");h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolAllItemOutputsWaitAtomically(GameTestHelper h){
        var p=pool(h);var recipe=new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(Items.STICK),1)),FluidStack.EMPTY,
                Ingredient.EMPTY,List.of(new ItemStack(Items.DIAMOND)),FluidStack.EMPTY,2000,List.of(),List.of(new ItemStack(Items.EMERALD)));
        p.items.setStackInSlot(0,new ItemStack(Items.STICK));h.assertTrue(p.start(recipe),"Reaction failed to start");
        for(int i=0;i<9;i++)p.items.setStackInSlot(i,new ItemStack(Items.STONE,64));p.items.setStackInSlot(0,ItemStack.EMPTY);
        p.advance(2000);h.assertTrue(p.reacting()&&p.progressPercent()==100&&p.items.getStackInSlot(0).isEmpty(),"Outputs were partially delivered");
        p=reload(h,p);p.advance(2000);h.assertTrue(p.reacting()&&p.items.getStackInSlot(0).isEmpty(),"Reload lost atomic completion wait");
        p.items.setStackInSlot(1,ItemStack.EMPTY);p.advance(1);
        h.assertTrue(!p.reacting()&&p.items.getStackInSlot(0).is(Items.EMERALD)&&p.items.getStackInSlot(1).is(Items.DIAMOND),"Completed output set lost");h.succeed();
    }
    @GameTest(template="spell_arena") public static void blockedJobDoesNotStopOtherJobs(GameTestHelper h){
        var p=pool(h);p.items.setStackInSlot(0,new ItemStack(Items.STICK));p.items.setStackInSlot(1,new ItemStack(Items.FLINT));
        h.assertTrue(p.start(itemRecipe(Items.STICK,Items.DIAMOND))&&p.start(itemRecipe(Items.FLINT,Items.EMERALD)),"Parallel recipes failed");
        p.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,64));p.advance(2000);
        h.assertTrue(p.reactionCount()==1&&p.items.getStackInSlot(1).is(Items.EMERALD),"Blocked job stopped independent output");
        p.items.extractItem(0,1,false);p.advance(1);
        h.assertTrue(!p.reacting()&&p.items.getStackInSlot(0).getCount()==64,"Blocked job failed to resume");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cauldronFluidWaitAndTenInstallments(GameTestHelper h){
        var v=vessel(h);var recipe=new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(Items.STICK),1)),FluidStack.EMPTY,
                Ingredient.EMPTY,List.of(),new FluidStack(Fluids.WATER,1000),2000);
        v.items.setStackInSlot(0,new ItemStack(Items.STICK));h.assertTrue(v.start(recipe),"Fluid reaction failed");
        ticks(h,v,199);h.assertTrue(v.tank.isEmpty(),"Fluid appeared before first installment");ticks(h,v,1);
        h.assertTrue(v.tank.getFluidAmount()==100,"Cauldron did not produce one tenth");
        v.tank.setFluid(new FluidStack(Fluids.WATER,4000));ticks(h,v,700);
        h.assertTrue(v.progress()==400&&v.working()&&v.tank.getFluidAmount()==4000,"Full fluid output failed to hold its stage");
        v=reload(h,v);v.tank.drain(100,EXECUTE);ticks(h,v,1);
        h.assertTrue(v.tank.getFluidAmount()==4000&&v.progress()==401,"Reload duplicated installments or accumulated blocked work");
        v.tank.drain(4000,EXECUTE);ticks(h,v,1599);
        h.assertTrue(!v.working()&&v.tank.getFluidAmount()==800,"Remaining installments lost or duplicated");h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolBlockedFluidDoesNotConsumeReagent(GameTestHelper h){
        var p=pool(h);var recipe=new ReactionRecipe(List.of(),new FluidStack(Fluids.LAVA,1000),Ingredient.EMPTY,
                List.of(),new FluidStack(Fluids.WATER,2000),2000);
        p.manualFluid.fill(recipe.fluid(),EXECUTE);h.assertTrue(p.start(recipe),"Fluid reaction failed");p.advance(200);
        p.fluids.fill(new FluidStack(Fluids.WATER,38900),EXECUTE);p.advance(1800);p.advance(2000);
        h.assertTrue(p.progressPercent()==20&&p.fluids.amount(recipe.fluid())==900&&p.fluids.total()==40000,"Blocked fluid stage consumed reagent or accumulated work");
        p=reload(h,p);p.fluids.drain(new FluidStack(Fluids.WATER,100),EXECUTE);p.advance(1);
        h.assertTrue(p.fluids.amount(recipe.fluid())==800&&p.fluids.total()==40000,"Resumption did not settle exactly one installment");
        p.fluids.drain(new FluidStack(Fluids.WATER,40000),EXECUTE);p.advance(2000);
        h.assertTrue(!p.reacting()&&p.fluids.amount(recipe.fluid())==0&&p.fluids.amount(recipe.fluidResult())==1600,"Remaining fluid installments were incorrect");h.succeed();
    }
}

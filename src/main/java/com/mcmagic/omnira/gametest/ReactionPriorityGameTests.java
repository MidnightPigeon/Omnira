package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class ReactionPriorityGameTests {
    private static ReactionRecipe reaction(boolean catalyst){
        return new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(Items.STICK),1)),new FluidStack(Fluids.WATER,1000),
                catalyst?Ingredient.of(Blocks.STONE):Ingredient.EMPTY,List.of(new ItemStack(catalyst?Items.DIAMOND:Items.EMERALD)),FluidStack.EMPTY,2000);
    }
    @GameTest(template="spell_arena") public static void primitiveCatalystPriorityAndFallback(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var vessel=(RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
        var generic=reaction(false);var catalytic=reaction(true);
        for(int scenario=0;scenario<4;scenario++){
            for(int i=0;i<4;i++)vessel.items.setStackInSlot(i,ItemStack.EMPTY);
            vessel.items.setStackInSlot(0,new ItemStack(Items.STICK));vessel.tank.setFluid(new FluidStack(Fluids.WATER,1000));
            level.setBlockAndUpdate(pos.below(),(scenario==1||scenario==3?Blocks.AIR:Blocks.STONE).defaultBlockState());
            if(scenario==2)vessel.items.setStackInSlot(1,new ItemStack(Items.DIAMOND,64));
            h.assertTrue(vessel.startNext(List.of(generic,catalytic)),"No eligible reaction selected");
            h.assertTrue(vessel.items.getStackInSlot(0).isEmpty(),"Selected recipe did not consume its ingredient");
            if(scenario==3){
                level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
                vessel.items.setStackInSlot(3,new ItemStack(Items.STICK));
                h.assertTrue(!vessel.startNext(List.of(catalytic,generic)),"Catalyst interrupted active reaction");
            }
            for(int tick=0;tick<2000;tick++)RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
            h.assertTrue(vessel.items.getStackInSlot(0).is(scenario==0?Items.DIAMOND:Items.EMERALD),"Catalytic priority/fallback incorrect for scenario "+scenario);
            h.assertTrue(!vessel.working()&&vessel.tank.isEmpty(),"Primitive reaction duplicated or failed to finish");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolRunsBothWithoutSharingReagents(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        h.assertTrue(ReactionPoolBlock.place(level,pos,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(level,pos);level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        pool.items.setStackInSlot(0,new ItemStack(Items.STICK,2));pool.manualFluid.fill(new FluidStack(Fluids.WATER,2000),EXECUTE);
        h.assertTrue(pool.start(reaction(false))&&pool.start(reaction(true))&&pool.reactionCount()==2,"Overlapping catalytic and generic jobs did not run together");
        pool.advance(200);h.assertTrue(pool.fluids.total()==1800,"Parallel jobs did not each consume their own batch");pool.advance(1800);
        boolean diamond=false,emerald=false;
        for(int i=0;i<9;i++){diamond|=pool.items.getStackInSlot(i).is(Items.DIAMOND);emerald|=pool.items.getStackInSlot(i).is(Items.EMERALD);}
        h.assertTrue(diamond&&emerald&&!pool.reacting()&&pool.fluids.total()==0,"Parallel reaction outputs or consumption incorrect");
        pool.items.setStackInSlot(2,new ItemStack(Items.STICK,2));pool.manualFluid.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);
        pool.startAvailable(List.of(reaction(false),reaction(true)));
        h.assertTrue(pool.reactionCount()==2&&pool.fluids.total()==1000,"Liquid requirements were incorrectly added for admission");
        pool.advance(200);h.assertTrue(pool.fluids.total()==800,"Concurrent liquid use was shared instead of independently debited");h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolCatalystConsumesItemsBeforeGeneric(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        h.assertTrue(ReactionPoolBlock.place(level,pos,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(level,pos);level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        pool.items.setStackInSlot(0,new ItemStack(Items.STICK));pool.manualFluid.fill(new FluidStack(Fluids.WATER,1000),EXECUTE);
        pool.startAvailable(List.of(reaction(false),reaction(true)));
        h.assertTrue(pool.reactionCount()==1&&pool.items.getStackInSlot(0).isEmpty(),"Shared item was consumed twice");
        pool.advance(2000);
        h.assertTrue(pool.items.getStackInSlot(0).is(Items.DIAMOND),"Generic recipe consumed the catalyst recipe's item first");h.succeed();
    }
}

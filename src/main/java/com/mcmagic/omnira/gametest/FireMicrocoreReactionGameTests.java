package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class FireMicrocoreReactionGameTests {
    private static ReactionRecipe recipe(GameTestHelper h){
        return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
                .filter(r->r.id().equals(ResourceLocation.parse("omnira:reaction/water_to_fire_microcore"))).findFirst().orElseThrow().value();
    }
    @GameTest(template="spell_arena") public static void blazePowderConversionWithoutFluid(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var vessel=(RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);var recipe=recipe(h);
        h.assertTrue(recipe.items().size()==2&&recipe.fluidInputs().isEmpty()&&recipe.support().isEmpty()&&recipe.duration()==2000,"Conversion definition changed incorrectly");
        vessel.items.setStackInSlot(0,new ItemStack(ModItems.WATER_MICROCORE.get(),2));
        h.assertTrue(!vessel.start(recipe),"Conversion did not require blaze powder");
        vessel.items.setStackInSlot(1,new ItemStack(Items.BLAZE_POWDER,2));h.assertTrue(vessel.start(recipe),"Dry conversion did not start");vessel.worldRotation();
        int water=0,powder=0,fire=0;for(int i=0;i<4;i++){var stack=vessel.items.getStackInSlot(i);if(stack.is(ModItems.WATER_MICROCORE.get()))water+=stack.getCount();if(stack.is(Items.BLAZE_POWDER))powder+=stack.getCount();if(stack.is(ModItems.FIRE_MICROCORE.get()))fire+=stack.getCount();}
        h.assertTrue(water==1&&powder==1&&fire==1&&vessel.tank.isEmpty(),"Conversion consumption/output incorrect");h.succeed();
    }
    @GameTest(template="spell_arena") public static void energyCoreMaterialsDoNotTriggerConversion(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        level.setBlockAndUpdate(pos.below(),net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("omnira:spiritual_crystal_block")).defaultBlockState());
        var vessel=(RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);var items=new Item[]{ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get()};
        for(int i=0;i<4;i++)vessel.items.setStackInSlot(i,new ItemStack(items[i]));vessel.tank.setFluid(new FluidStack(Fluids.LAVA,4000));
        h.assertTrue(!vessel.start(recipe(h)),"Lava still converted energy-core materials");
        h.assertTrue(vessel.items.getStackInSlot(1).is(ModItems.WATER_MICROCORE.get())&&vessel.tank.getFluidAmount()==4000,"Rejected conversion consumed ingredients");
        AlchemyCauldronGameTests.roughEnergyCoreReaction(h);
    }
}

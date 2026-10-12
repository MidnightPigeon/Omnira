package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.ReactionRecipe;
import com.mcmagic.omnira.alchemy.catalysis.CatalysisRecipe;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;

/** Select this namespace only with -PkubejsRecipeTest. */
@GameTestHolder("omnira_kubejs_recipes")
@PrefixGameTestTemplate(false)
public final class KubeRecipeGameTests {
    @GameTest(template="spell_arena")
    public static void customRecipesReachMachineMatchers(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        var reaction=(ReactionRecipe)manager.byKey(ResourceLocation.parse("omnira_test:kubejs_reaction")).orElseThrow().value();
        var items=new ItemStackHandler(4);items.setStackInSlot(0,new ItemStack(Items.OAK_PLANKS));
        var tank=new FluidTank(4000);tank.setFluid(new FluidStack(Fluids.WATER,1000));
        h.assertTrue(reaction.matches(new ReactionRecipe.Input(items,tank,i->true),h.getLevel()),"KubeJS reaction/tag input rejected");
        h.assertTrue(reaction.getResultItem(h.getLevel().registryAccess()).is(Items.STICK),"KubeJS reaction output changed");
        var catalysis=(CatalysisRecipe)manager.byKey(ResourceLocation.parse("omnira_test:kubejs_catalysis")).orElseThrow().value();
        h.assertTrue(catalysis.matches(new CatalysisRecipe.Input(new ItemStack(Items.DIRT),new ItemStack(Items.WHEAT_SEEDS),
                new FluidStack(Fluids.WATER,500)),h.getLevel()),"KubeJS catalysis/tag input rejected");
        var assembly=(AssemblyRecipe)manager.byKey(ResourceLocation.parse("omnira_test:kubejs_assembly")).orElseThrow().value();
        var slots=new SimpleContainer(7);slots.setItem(6,new ItemStack(Items.STICK));
        h.assertTrue(assembly.matches(new AssemblyRecipe.Input(slots),h.getLevel()),"KubeJS empty assembly slots rejected");
        h.assertTrue(assembly.assemble(new AssemblyRecipe.Input(slots),h.getLevel().registryAccess()).is(Items.DIAMOND),"KubeJS assembly output changed");
        h.succeed();
    }
    private KubeRecipeGameTests() {}
}

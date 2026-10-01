package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.reversal.ReversalRecipe;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_time_microcore_reversal")
@PrefixGameTestTemplate(false)
public final class TimeMicrocoreReversalGameTests {
    @GameTest(template="spell_arena")
    public static void twoPointsForOneCore(GameTestHelper h){
        var recipe=(ReversalRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:spacetime_reversal/time_microcore")).orElseThrow().value();
        h.assertTrue(recipe.count()==2,"Reversal cost must be two");
        h.assertTrue(!recipe.matches(new SingleRecipeInput(new ItemStack(ModItems.TIME_WARP_POINT.get())),h.getLevel()),"One point accepted");
        var input=new SingleRecipeInput(new ItemStack(ModItems.TIME_WARP_POINT.get(),2));
        h.assertTrue(recipe.matches(input,h.getLevel()),"Two points rejected");
        var output=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(output.is(ModItems.TIME_MICROCORE.get())&&output.getCount()==1&&recipe.results().size()==1,"Wrong output");
        h.succeed();
    }
}

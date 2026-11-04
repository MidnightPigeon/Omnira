package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.forging.AdvancedForgeRecipe;
import com.mcmagic.omnira.recipe.AnalysisRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spacetime_core_recipe")
@PrefixGameTestTemplate(false)
public final class SpacetimeCoreRecipeGameTests {
    private static ItemStack item(String id,int count){
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:"+id)),count);
    }
    private static Recipe<?> recipe(GameTestHelper h,String id){
        return h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:"+id)).orElseThrow().value();
    }
    @GameTest(template="spell_arena")
    public static void coreAssembly(GameTestHelper h){
        var recipe=(AdvancedForgeRecipe)recipe(h,"advanced_assembly_table/spacetime_spell_core");
        var inventory=new SimpleContainer(9);
        for(int i=0;i<6;i++)inventory.setItem(i,item(i%2==0?"time_warp_point":"spatial_crystal_shard",1));
        inventory.setItem(6,item("time_microcore",1));inventory.setItem(7,item("light_dark_spell_core",1));inventory.setItem(8,item("space_microcore",1));
        var input=new AdvancedForgeRecipe.Input(inventory);
        h.assertTrue(recipe.matches(input,h.getLevel()),"Shapeless inner materials rejected");
        var result=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(item("spacetime_spell_core",1).getItem())&&result.getCount()==1,"Wrong core output");
        inventory.setItem(7,item("dream_spell_core",1));
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Dream core accepted");
        inventory.setItem(7,item("light_dark_spell_core",1));inventory.setItem(0,ItemStack.EMPTY);
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Missing warp point accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void forgeConsumesAnalysisCrystals(GameTestHelper h){
        var recipe=(AdvancedForgeRecipe)recipe(h,"advanced_assembly_table/crystallized_philosophers_stone");
        var inventory=new SimpleContainer(9);
        for(int slot=0;slot<6;slot++)inventory.setItem(slot,item("analysis_crystal",1));
        inventory.setItem(6,item("time_microcore",1));inventory.setItem(7,item("light_dark_spell_core",1));inventory.setItem(8,item("space_microcore",1));
        var input=new AdvancedForgeRecipe.Input(inventory);
        h.assertTrue(recipe.matches(input,h.getLevel())&&recipe.assemble(input,h.getLevel().registryAccess()).is(item("crystallized_philosophers_stone",1).getItem()),"Incorrect stone assembly");
        h.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"Six crystals must be fully consumed");
        inventory.setItem(0,item("crystallized_philosophers_stone",1));
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Stone accepted in place of whole crystal");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void analysisRoutes(GameTestHelper h){
        for(String kind:java.util.List.of("time","space"))checkAnalysis(h,kind);
        h.succeed();
    }
    private static void checkAnalysis(GameTestHelper h,String kind){
        var recipe=(AnalysisRecipe)recipe(h,"analysis/"+kind);
        h.assertTrue(!recipe.matches(new SingleRecipeInput(item(kind+"_warp_point",3)),h.getLevel()),"Three points accepted");
        var input=new SingleRecipeInput(item(kind+"_warp_point",4));
        h.assertTrue(recipe.matches(input,h.getLevel()),"Four points rejected");
        var output=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(output.is(item(kind+"_microcore",1).getItem())&&output.getCount()==1,"Wrong analysis output");
        for(int i=0;i<32;i++){
            var dust=recipe.secondaryOutput().roll(h.getLevel().random);
            h.assertTrue(dust.is(item("arcane_dust",1).getItem())&&dust.getCount()==16,"Dust must be exactly sixteen");
        }
        var crystal=item("analysis_crystal",1);crystal.setDamageValue(2);
        var crafting=CraftingInput.of(2,2,java.util.List.of(item(kind+"_warp_point",1),crystal,item(kind+"_warp_point",1),item(kind+"_warp_point",1)));
        var conversion=(CraftingRecipe)recipe(h,"crystal_analysis/"+kind);
        h.assertTrue(conversion.matches(crafting,h.getLevel()),"Three-point crystal recipe rejected");
        var core=conversion.assemble(crafting,h.getLevel().registryAccess());
        h.assertTrue(core.is(item(kind+"_microcore",1).getItem())&&core.getCount()==1,"Crystal must produce one core");
        var remainder=conversion.getRemainingItems(crafting);
        h.assertTrue(remainder.get(0).isEmpty()&&remainder.get(2).isEmpty()&&remainder.get(3).isEmpty()&&remainder.get(1).getDamageValue()==3,"Unexpected dust or catalyst wear");
        h.assertTrue(!conversion.matches(CraftingInput.of(3,1,java.util.List.of(crystal,item(kind+"_warp_point",1),item(kind+"_warp_point",1))),h.getLevel()),"Two points accepted");
        var reverse=(CraftingRecipe)recipe(h,"crystal_analysis/"+kind+"_reverse");
        var reverseInput=CraftingInput.of(2,1,java.util.List.of(core,crystal));
        h.assertTrue(reverse.matches(reverseInput,h.getLevel()),"Reverse recipe rejected "+kind);
        var points=reverse.assemble(reverseInput,h.getLevel().registryAccess());
        h.assertTrue(points.is(item(kind+"_warp_point",1).getItem())&&points.getCount()==4,"Wrong reverse output");
        h.assertTrue(reverse.getRemainingItems(reverseInput).get(1).getDamageValue()==3,"Wrong reverse crystal wear");
    }
}

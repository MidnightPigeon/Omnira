package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_analysis_crystal")
@PrefixGameTestTemplate(false)
public final class AnalysisCrystalGameTests {
    private static CraftingRecipe recipe(GameTestHelper h,String name) {
        return (CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira",name)).orElseThrow().value();
    }

    @GameTest(template="spell_arena")
    public static void conversionsAndDurability(GameTestHelper h) {
        String[] elements={"earth","water","air","fire"};
        Item[] inputs={Items.COARSE_DIRT,Items.SALMON,Items.FEATHER,Items.CHARCOAL};
        Item[] outputs={Items.DIRT,Items.COD,Items.FEATHER,Items.COAL};
        Item[] cores={ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.AIR_MICROCORE.get(),ModItems.FIRE_MICROCORE.get()};
        for(int e=0;e<4;e++) {
            var slots=NonNullList.withSize(9,ItemStack.EMPTY);
            for(int i=0;i<9;i++) slots.set(i,new ItemStack(inputs[e]));
            var crystal=new ItemStack(ModItems.ANALYSIS_CRYSTAL.get());crystal.setDamageValue(2);slots.set(4,crystal);
            var input=CraftingInput.of(3,3,slots);
            var forward=recipe(h,"crystal_analysis/"+elements[e]);
            h.assertTrue(forward.matches(input,h.getLevel()),"Tagged forward recipe failed");
            h.assertTrue(forward.assemble(input,h.getLevel().registryAccess()).is(cores[e]),"Wrong microcore");
            h.assertTrue(forward.getRemainingItems(input).get(4).getDamageValue()==3 && crystal.getDamageValue()==2,"Catalyst wear incorrect");
            slots.set(0,ItemStack.EMPTY);
            h.assertTrue(!forward.matches(CraftingInput.of(3,3,slots),h.getLevel()),"Seven ingredients accepted");
            var reverse=recipe(h,"crystal_analysis/"+elements[e]+"_reverse");
            var reverseInput=CraftingInput.of(2,1,java.util.List.of(new ItemStack(cores[e]),crystal));
            h.assertTrue(reverse.matches(reverseInput,h.getLevel()),"Reverse recipe failed");
            var result=reverse.assemble(reverseInput,h.getLevel().registryAccess());
            h.assertTrue(result.is(outputs[e]) && result.getCount()==16,"Reverse output is not fixed sixteen");
            h.assertTrue(reverse.getRemainingItems(reverseInput).get(1).getDamageValue()==3,"Reverse catalyst wear incorrect");
        }
        var crystal=new ItemStack(ModItems.ANALYSIS_CRYSTAL.get());
        for(int i=0;i<7;i++) crystal=crystal.getCraftingRemainingItem();
        h.assertTrue(crystal.getDamageValue()==7&&crystal.getMaxDamage()==8,"Incorrect lifespan");
        h.assertTrue(crystal.getCraftingRemainingItem().isEmpty(),"Final use did not break crystal");
        var creation=CraftingInput.of(2,2,java.util.Arrays.stream(cores).map(ItemStack::new).toList());
        h.assertTrue(recipe(h,"analysis_crystal").matches(creation,h.getLevel()),"Four-core recipe failed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void reusableStoneInAllConversions(GameTestHelper h){
        var stone=ModItems.PHILOSOPHERS_STONE.get().getDefaultInstance();
        stone.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Kept"));
        var level=h.getLevel();int tested=0;
        for(var holder:level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)){
            if(!holder.id().getNamespace().equals("omnira")||!holder.id().getPath().startsWith("crystal_analysis/"))continue;
            var recipe=holder.value();var slots=NonNullList.withSize(9,ItemStack.EMPTY);int index=0,toolSlot=-1;
            for(var ingredient:recipe.getIngredients()){
                if(ingredient.test(ModItems.ANALYSIS_CRYSTAL.get().getDefaultInstance())){
                    toolSlot=index;slots.set(index++,stone.copy());
                }else slots.set(index++,ingredient.getItems()[0].copy());
            }
            var input=CraftingInput.of(3,3,slots);
            h.assertTrue(recipe.matches(input,level)&&toolSlot>=0,"Stone cannot convert "+holder.id());
            h.assertTrue(ItemStack.matches(stone,recipe.getRemainingItems(input).get(toolSlot)),"Stone remainder changed");
            tested++;
        }
        h.assertTrue(tested>0,"No analysis conversions tested");h.succeed();
    }
}

package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_milk_crafting")
@PrefixGameTestTemplate(false)
public final class MilkCraftingGameTests {
    private static CraftingRecipe recipe(GameTestHelper h,String id){return (CraftingRecipe)h.getLevel().getRecipeManager()
            .byKey(ResourceLocation.fromNamespaceAndPath("omnira",id)).orElseThrow().value();}
    private static ArrayList<ItemStack> empty(int size){return new ArrayList<>(Collections.nCopies(size*size,ItemStack.EMPTY));}
    private static ItemStack suspension(){return new ItemStack(ModItems.MILK_SUSPENSION.get());}
    @GameTest(template="spell_arena") public static void suspensionShapelessPermutationsAndRemainders(GameTestHelper h){
        var recipe=recipe(h,"milk_suspension");h.assertTrue(recipe instanceof ShapelessRecipe,"Not vanilla shapeless crafting");
        for(int size:new int[]{2,3})for(int milk=0;milk<size*size;milk++)for(int crystal=0;crystal<size*size;crystal++)if(milk!=crystal){
            var stacks=empty(size);stacks.set(milk,new ItemStack(Items.MILK_BUCKET));stacks.set(crystal,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2));
            var input=CraftingInput.of(size,size,stacks);
            var selected=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
            h.assertTrue(selected.id().equals(ResourceLocation.parse("omnira:milk_suspension"))&&recipe.matches(input,h.getLevel()),"Shapeless position not accepted");
            var result=recipe.assemble(input,h.getLevel().registryAccess());var remainders=recipe.getRemainingItems(input);
            h.assertTrue(result.is(ModItems.MILK_SUSPENSION.get())&&result.getCount()==1,"Incorrect suspension output");
            for(int i=0;i<remainders.size();i++)h.assertTrue(input.getItem(i).is(Items.MILK_BUCKET)?remainders.get(i).is(Items.BUCKET)&&remainders.get(i).getCount()==1:remainders.get(i).isEmpty(),"Remainder moved or duplicated bucket");
            stacks.set(crystal,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));h.assertTrue(!recipe.matches(CraftingInput.of(size,size,stacks),h.getLevel()),"Accepted uninfused crystal");
        }
        var stacks=empty(3);stacks.set(0,new ItemStack(Items.MILK_BUCKET));stacks.set(1,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));stacks.set(2,new ItemStack(Items.STONE));
        h.assertTrue(!recipe.matches(CraftingInput.of(3,3,stacks),h.getLevel()),"Ignored extra ingredient");h.succeed();
    }
    @GameTest(template="spell_arena") public static void nativePickupLeavesBucketInOriginalSlot(GameTestHelper h){
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"milk-crafting"));var recipe=recipe(h,"milk_suspension");
        for(int milk=0;milk<9;milk++){
            int crystal=(milk+1)%9;var grid=new TransientCraftingContainer(player.inventoryMenu,3,3);
            grid.setItem(milk,new ItemStack(Items.MILK_BUCKET));grid.setItem(crystal,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2));
            var output=new ResultContainer();output.setItem(0,recipe.assemble(grid.asCraftInput(),h.getLevel().registryAccess()));
            var slot=new ResultSlot(player,grid,output,0,0,0);var taken=slot.remove(1);slot.onTake(player,taken);
            h.assertTrue(taken.is(ModItems.MILK_SUSPENSION.get())&&taken.getCount()==1,"Native pickup lost suspension");
            for(int i=0;i<9;i++)h.assertTrue(i==milk?grid.getItem(i).is(Items.BUCKET)&&grid.getItem(i).getCount()==1:
                    i==crystal?grid.getItem(i).is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())&&grid.getItem(i).getCount()==1:grid.getItem(i).isEmpty(),"Native pickup moved bucket or consumed wrong quantity");
            h.assertTrue(player.getInventory().isEmpty(),"Remainder entered player inventory instead of original slot");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void cakeAcceptsAllEightMilkCombinationsWithNativeRemainders(GameTestHelper h){
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("minecraft:cake")).orElseThrow().value();h.assertTrue(recipe instanceof ShapedRecipe,"Not vanilla shaped crafting");
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"cake-crafting"));
        for(int mask=0;mask<8;mask++){
            var grid=new TransientCraftingContainer(player.inventoryMenu,3,3);
            for(int i=0;i<3;i++)grid.setItem(i,(mask&(1<<i))==0?new ItemStack(Items.MILK_BUCKET):suspension());
            grid.setItem(3,new ItemStack(Items.SUGAR));grid.setItem(4,new ItemStack(Items.EGG));grid.setItem(5,new ItemStack(Items.SUGAR));for(int i=6;i<9;i++)grid.setItem(i,new ItemStack(Items.WHEAT));
            var input=grid.asCraftInput();h.assertTrue(recipe.matches(input,h.getLevel()),"Rejected milk mask "+mask);
            var selected=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();var result=selected.value().assemble(input,h.getLevel().registryAccess());
            h.assertTrue(result.is(Items.CAKE)&&result.getCount()==1,"Incorrect cake output");
            var output=new ResultContainer();output.setItem(0,result);var slot=new ResultSlot(player,grid,output,0,0,0);slot.onTake(player,slot.remove(1));
            for(int i=0;i<9;i++)h.assertTrue(i<3&&(mask&(1<<i))==0?grid.getItem(i).is(Items.BUCKET)&&grid.getItem(i).getCount()==1:grid.getItem(i).isEmpty(),"Cake returned a suspension bucket or lost native bucket");
        }
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("minecraft:cake")).isPresent(),"Vanilla recipe was replaced");h.succeed();
    }
    @GameTest(template="spell_arena") public static void milkExpansionIsScopedIdempotentAndNetworked(GameTestHelper h){
        var milk=Ingredient.of(Items.MILK_BUCKET);var ingredients=net.minecraft.core.NonNullList.of(Ingredient.EMPTY,milk,Ingredient.EMPTY,Ingredient.of(Items.SUGAR));
        com.mcmagic.omnira.recipe.MilkCraftingIngredients.expand(ingredients);
        h.assertTrue(ingredients.getFirst().test(suspension())&&!milk.test(suspension()),"Changed shared original ingredient");
        h.assertTrue(ingredients.get(1)==Ingredient.EMPTY&&!ingredients.get(2).test(suspension()),"Expanded unrelated or empty slot");
        var expanded=ingredients.getFirst();com.mcmagic.omnira.recipe.MilkCraftingIngredients.expand(ingredients);
        h.assertTrue(ingredients.getFirst()==expanded,"Expansion is not idempotent");
        var json=Ingredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,expanded).getOrThrow();
        h.assertTrue(Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow().test(suspension()),"JSON ingredient lost alternative");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,expanded);var synced=Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            h.assertTrue(synced.test(suspension())&&synced.test(new ItemStack(Items.MILK_BUCKET))&&synced.getItems().length==2,"Recipe sync lost alternatives");
        } finally {buffer.release();}
        var stacks=empty(2);stacks.set(0,suspension());stacks.set(1,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(!recipe(h,"milk_suspension").matches(CraftingInput.of(2,2,stacks),h.getLevel()),"Suspension can craft itself");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:cake_from_milk_suspension")).isEmpty(),"Duplicate cake recipe remains");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void otherModWorkbenchRecipesAndShapelessMatching(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();
        var create=manager.byKey(ResourceLocation.parse("create:crafting/curiosities/cake"));
        if(net.neoforged.fml.ModList.get().isLoaded("create")){
            h.assertTrue(create.isPresent(),"Installed Create cake recipe missing");
            var recipe=(CraftingRecipe)create.orElseThrow().value();
            h.assertTrue(recipe.getIngredients().stream().anyMatch(i->i.test(suspension())),"Create workbench milk not expanded");
            var stacks=empty(3);stacks.set(1,suspension());
            for(int i=3;i<6;i++)stacks.set(i,new ItemStack(i==4?Items.EGG:Items.SUGAR));
            stacks.set(7,recipe.getIngredients().get(7).getItems()[0].copy());
            h.assertTrue(recipe.matches(CraftingInput.of(3,3,stacks),h.getLevel()),"Create workbench recipe rejects suspension");
        }
        var ingredients=net.minecraft.core.NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.MILK_BUCKET),Ingredient.of(Items.SUGAR));
        var recipe=new ShapelessRecipe("",CraftingBookCategory.MISC,new ItemStack(Items.PAPER),ingredients);
        com.mcmagic.omnira.recipe.MilkCraftingIngredients.expand(ingredients);
        h.assertTrue(recipe.matches(CraftingInput.of(2,2,List.of(suspension(),new ItemStack(Items.SUGAR),ItemStack.EMPTY,ItemStack.EMPTY)),h.getLevel()),"Shapeless cached matcher lost alternative");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void existingMilkBucketRecipeRetainedAndNoCreateRecipesAdded(GameTestHelper h){
        var recipe=recipe(h,"milk_bucket");var input=CraftingInput.of(2,2,List.of(suspension(),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.BUCKET)));
        h.assertTrue(recipe.matches(input,h.getLevel())&&recipe.assemble(input,h.getLevel().registryAccess()).is(Items.MILK_BUCKET)
                &&recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"Existing milk-bucket crafting changed");
        for(String type:List.of("crafting","deploying"))h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:compat/create/"+type+"/cake_from_milk_suspension")).isEmpty(),"Canceled Create recipe still loaded");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void taggedMilkAndProcessingBoundaries(GameTestHelper h){
        var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("c:buckets/milk"));
        var original=Ingredient.of(tag);
        h.assertTrue(original.test(new ItemStack(Items.MILK_BUCKET)),"Milk tag is missing");
        var ingredients=net.minecraft.core.NonNullList.of(Ingredient.EMPTY,original);
        com.mcmagic.omnira.recipe.MilkCraftingIngredients.expand(ingredients);
        h.assertTrue(ingredients.getFirst().test(suspension())&&!original.test(suspension()),"Tag substitution changed global bucket classification");
        for(var holder:h.getLevel().getRecipeManager().getRecipes()) {
            var recipe=holder.value();
            if(recipe.getType()==RecipeType.CRAFTING)continue;
            for(var ingredient:recipe.getIngredients())if(ingredient.isSimple()&&ingredient.test(new ItemStack(Items.MILK_BUCKET)))
                h.assertTrue(!ingredient.test(suspension()),"Non-workbench milk ingredient expanded: "+holder.id());
        }
        h.succeed();
    }
}

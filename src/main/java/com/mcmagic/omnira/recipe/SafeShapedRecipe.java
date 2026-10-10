package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record SafeShapedRecipe(ShapedRecipe shaped) implements CraftingRecipe {
    private static final ShapedRecipe.Serializer SHAPED=new ShapedRecipe.Serializer();
    public static final MapCodec<SafeShapedRecipe> CODEC=SHAPED.codec().xmap(SafeShapedRecipe::new,SafeShapedRecipe::shaped);
    public static final StreamCodec<RegistryFriendlyByteBuf,SafeShapedRecipe> STREAM=SHAPED.streamCodec().map(SafeShapedRecipe::new,SafeShapedRecipe::shaped);
    public boolean matches(CraftingInput input,Level level){
        if(!shaped.matches(input,level))return false;
        for(int i=0;i<input.size();i++)if(!RecipeInputSafety.empty(input.getItem(i)))return false;
        return true;
    }
    public ItemStack assemble(CraftingInput input,HolderLookup.Provider provider){
        var result=shaped.assemble(input,provider);
        if(result.is(ModItems.INFUSED_CRYSTAL_PICKAXE.get()))
            for(int i=0;i<input.size();i++)if(input.getItem(i).is(ModItems.CRYSTAL_PICKAXE.get()))
                return RecipeInputSafety.upgradePickaxe(input.getItem(i),result);
        return result;
    }
    public ItemStack getResultItem(HolderLookup.Provider provider){return shaped.getResultItem(provider);}
    public boolean canCraftInDimensions(int w,int h){return shaped.canCraftInDimensions(w,h);}
    public CraftingBookCategory category(){return shaped.category();}
    public NonNullList<Ingredient> getIngredients(){return shaped.getIngredients();}
    public String getGroup(){return shaped.getGroup();}
    public RecipeSerializer<?> getSerializer(){return ModRecipes.SAFE_SHAPED_SERIALIZER.get();}
}

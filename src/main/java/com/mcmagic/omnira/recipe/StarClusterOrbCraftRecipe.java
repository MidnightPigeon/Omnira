package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Keep the normal shaped recipe, but never silently discard an upgraded/filled source orb. */
public record StarClusterOrbCraftRecipe(ShapedRecipe shaped) implements CraftingRecipe {
    private static final ShapedRecipe.Serializer SHAPED=new ShapedRecipe.Serializer();
    public static final MapCodec<StarClusterOrbCraftRecipe> CODEC=SHAPED.codec().xmap(StarClusterOrbCraftRecipe::new,StarClusterOrbCraftRecipe::shaped);
    public static final StreamCodec<RegistryFriendlyByteBuf,StarClusterOrbCraftRecipe> STREAM=SHAPED.streamCodec().map(StarClusterOrbCraftRecipe::new,StarClusterOrbCraftRecipe::shaped);
    public static boolean emptyOrb(ItemStack stack){return RecipeInputSafety.empty(stack);}
    public boolean matches(CraftingInput input,Level level){
        if(!shaped.matches(input,level))return false;
        for(int i=0;i<input.size();i++)if(input.getItem(i).is(com.mcmagic.omnira.registry.ModItems.CRYSTAL_BALL.get())&&!emptyOrb(input.getItem(i)))return false;
        return true;
    }
    public ItemStack assemble(CraftingInput input,HolderLookup.Provider provider){return shaped.assemble(input,provider);}
    public ItemStack getResultItem(HolderLookup.Provider provider){return shaped.getResultItem(provider);}
    public boolean canCraftInDimensions(int w,int h){return shaped.canCraftInDimensions(w,h);}
    public CraftingBookCategory category(){return shaped.category();}
    public NonNullList<Ingredient> getIngredients(){return shaped.getIngredients();}
    public String getGroup(){return shaped.getGroup();}
    public RecipeSerializer<?> getSerializer(){return ModRecipes.STELLAR_ORB_CRAFT_SERIALIZER.get();}
}

package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record StarlightConversionRecipe(Ingredient ingredient,ItemStack result) implements Recipe<SingleRecipeInput> {
    public StarlightConversionRecipe{result=result.copy();if(result.isEmpty()||result.getCount()!=1)throw new IllegalArgumentException("Starlight conversion is one-to-one");}
    public static final MapCodec<StarlightConversionRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(StarlightConversionRecipe::ingredient),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(StarlightConversionRecipe::result)).apply(i,StarlightConversionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,StarlightConversionRecipe> STREAM=StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC,StarlightConversionRecipe::ingredient,ItemStack.STREAM_CODEC,StarlightConversionRecipe::result,StarlightConversionRecipe::new);
    public boolean matches(SingleRecipeInput input,Level level){return ingredient.test(input.item())&&!input.item().is(result.getItem());}
    public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries){return result.copyWithCount(input.item().getCount());}
    public ItemStack getResultItem(HolderLookup.Provider registries){return result.copy();}
    public boolean canCraftInDimensions(int width,int height){return width*height>=1;}
    public RecipeType<?> getType(){return ModRecipes.STARLIGHT_TYPE.get();}
    public RecipeSerializer<?> getSerializer(){return ModRecipes.STARLIGHT_SERIALIZER.get();}
    public static ItemStack output(Level level,ItemStack input){
        return level==null||input.isEmpty()?ItemStack.EMPTY:level.getRecipeManager().getRecipeFor(ModRecipes.STARLIGHT_TYPE.get(),new SingleRecipeInput(input),level)
            .map(r->r.value().assemble(new SingleRecipeInput(input),level.registryAccess())).orElse(ItemStack.EMPTY);
    }
}

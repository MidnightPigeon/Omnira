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

public record VesselConversionRecipe(Ingredient ingredient,ItemStack result) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<VesselConversionRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(VesselConversionRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(VesselConversionRecipe::result)).apply(i,VesselConversionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,VesselConversionRecipe> STREAM=StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,VesselConversionRecipe::ingredient,ItemStack.STREAM_CODEC,VesselConversionRecipe::result,VesselConversionRecipe::new);
    @Override public boolean matches(SingleRecipeInput input,Level level) {return ingredient.test(input.item());}
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries) {return result.copy();}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {return result;}
    @Override public boolean canCraftInDimensions(int width,int height) {return width*height>=1;}
    @Override public RecipeType<?> getType() {return ModRecipes.VESSEL_CONVERSION_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer() {return ModRecipes.VESSEL_CONVERSION_SERIALIZER.get();}
}

package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record CondensationRecipe(int manaCost,ItemStack result,Ingredient ingredient) implements Recipe<SingleRecipeInput> {
    public CondensationRecipe(int manaCost,ItemStack result) {this(manaCost,result,Ingredient.EMPTY);}
    public boolean advanced() {return !ingredient.isEmpty();}
    public static final MapCodec<CondensationRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("mana_cost").forGetter(CondensationRecipe::manaCost),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CondensationRecipe::result),
            Ingredient.CODEC.optionalFieldOf("ingredient",Ingredient.EMPTY).forGetter(CondensationRecipe::ingredient)).apply(i,CondensationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,CondensationRecipe> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,CondensationRecipe::manaCost,ItemStack.STREAM_CODEC,CondensationRecipe::result,Ingredient.CONTENTS_STREAM_CODEC,CondensationRecipe::ingredient,CondensationRecipe::new);
    @Override public boolean matches(SingleRecipeInput input,Level level) {return advanced()?ingredient.test(input.item()):input.item().isEmpty();}
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries) {
        if(result.is(com.mcmagic.omnira.registry.ModItems.ENHANCED_RESONANCE_TERMINAL.get())
                &&input.item().is(com.mcmagic.omnira.registry.ModItems.RESONANCE_TERMINAL.get()))
            return input.item().transmuteCopy(result.getItem(),result.getCount());
        return result.copy();
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {return result.copy();}
    @Override public boolean canCraftInDimensions(int width,int height) {return true;}
    @Override public boolean isSpecial() {return true;}
    @Override public RecipeType<?> getType() {return ModRecipes.CONDENSATION_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer() {return ModRecipes.CONDENSATION_SERIALIZER.get();}
}

package com.mcmagic.omnira.reversal;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.util.List;

public record ReversalRecipe(Ingredient ingredient,int count,List<ItemStack> results) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<ReversalRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ReversalRecipe::ingredient),Codec.intRange(1,64).fieldOf("count").forGetter(ReversalRecipe::count),ItemStack.STRICT_CODEC.listOf().validate(v->v.isEmpty()?DataResult.error(()->"Empty reversal results"):DataResult.success(v)).fieldOf("results").forGetter(ReversalRecipe::results)).apply(i,ReversalRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,ReversalRecipe> STREAM=StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC,ReversalRecipe::ingredient,ByteBufCodecs.VAR_INT,ReversalRecipe::count,ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),ReversalRecipe::results,ReversalRecipe::new);
    public boolean matches(SingleRecipeInput input,net.minecraft.world.level.Level level){return ingredient.test(input.item())&&input.item().getCount()>=count;}
    public List<ItemStack> outputs(ItemStack input){
        if(input.is(com.mcmagic.omnira.registry.ModItems.MEMORY_CUBE.get()))return List.of(com.mcmagic.omnira.item.MemoryCubeBlockItem.withState(input.copyWithCount(1),!com.mcmagic.omnira.item.MemoryCubeBlockItem.peaceful(input)));
        return results;
    }
    public ItemStack assemble(SingleRecipeInput input,net.minecraft.core.HolderLookup.Provider registries){return results.getFirst().copy();}
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries){return results.getFirst().copy();}
    public boolean canCraftInDimensions(int w,int h){return true;}
    public boolean isSpecial(){return true;}
    public RecipeType<?> getType(){return ReversalContent.TYPE.get();}
    public RecipeSerializer<?> getSerializer(){return ReversalContent.SERIALIZER.get();}
}

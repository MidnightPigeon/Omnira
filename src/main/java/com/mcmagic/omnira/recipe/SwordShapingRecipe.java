package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.block.entity.PureVesselBlockEntity;
import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

public record SwordShapingRecipe(List<Ingredient> offerings,ItemStack result) implements Recipe<SwordShapingRecipe.Input> {
    public SwordShapingRecipe {
        offerings=List.copyOf(offerings);
        if(offerings.size()!=4 || offerings.stream().anyMatch(Ingredient::isEmpty) || result.getCount()!=1)
            throw new IllegalArgumentException("Sword shaping requires four offerings and one result");
    }
    public record Input(ItemStack weapon,List<ItemStack> offerings) implements RecipeInput {
        @Override public ItemStack getItem(int slot) {return slot==0?weapon:offerings.get(slot-1);}
        @Override public int size() {return 5;}
    }
    public static final MapCodec<SwordShapingRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Ingredient.CODEC.listOf().fieldOf("offerings").forGetter(SwordShapingRecipe::offerings),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SwordShapingRecipe::result)).apply(i,SwordShapingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,SwordShapingRecipe> STREAM_CODEC=new StreamCodec<>() {
        @Override public SwordShapingRecipe decode(RegistryFriendlyByteBuf b) {
            var inputs=new java.util.ArrayList<Ingredient>();for(int i=0;i<4;i++)inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b));
            return new SwordShapingRecipe(inputs,ItemStack.STREAM_CODEC.decode(b));
        }
        @Override public void encode(RegistryFriendlyByteBuf b,SwordShapingRecipe r) {
            for(var input:r.offerings)Ingredient.CONTENTS_STREAM_CODEC.encode(b,input);ItemStack.STREAM_CODEC.encode(b,r.result);
        }
    };
    @Override public boolean matches(Input input,Level level) {return input.offerings.size()==4 && input.weapon.getCount()==1 && input.weapon.is(PureVesselBlockEntity.WEAPONS) && match(input,0,0);}
    private boolean match(Input input,int slot,int used) {
        if(slot==4)return true;
        for(int i=0;i<4;i++)if((used&(1<<i))==0 && offerings.get(i).test(input.offerings.get(slot)) && match(input,slot+1,used|(1<<i)))return true;
        return false;
    }
    @Override public ItemStack assemble(Input input,HolderLookup.Provider registries) {return result.copy();}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {return result.copy();}
    @Override public boolean canCraftInDimensions(int width,int height) {return true;}
    @Override public boolean isSpecial() {return true;}
    @Override public RecipeType<?> getType() {return ModRecipes.SWORD_SHAPING_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer() {return ModRecipes.SWORD_SHAPING_SERIALIZER.get();}
}

package com.mcmagic.omnira.forging;

import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Six shapeless ingredients, then entrance-view left / middle / right nodes. */
public record AdvancedForgeRecipe(List<Ingredient> slots,ItemStack result,String operation) implements Recipe<AdvancedForgeRecipe.Input> {
    public AdvancedForgeRecipe(List<Ingredient> slots,ItemStack result){this(slots,result,"fixed");}
    public AdvancedForgeRecipe {
        slots=List.copyOf(slots);
        if(!operation.equals("fixed") && !operation.equals("staff_enhancement"))throw new IllegalArgumentException("Unknown advanced assembly operation: "+operation);
        if(slots.size()!=9 || slots.stream().allMatch(Ingredient::isEmpty))throw new IllegalArgumentException("Expected nine material positions");
    }
    public record Input(Container container) implements RecipeInput {
        public ItemStack getItem(int slot){return container.getItem(slot);}
        public int size(){return 9;}
    }
    public static final MapCodec<AdvancedForgeRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Ingredient.CODEC.listOf().fieldOf("slots").forGetter(AdvancedForgeRecipe::slots),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AdvancedForgeRecipe::result),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("operation","fixed").forGetter(AdvancedForgeRecipe::operation)).apply(i,AdvancedForgeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,AdvancedForgeRecipe> STREAM_CODEC=new StreamCodec<>() {
        public AdvancedForgeRecipe decode(RegistryFriendlyByteBuf b){var list=new java.util.ArrayList<Ingredient>();for(int i=0;i<9;i++)list.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b));return new AdvancedForgeRecipe(list,ItemStack.STREAM_CODEC.decode(b),b.readUtf());}
        public void encode(RegistryFriendlyByteBuf b,AdvancedForgeRecipe r){for(var ingredient:r.slots)Ingredient.CONTENTS_STREAM_CODEC.encode(b,ingredient);ItemStack.STREAM_CODEC.encode(b,r.result);b.writeUtf(r.operation);}
    };
    public boolean matches(Input input,Level level){
        for(int i=6;i<9;i++)if(slots.get(i).isEmpty()?!input.getItem(i).isEmpty():!slots.get(i).test(input.getItem(i)))return false;
        var materials=new java.util.ArrayList<ItemStack>(6);
        var ingredients=new java.util.ArrayList<Ingredient>(6);
        for(int i=0;i<6;i++){
            if(!input.getItem(i).isEmpty())materials.add(input.getItem(i));
            if(!slots.get(i).isEmpty())ingredients.add(slots.get(i));
        }
        return net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(materials,ingredients)!=null
                && (!operation.equals("staff_enhancement") || !com.mcmagic.omnira.item.staff.StaffEnhancements.assemble(input.container()).isEmpty());
    }
    public ItemStack assemble(Input input,HolderLookup.Provider registries){return operation.equals("staff_enhancement")?
            com.mcmagic.omnira.item.staff.StaffEnhancements.assemble(input.container()):result.copy();}
    public ItemStack displayResult(){
        if(!operation.equals("staff_enhancement"))return result.copy();
        var input=new net.minecraft.world.SimpleContainer(9);
        for(int i=0;i<9;i++)input.setItem(i,slots.get(i).getItems()[0].copyWithCount(1));
        return com.mcmagic.omnira.item.staff.StaffEnhancements.assemble(input);
    }
    public ItemStack getResultItem(HolderLookup.Provider registries){return displayResult();}
    public boolean canCraftInDimensions(int width,int height){return true;}
    public boolean isSpecial(){return true;}
    public ItemStack getToastSymbol(){return new ItemStack(com.mcmagic.omnira.registry.ModItems.ADVANCED_FORGE.get());}
    public RecipeType<?> getType(){return ModRecipes.ADVANCED_FORGE_TYPE.get();}
    public RecipeSerializer<?> getSerializer(){return ModRecipes.ADVANCED_FORGE_SERIALIZER.get();}
}

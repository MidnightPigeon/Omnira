package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record AnalysisRecipe(Ingredient ingredient, int count, ItemStack result, int dustMin, int dustMax, Byproduct byproduct, int inputDamage)
        implements Recipe<SingleRecipeInput> {
    public static final int MANA_COST = 50;
    public AnalysisRecipe(Ingredient ingredient,int count,ItemStack result,int dustMin,int dustMax) {
        this(ingredient,count,result,dustMin,dustMax,Byproduct.EMPTY,0);
    }
    public record Byproduct(java.util.List<ItemStack> choices,int min,int max) {
        public static final Byproduct EMPTY=new Byproduct(java.util.List.of(),0,0);
        public Byproduct {
            choices=choices.stream().map(ItemStack::copy).toList();
            if(choices.size()>64 || min<0 || max<min || max>64 || (choices.isEmpty() && max!=0)
                    || choices.stream().anyMatch(s->s.isEmpty() || s.getCount()!=1 || max>s.getMaxStackSize()))
                throw new IllegalArgumentException("Invalid analysis byproduct");
        }
        public static final Codec<Byproduct> CODEC=RecordCodecBuilder.create(i->i.group(
                ItemStack.STRICT_CODEC.listOf().fieldOf("choices").forGetter(Byproduct::choices),
                Codec.intRange(0,64).optionalFieldOf("min",1).forGetter(Byproduct::min),
                Codec.intRange(0,64).optionalFieldOf("max",1).forGetter(Byproduct::max)).apply(i,Byproduct::new));
        public static final StreamCodec<RegistryFriendlyByteBuf,Byproduct> STREAM_CODEC=new StreamCodec<>() {
            @Override public Byproduct decode(RegistryFriendlyByteBuf b) {
                int size=b.readVarInt();if(size<0 || size>64) throw new IllegalArgumentException("Invalid byproduct pool size");
                var choices=new java.util.ArrayList<ItemStack>();
                for(int n=0;n<size;n++) choices.add(ItemStack.STREAM_CODEC.decode(b));
                return new Byproduct(choices,b.readVarInt(),b.readVarInt());
            }
            @Override public void encode(RegistryFriendlyByteBuf b,Byproduct value) {
                b.writeVarInt(value.choices.size());for(var stack:value.choices) ItemStack.STREAM_CODEC.encode(b,stack);
                b.writeVarInt(value.min);b.writeVarInt(value.max);
            }
        };
        public java.util.List<ItemStack> maximumOutputs() {
            return max==0?java.util.List.of():choices.stream().map(s->s.copyWithCount(max)).toList();
        }
        public java.util.List<ItemStack> displayOutputs() {
            var result=new java.util.ArrayList<ItemStack>();
            for(var choice:choices) for(int n=Math.max(1,min);n<=max;n++) result.add(choice.copyWithCount(n));
            return result;
        }
        public ItemStack roll(net.minecraft.util.RandomSource random) {
            if(choices.isEmpty() || max==0) return ItemStack.EMPTY;
            int amount=min+random.nextInt(max-min+1);
            return amount==0?ItemStack.EMPTY:choices.get(random.nextInt(choices.size())).copyWithCount(amount);
        }
    }
    public Byproduct secondaryOutput() {
        return dustMax>0?new Byproduct(java.util.List.of(new ItemStack(com.mcmagic.omnira.registry.ModItems.ARCANE_DUST.get())),dustMin,dustMax):byproduct;
    }
    public AnalysisRecipe {
        if (count < 1 || count > 64 || dustMin < 0 || dustMax < dustMin || dustMax > 64)
            throw new IllegalArgumentException("Invalid analysis recipe counts");
        if(dustMax>0 && byproduct.max()>0) throw new IllegalArgumentException("Analysis has only one byproduct slot");
        if(inputDamage<0 || inputDamage>65535 || (inputDamage>0 && count!=1)) throw new IllegalArgumentException("Durability analysis requires one input");
    }
    public static final MapCodec<AnalysisRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(AnalysisRecipe::ingredient),
            Codec.intRange(1,64).fieldOf("count").forGetter(AnalysisRecipe::count),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AnalysisRecipe::result),
            Codec.intRange(0,64).optionalFieldOf("dust_min", 0).forGetter(AnalysisRecipe::dustMin),
            Codec.intRange(0,64).optionalFieldOf("dust_max", 0).forGetter(AnalysisRecipe::dustMax),
            Byproduct.CODEC.optionalFieldOf("byproduct",Byproduct.EMPTY).forGetter(AnalysisRecipe::byproduct),
            Codec.intRange(0,65535).optionalFieldOf("input_damage",0).forGetter(AnalysisRecipe::inputDamage)
    ).apply(instance, AnalysisRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnalysisRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override public AnalysisRecipe decode(RegistryFriendlyByteBuf b) {
            return new AnalysisRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(b),b.readVarInt(),ItemStack.STREAM_CODEC.decode(b),
                    b.readVarInt(),b.readVarInt(),Byproduct.STREAM_CODEC.decode(b),b.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf b,AnalysisRecipe r) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(b,r.ingredient);b.writeVarInt(r.count);ItemStack.STREAM_CODEC.encode(b,r.result);
            b.writeVarInt(r.dustMin);b.writeVarInt(r.dustMax);Byproduct.STREAM_CODEC.encode(b,r.byproduct);b.writeVarInt(r.inputDamage);
        }
    };
    @Override public boolean matches(SingleRecipeInput input, Level level) {
        var stack=input.item();
        return ingredient.test(stack) && stack.getCount()>=count && (inputDamage==0 ||
                (stack.getCount()==1 && stack.isDamageableItem() && stack.getMaxDamage()-stack.getDamageValue()>=inputDamage));
    }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
    @Override public RecipeType<?> getType() { return ModRecipes.ANALYSIS_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ANALYSIS_SERIALIZER.get(); }
}

package com.mcmagic.omnira.alchemy.catalysis;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fixed products can be extended or overridden by data packs; common crops derive their real loot. */
public record CatalysisRecipe(Ingredient substrate,Ingredient seed,FluidStack fluid,ItemStack result,int stages) implements Recipe<CatalysisRecipe.Input> {
    public CatalysisRecipe {
        fluid=fluid.copy();result=result.copy();
        if(substrate.isEmpty()||seed.isEmpty()||fluid.isEmpty()||fluid.getAmount()!=500||result.isEmpty()
                ||result.getCount()>result.getMaxStackSize()||stages<2||stages>64)throw new IllegalArgumentException("Invalid catalysis recipe");
    }
    public static final MapCodec<CatalysisRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("substrate").forGetter(CatalysisRecipe::substrate),
            Ingredient.CODEC_NONEMPTY.fieldOf("seed").forGetter(CatalysisRecipe::seed),
            FluidStack.CODEC.fieldOf("fluid").forGetter(CatalysisRecipe::fluid),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CatalysisRecipe::result),
            Codec.intRange(2,64).optionalFieldOf("stages",4).forGetter(CatalysisRecipe::stages)).apply(i,CatalysisRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,CatalysisRecipe> STREAM=new StreamCodec<>(){
        public CatalysisRecipe decode(RegistryFriendlyByteBuf b){return CODEC.codec().parse(b.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),b.readNbt()).getOrThrow();}
        public void encode(RegistryFriendlyByteBuf b,CatalysisRecipe r){b.writeNbt((net.minecraft.nbt.CompoundTag)CODEC.codec().encodeStart(b.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),r).getOrThrow());}
    };
    public record Input(ItemStack substrate,ItemStack seed,FluidStack fluid) implements RecipeInput {
        public ItemStack getItem(int slot){return slot==0?substrate:seed;}public int size(){return 2;}
    }
    public boolean matches(Input input,Level level){return substrate.test(input.substrate)&&seed.test(input.seed)&&FluidStack.isSameFluidSameComponents(fluid,input.fluid);}
    public ItemStack assemble(Input input,HolderLookup.Provider provider){return result.copy();}
    public ItemStack getResultItem(HolderLookup.Provider provider){return result.copy();}
    public boolean canCraftInDimensions(int x,int y){return true;}
    public boolean isSpecial(){return true;}
    public RecipeType<?> getType(){return CatalysisContent.TYPE.get();}
    public RecipeSerializer<?> getSerializer(){return CatalysisContent.SERIALIZER.get();}
}

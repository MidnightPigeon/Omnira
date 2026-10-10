package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public record ReactionRecipe(List<InputItem> items, FluidStack fluid, Ingredient support,
                             List<ItemStack> results, FluidStack fluidResult, int duration, List<FluidStack> additionalFluids, List<ItemStack> guaranteedResults)
        implements Recipe<ReactionRecipe.Input> {
    public record InputItem(Ingredient ingredient,int count) {
        public static final Codec<InputItem> CODEC=RecordCodecBuilder.create(i->i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(InputItem::ingredient),
                Codec.intRange(1,64).optionalFieldOf("count",1).forGetter(InputItem::count)).apply(i,InputItem::new));
    }
    public ReactionRecipe {
        items=List.copyOf(items);fluid=fluid.copy();fluidResult=fluidResult.copy();
        additionalFluids=additionalFluids.stream().map(FluidStack::copy).toList();
        results=results.stream().map(ItemStack::copy).toList();
        guaranteedResults=guaranteedResults.stream().map(ItemStack::copy).toList();
        var inputs=new java.util.ArrayList<FluidStack>();if(!fluid.isEmpty())inputs.add(fluid);inputs.addAll(additionalFluids);
        if(items.size()>4 || (items.isEmpty()&&inputs.isEmpty()&&support.isEmpty()) || duration<1 || duration>72000
                || results.size()>64 || guaranteedResults.size()>9 || (results.isEmpty()&&guaranteedResults.isEmpty()&&fluidResult.isEmpty())
                || inputs.size()>4 || inputs.stream().anyMatch(FluidStack::isEmpty)
                || inputs.stream().mapToLong(FluidStack::getAmount).sum()>40000 || fluidResult.getAmount()>40000
                || items.stream().anyMatch(i->i.ingredient.isEmpty()||i.count<1||i.count>64)
                || java.util.stream.Stream.concat(results.stream(),guaranteedResults.stream()).anyMatch(s->s.isEmpty()||s.getCount()>s.getMaxStackSize()))
            throw new IllegalArgumentException("Invalid reaction inputs, outputs or duration");
        for(int a=0;a<inputs.size();a++)for(int b=a+1;b<inputs.size();b++)
            if(FluidStack.isSameFluidSameComponents(inputs.get(a),inputs.get(b)))throw new IllegalArgumentException("Duplicate reaction fluid input");
    }
    public ReactionRecipe(List<InputItem> items,FluidStack fluid,Ingredient support,List<ItemStack> results,FluidStack fluidResult,int duration){
        this(items,fluid,support,results,fluidResult,duration,List.of());
    }
    public ReactionRecipe(List<InputItem> items,FluidStack fluid,Ingredient support,List<ItemStack> results,FluidStack fluidResult,int duration,List<FluidStack> additionalFluids){
        this(items,fluid,support,results,fluidResult,duration,additionalFluids,List.of());
    }
    public List<ItemStack> allItemResults(){return java.util.stream.Stream.concat(results.stream(),guaranteedResults.stream()).toList();}
    public List<ItemStack> completeOutputs(ItemStack rolled){
        var outputs=new java.util.ArrayList<>(guaranteedResults);if(!rolled.isEmpty())outputs.add(rolled);return List.copyOf(outputs);
    }
    public List<List<ItemStack>> outputOptions(){return results.isEmpty()?List.of(completeOutputs(ItemStack.EMPTY)):results.stream().map(this::completeOutputs).toList();}
    public List<FluidStack> fluidInputs(){
        var inputs=new java.util.ArrayList<FluidStack>();if(!fluid.isEmpty())inputs.add(fluid);inputs.addAll(additionalFluids);return List.copyOf(inputs);
    }
    public int totalFluidInput(){return fluidInputs().stream().mapToInt(FluidStack::getAmount).sum();}
    public static int batchAmount(FluidStack fluid,int step){return fluid.getAmount()*step/10-fluid.getAmount()*(step-1)/10;}
    public boolean requiresPool(){return !additionalFluids.isEmpty()||totalFluidInput()>4000||fluidResult.getAmount()>4000
            || !fluidInputs().isEmpty()&&!fluidResult.isEmpty();}
    public static final MapCodec<ReactionRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            InputItem.CODEC.listOf().optionalFieldOf("items",List.of()).forGetter(ReactionRecipe::items),
            FluidStack.CODEC.optionalFieldOf("fluid",FluidStack.EMPTY).forGetter(ReactionRecipe::fluid),
            Ingredient.CODEC.optionalFieldOf("support",Ingredient.EMPTY).forGetter(ReactionRecipe::support),
            ItemStack.STRICT_CODEC.listOf().optionalFieldOf("results",List.of()).forGetter(ReactionRecipe::results),
            FluidStack.CODEC.optionalFieldOf("fluid_result",FluidStack.EMPTY).forGetter(ReactionRecipe::fluidResult),
            Codec.intRange(1,72000).optionalFieldOf("duration",2000).forGetter(ReactionRecipe::duration),
            FluidStack.CODEC.listOf().optionalFieldOf("additional_fluids",List.of()).forGetter(ReactionRecipe::additionalFluids),
            ItemStack.STRICT_CODEC.listOf().optionalFieldOf("guaranteed_results",List.of()).forGetter(ReactionRecipe::guaranteedResults)).apply(i,ReactionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,ReactionRecipe> STREAM_CODEC=new StreamCodec<>() {
        public ReactionRecipe decode(RegistryFriendlyByteBuf b) {
            return CODEC.codec().parse(b.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),b.readNbt()).getOrThrow();
        }
        public void encode(RegistryFriendlyByteBuf b,ReactionRecipe r) {
            b.writeNbt((net.minecraft.nbt.CompoundTag)CODEC.codec().encodeStart(b.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),r).getOrThrow());
        }
    };
    public record Input(net.neoforged.neoforge.items.IItemHandler inventory,
                        net.neoforged.neoforge.fluids.capability.IFluidHandler tank,
                        java.util.function.Predicate<Ingredient> support) implements RecipeInput {
        public Input(RoughAlchemyCauldronBlockEntity vessel){this(vessel.items,vessel.tank,
                ingredient->ingredient.isEmpty()||ingredient.test(new ItemStack(vessel.getLevel().getBlockState(vessel.getBlockPos().below()).getBlock())));}
        public ItemStack getItem(int slot){return inventory.getStackInSlot(slot);}
        public int size(){return inventory.getSlots();}
    }
    public boolean supportMatches(RoughAlchemyCauldronBlockEntity vessel) {
        return support.isEmpty()||support.test(new ItemStack(vessel.getLevel().getBlockState(vessel.getBlockPos().below()).getBlock()));
    }
    public int[] assignment(Input input) {
        int[] slots=new int[items.size()];
        return assign(input,slots,0,0)?slots:null;
    }
    private boolean assign(Input input,int[] slots,int n,int used) {
        if(n==items.size())return true;
        var required=items.get(n);
        for(int slot=0;slot<input.size();slot++)if((used&(1<<slot))==0&&required.ingredient.test(input.getItem(slot))
                &&input.getItem(slot).getCount()>=required.count){
            slots[n]=slot;if(assign(input,slots,n+1,used|(1<<slot)))return true;
        }
        return false;
    }
    public boolean matches(Input input,Level level) {
        return input.support.test(support)&&assignment(input)!=null&&fluidInputs().stream().allMatch(f->
                input.tank.drain(f,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).getAmount()>=f.getAmount());
    }
    public ItemStack assemble(Input input,HolderLookup.Provider registries){return getResultItem(registries);}
    public ItemStack getResultItem(HolderLookup.Provider registries){return allItemResults().isEmpty()?ItemStack.EMPTY:allItemResults().getFirst().copy();}
    public boolean canCraftInDimensions(int width,int height){return true;}
    public boolean isSpecial(){return true;}
    public RecipeType<?> getType(){return ModRecipes.REACTION_TYPE.get();}
    public RecipeSerializer<?> getSerializer(){return ModRecipes.REACTION_SERIALIZER.get();}
}

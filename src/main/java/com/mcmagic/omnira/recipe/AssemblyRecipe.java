package com.mcmagic.omnira.recipe;

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

public record AssemblyRecipe(List<Ingredient> slots,ItemStack result,boolean outerShapeless,String operation) implements Recipe<AssemblyRecipe.Input> {
    public AssemblyRecipe(List<Ingredient> slots,ItemStack result) {this(slots,result,false,"fixed");}
    public AssemblyRecipe(List<Ingredient> slots,ItemStack result,boolean outerShapeless) {this(slots,result,outerShapeless,"fixed");}
    public AssemblyRecipe {
        slots=List.copyOf(slots);
        if(!List.of("fixed","alternating","grid_upgrade","staff","staff_upgrade","arquebus","arquebus_upgrade","armor_exchange").contains(operation)) throw new IllegalArgumentException("Unknown assembly operation: "+operation);
        if(slots.size()!=7 || slots.stream().allMatch(Ingredient::isEmpty) || result.getCount()!=1)
            throw new IllegalArgumentException("Assembly requires seven positions, at least one component and one result");
    }
    public record Input(Container container) implements RecipeInput {
        @Override public ItemStack getItem(int slot) {return container.getItem(slot);}
        @Override public int size() {return 7;}
    }
    public static final MapCodec<AssemblyRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Ingredient.CODEC.listOf().fieldOf("slots").forGetter(AssemblyRecipe::slots),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblyRecipe::result),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("outer_shapeless",false).forGetter(AssemblyRecipe::outerShapeless),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("operation","fixed").forGetter(AssemblyRecipe::operation)).apply(i,AssemblyRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,AssemblyRecipe> STREAM_CODEC=new StreamCodec<>() {
        @Override public AssemblyRecipe decode(RegistryFriendlyByteBuf buf) {
            var slots=new java.util.ArrayList<Ingredient>();
            for(int i=0;i<7;i++) slots.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new AssemblyRecipe(slots,ItemStack.STREAM_CODEC.decode(buf),buf.readBoolean(),buf.readUtf());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf,AssemblyRecipe recipe) {
            for(var ingredient:recipe.slots) Ingredient.CONTENTS_STREAM_CODEC.encode(buf,ingredient);
            ItemStack.STREAM_CODEC.encode(buf,recipe.result);
            buf.writeBoolean(recipe.outerShapeless);
            buf.writeUtf(recipe.operation);
        }
    };
    @Override public boolean matches(Input input,Level level) {
        if(operation.equals("armor_exchange") && com.mcmagic.omnira.registry.ArmorContent.exchange(input.getItem(6),result).isEmpty())return false;
        if(result.is(com.mcmagic.omnira.registry.ModItems.LIQUID_CRYSTAL_BALL.get()) && input.getItem(6).has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA))return false;
        if(result.is(com.mcmagic.omnira.registry.ModItems.SPACETIME_STABILIZATION_UPGRADE.get()) && input.getItem(6).getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).stream().anyMatch(s->!s.isEmpty()))return false;
        if(operation.equals("arquebus") && input.getItem(6).getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).stream().anyMatch(s->!s.isEmpty())) return false;
        if(operation.equals("arquebus_upgrade") && (!(input.getItem(6).getItem() instanceof com.mcmagic.omnira.item.ArcaneArquebusItem)
                || com.mcmagic.omnira.item.ArcaneArquebusItem.upgraded(input.getItem(6)))) return false;
        if(operation.equals("alternating")) return matches(slots.get(6),input.getItem(6))
                && (matchAlternating(input,0,0,0) || matchAlternating(input,0,0,1));
        if(operation.startsWith("staff")) return !StaffAssemblyRecipes.assemble(input,operation).isEmpty();
        if(outerShapeless) return matches(slots.get(6),input.getItem(6)) && matchOuter(input,0,0)
                && (!operation.equals("grid_upgrade") || !GridAssemblyRecipes.upgrade(input,result).isEmpty());
        for(int i=0;i<7;i++)
            if(slots.get(i).isEmpty()?!input.getItem(i).isEmpty():!slots.get(i).test(input.getItem(i))) return false;
        return !operation.equals("grid_upgrade") || !GridAssemblyRecipes.upgrade(input,result).isEmpty();
    }
    private static boolean matches(Ingredient ingredient,ItemStack stack) {return ingredient.isEmpty()?stack.isEmpty():ingredient.test(stack);}
    private boolean matchAlternating(Input input,int slot,int used,int parity) {
        if(slot==6) return true;
        for(int i=(slot+parity)%2;i<6;i+=2)
            if((used&(1<<i))==0 && matches(slots.get(i),input.getItem(slot))
                    && matchAlternating(input,slot+1,used|(1<<i),parity)) return true;
        return false;
    }
    private boolean matchOuter(Input input,int slot,int used) {
        if(slot==6) return true;
        for(int i=0;i<6;i++) if((used&(1<<i))==0 && matches(slots.get(i),input.getItem(slot)) && matchOuter(input,slot+1,used|(1<<i))) return true;
        return false;
    }
    @Override public ItemStack assemble(Input input,HolderLookup.Provider registries) {
        if(operation.equals("armor_exchange"))return com.mcmagic.omnira.registry.ArmorContent.exchange(input.getItem(6),result);
        if(operation.equals("arquebus_upgrade")) {
            var gun=input.getItem(6).copyWithCount(1);
            if(com.mcmagic.omnira.item.ArcaneArquebusItem.upgraded(gun)) return ItemStack.EMPTY;
            var plugin=com.mcmagic.omnira.item.ArquebusPlugin.of(result);
            if(plugin==com.mcmagic.omnira.item.ArquebusPlugin.NONE) return ItemStack.EMPTY;
            gun.remove(com.mcmagic.omnira.registry.ModDataComponents.ANCESTOR_LAUNCHER);
            gun.set(com.mcmagic.omnira.registry.ModDataComponents.ARQUEBUS_PLUGIN,plugin);
            return gun;
        }
        if(operation.equals("grid_upgrade")) return GridAssemblyRecipes.upgrade(input,result);
        return operation.startsWith("staff")?StaffAssemblyRecipes.assemble(input,operation):result.copy();
    }
    public List<Ingredient> displaySlots() {return operation.equals("staff")?StaffAssemblyRecipes.displaySlots():slots;}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {return result.copy();}
    @Override public boolean canCraftInDimensions(int w,int h) {return true;}
    @Override public boolean isSpecial() {return true;}
    @Override public RecipeType<?> getType() {return ModRecipes.ASSEMBLY_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer() {return ModRecipes.ASSEMBLY_SERIALIZER.get();}
}

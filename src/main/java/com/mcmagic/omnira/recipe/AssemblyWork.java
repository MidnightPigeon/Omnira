package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.registry.ModRecipes;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.function.Supplier;

/** Output is rolled only when the last strike commits the operation. */
public record AssemblyWork(String id,Supplier<List<ItemStack>> finish) {
    public static AssemblyWork find(Level level,Container inventory) {
        var recipe=level.getRecipeManager().getRecipeFor(ModRecipes.ASSEMBLY_TYPE.get(),new AssemblyRecipe.Input(inventory),level);
        if(recipe.isPresent()) {
            var holder=recipe.get();
            return new AssemblyWork("assembly/"+holder.id(),()->{
                var results=new java.util.ArrayList<ItemStack>();
                results.add(holder.value().assemble(new AssemblyRecipe.Input(inventory),level.registryAccess()));
                for(int i=0;i<7;i++) {
                    var input=inventory.getItem(i);
                    if(results.getFirst().is(com.mcmagic.omnira.registry.ModItems.LAVA_PRODUCTION_UPGRADE.get()) && input.hasCraftingRemainingItem())results.add(input.getCraftingRemainingItem());
                }
                return results;
            });
        }
        return net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.CreatePressing.find(level,inventory):null;
    }
}

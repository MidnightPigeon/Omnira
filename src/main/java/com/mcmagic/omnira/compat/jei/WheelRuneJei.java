package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.compat.WheelRuneDeploying;
import com.mcmagic.omnira.registry.ModItems;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class WheelRuneJei {
    static void register(IRecipeRegistration registration){
        var wheel=new ItemStack(ModItems.INDUSTRIAL_WHEEL.get());
        var recipes=java.util.stream.Stream.of(ModItems.SWIFTNESS_RUNE,ModItems.WIDE_AREA_RUNE,ModItems.AMPLIFICATION_RUNE)
                .flatMap(rune->WheelRuneDeploying.recipe(wheel,new ItemStack(rune.get())).stream()).toList();
        registration.addRecipes(RecipeType.<DeployerApplicationRecipe>createRecipeHolderType(ResourceLocation.parse("create:deploying")),recipes);
    }
}

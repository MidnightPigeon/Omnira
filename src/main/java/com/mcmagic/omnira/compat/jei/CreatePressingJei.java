package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu;
import com.mcmagic.omnira.registry.*;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.world.item.ItemStack;

/** Loaded only when both optional integrations are present. */
public final class CreatePressingJei {
    private static final RecipeType<PressingRecipe> TYPE=RecipeType.create("create","pressing",PressingRecipe.class);
    public static void catalyst(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.ARCANE_ASSEMBLY_TABLE.get()),TYPE);
    }
    public static void transfer(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(ArcaneAssemblyTableMenu.class,ModMenuTypes.ARCANE_ASSEMBLY_TABLE.get(),TYPE,6,1,7,36);
    }
}

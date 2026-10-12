package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.reversal.*;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class ReversalFuelJei implements IModPlugin {
    private static final RecipeType<ReversalFuels.Fuel> TIME=RecipeType.create("omnira","temporal_energy",ReversalFuels.Fuel.class);
    private static final RecipeType<ReversalFuels.Fuel> SPACE=RecipeType.create("omnira","spatial_energy",ReversalFuels.Fuel.class);
    public net.minecraft.resources.ResourceLocation getPluginUid(){return net.minecraft.resources.ResourceLocation.parse("omnira:instrument_fuels");}
    public void registerCategories(IRecipeCategoryRegistration r){
        var gui=r.getJeiHelpers().getGuiHelper();
        for(boolean spatial:new boolean[]{false,true})r.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<ReversalFuels.Fuel>(){
            public RecipeType<ReversalFuels.Fuel> getRecipeType(){return spatial?SPACE:TIME;}
            public Component getTitle(){return Component.translatable(spatial?"jei.omnira.spatial_energy":"jei.omnira.temporal_energy");}
            public mezz.jei.api.gui.drawable.IDrawable getIcon(){return gui.createDrawableItemStack(new ItemStack(spatial?ReversalContent.INFUSER_ITEM.get():ReversalContent.MACHINE_ITEM.get()));}
            public int getWidth(){return 130;}public int getHeight(){return 40;}
            public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,ReversalFuels.Fuel f,IFocusGroup focus){
                b.addSlot(RecipeIngredientRole.INPUT,12,11).setStandardSlotBackground().addItemStack(f.stack())
                        .addRichTooltipCallback((v,t)->t.add(Component.translatable("jei.omnira.energy_points",f.energy())));
            }
            public void draw(ReversalFuels.Fuel f,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                g.drawString(net.minecraft.client.Minecraft.getInstance().font,Component.translatable("jei.omnira.energy_points",f.energy()),42,15,0x445B65,false);
            }
        });
    }
    public void registerRecipes(IRecipeRegistration r){r.addRecipes(TIME,ReversalFuels.all(false));r.addRecipes(SPACE,ReversalFuels.all(true));}
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(ReversalContent.MACHINE_ITEM.get()),TIME);r.addRecipeCatalyst(new ItemStack(ReversalContent.INFUSER_ITEM.get()),SPACE);}
}

package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.reversal.*;
import mezz.jei.api.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.recipe.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

@JeiPlugin
public final class ReversalJei implements IModPlugin {
    public static final RecipeType<ReversalRecipe> TYPE=RecipeType.create("omnira","spacetime_reversal",ReversalRecipe.class);
    public net.minecraft.resources.ResourceLocation getPluginUid(){return net.minecraft.resources.ResourceLocation.parse("omnira:reversal");}
    public void registerCategories(IRecipeCategoryRegistration registration){var gui=registration.getJeiHelpers().getGuiHelper();registration.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<ReversalRecipe>(){
        public RecipeType<ReversalRecipe> getRecipeType(){return TYPE;}
        public Component getTitle(){return ReversalContent.MACHINE_ITEM.get().getDescription();}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return gui.createDrawableItemStack(new ItemStack(ReversalContent.MACHINE_ITEM.get()));}
        public int getWidth(){return 160;}public int getHeight(){return 64;}
        public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,ReversalRecipe r,IFocusGroup f){
            if(r.results().getFirst().is(com.mcmagic.omnira.registry.ModItems.MEMORY_CUBE.get())){
                for(int i=0;i<2;i++){
                    var input=com.mcmagic.omnira.item.MemoryCubeBlockItem.withState(new ItemStack(com.mcmagic.omnira.registry.ModItems.MEMORY_CUBE.get()),i==0);
                    b.addSlot(RecipeIngredientRole.INPUT,14,2+23*i).setStandardSlotBackground().addItemStack(input);
                    b.addSlot(RecipeIngredientRole.OUTPUT,100,2+23*i).setStandardSlotBackground().addItemStack(r.outputs(input).getFirst());
                }
                return;
            }
            b.addSlot(RecipeIngredientRole.INPUT,14,13).setStandardSlotBackground().addItemStacks(java.util.Arrays.stream(r.ingredient().getItems()).map(s->s.copyWithCount(r.count())).toList());
            for(int i=0;i<r.results().size();i++)b.addSlot(RecipeIngredientRole.OUTPUT,76+26*i,13).setStandardSlotBackground().addItemStack(r.results().get(i));
        }
        public void draw(ReversalRecipe r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
            var font=net.minecraft.client.Minecraft.getInstance().font;g.drawString(font,"->",45,18,0x445B65,false);g.drawString(font,Component.translatable("jei.omnira.reversal_cost"),6,40,0x445B65,false);
            if(r.results().size()>1)g.drawString(font,Component.translatable("jei.omnira.reversal_random"),6,52,0x445B65,false);
        }
    });}
    public void registerRecipes(IRecipeRegistration r){var l=net.minecraft.client.Minecraft.getInstance().level;if(l!=null)r.addRecipes(TYPE,l.getRecipeManager().getAllRecipesFor(ReversalContent.TYPE.get()).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());}
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(ReversalContent.MACHINE_ITEM.get()),TYPE);}
}

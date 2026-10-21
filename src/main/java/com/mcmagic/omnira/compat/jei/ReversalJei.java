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
    public static final RecipeType<ReversalRecipe> INFUSION=RecipeType.create("omnira","void_infusion",ReversalRecipe.class);
    public net.minecraft.resources.ResourceLocation getPluginUid(){return net.minecraft.resources.ResourceLocation.parse("omnira:reversal");}
    public void registerCategories(IRecipeCategoryRegistration registration){var gui=registration.getJeiHelpers().getGuiHelper();for(boolean infusion:new boolean[]{false,true})registration.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<ReversalRecipe>(){
        public RecipeType<ReversalRecipe> getRecipeType(){return infusion?INFUSION:TYPE;}
        public Component getTitle(){return (infusion?ReversalContent.INFUSER_ITEM.get():ReversalContent.MACHINE_ITEM.get()).getDescription();}
        public mezz.jei.api.gui.drawable.IDrawable getIcon(){return gui.createDrawableItemStack(new ItemStack(infusion?ReversalContent.INFUSER_ITEM.get():ReversalContent.MACHINE_ITEM.get()));}
        public int getWidth(){return 160;}public int getHeight(){return 64;}
        public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,ReversalRecipe r,IFocusGroup f){
            if(r.results().getFirst().is(com.mcmagic.omnira.registry.ModItems.MEMORY_CUBE.get())){
                for(int i=0;i<2;i++){
                    var input=com.mcmagic.omnira.item.MemoryCubeBlockItem.withState(new ItemStack(com.mcmagic.omnira.registry.ModItems.MEMORY_CUBE.get()),i==0);
                    b.addSlot(RecipeIngredientRole.INPUT,14,1+21*i).setStandardSlotBackground().addItemStack(input);
                    b.addSlot(RecipeIngredientRole.OUTPUT,100,1+21*i).setStandardSlotBackground().addItemStack(r.outputs(input).getFirst());
                }
                return;
            }
            b.addSlot(RecipeIngredientRole.INPUT,14,13).setStandardSlotBackground().addItemStacks(java.util.Arrays.stream(r.ingredient().getItems()).map(s->s.copyWithCount(r.count())).toList());
            b.addSlot(RecipeIngredientRole.OUTPUT,100,13).setStandardSlotBackground().addItemStacks(r.results())
                    .addRichTooltipCallback((v,t)->{if(r.results().size()>1)t.add(Component.translatable("jei.omnira.reaction.chance",100.0/r.results().size()));});
        }
        public void draw(ReversalRecipe r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
            var font=net.minecraft.client.Minecraft.getInstance().font;
            boolean memory=r.results().getFirst().is(com.mcmagic.omnira.registry.ModItems.MEMORY_CUBE.get());
            if(memory){gui.getRecipeArrow().draw(g,53,2);gui.getRecipeArrow().draw(g,53,23);}
            else gui.getRecipeArrow().draw(g,53,14);
            g.drawString(font,Component.translatable(infusion?"jei.omnira.infusion_cost":"jei.omnira.reversal_cost"),6,memory?43:40,0x445B65,false);
            if(r.results().size()>1)g.drawString(font,Component.translatable("jei.omnira.reversal_random"),6,52,0x445B65,false);
        }
    });}
    public void registerRecipes(IRecipeRegistration r){var l=net.minecraft.client.Minecraft.getInstance().level;if(l!=null){r.addRecipes(TYPE,l.getRecipeManager().getAllRecipesFor(ReversalContent.TYPE.get()).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());r.addRecipes(INFUSION,l.getRecipeManager().getAllRecipesFor(ReversalContent.INFUSION_TYPE.get()).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());}}
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(ReversalContent.MACHINE_ITEM.get()),TYPE);r.addRecipeCatalyst(new ItemStack(ReversalContent.INFUSER_ITEM.get()),INFUSION);}
}

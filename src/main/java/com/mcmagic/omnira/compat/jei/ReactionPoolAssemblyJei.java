package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.alchemy.ReactionPoolStructure;
import com.mcmagic.omnira.registry.ModItems;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

@JeiPlugin
public final class ReactionPoolAssemblyJei implements IModPlugin {
    public record Assembly(){}
    private static final RecipeType<Assembly> TYPE=RecipeType.create("omnira","reaction_pool_assembly",Assembly.class);
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.parse("omnira:reaction_pool_assembly");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){
        var icon=registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(new ItemStack(ModItems.REACTION_POOL.get()));
        registration.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<Assembly>(){
            @Override public RecipeType<Assembly> getRecipeType(){return TYPE;}
            @Override public Component getTitle(){return ModItems.REACTION_POOL.get().getDescription();}
            @Override public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
            @Override public int getWidth(){return 214;}
            @Override public int getHeight(){return 130;}
            @Override public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder,Assembly recipe,IFocusGroup focus){
                for(int layer=0;layer<2;layer++)for(int row=0;row<3;row++)for(int col=0;col<3;col++){
                    int part=layer*9+(2-row)*3+col;
                    var slot=builder.addSlot(RecipeIngredientRole.INPUT,8+layer*74+col*19,20+row*19).setStandardSlotBackground();
                    slot.addItemStack(new ItemStack(ReactionPoolStructure.required(part)));
                }
                builder.addSlot(RecipeIngredientRole.INPUT,8,88).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
                builder.addSlot(RecipeIngredientRole.OUTPUT,186,39).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.REACTION_POOL.get()));
            }
            @Override public void draw(Assembly recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                label(g,"jei.omnira.reaction_pool.lower",8,5,58);label(g,"jei.omnira.reaction_pool.upper",82,5,58);
                label(g,"jei.omnira.reaction_pool.activate",32,91,174);label(g,"jei.omnira.reaction_pool.front",8,114,198);
                g.fill(152,46,173,48,0xFF8DB6BB);g.fill(170,43,172,51,0xFF8DB6BB);
            }
        });
    }
    private static void label(net.minecraft.client.gui.GuiGraphics g,String key,int x,int y,int width){
        var font=net.minecraft.client.Minecraft.getInstance().font;var text=Component.translatable(key);
        float scale=Math.min(1,width/(float)font.width(text));g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,text,0,0,0x404040,false);g.pose().popPose();
    }
    @Override public void registerRecipes(IRecipeRegistration r){r.addRecipes(TYPE,List.of(new Assembly()));}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(com.mcmagic.omnira.registry.DreamContent.DREAM_CRYSTAL_BLOCK.get()),TYPE);}
}

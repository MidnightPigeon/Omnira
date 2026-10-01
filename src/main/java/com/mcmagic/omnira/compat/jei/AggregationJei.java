package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.registry.*;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

@JeiPlugin
public final class AggregationJei implements IModPlugin {
    public record Assembly(){}
    private static final RecipeType<Assembly> TYPE=RecipeType.create("omnira","aggregation_assembly",Assembly.class);
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.parse("omnira:aggregation");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){
        var icon=registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(new ItemStack(AggregationContent.ITEM.get()));
        registration.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<Assembly>(){
            @Override public RecipeType<Assembly> getRecipeType(){return TYPE;}
            @Override public net.minecraft.network.chat.Component getTitle(){return AggregationContent.ITEM.get().getDescription();}
            @Override public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
            @Override public int getWidth(){return 160;}
            @Override public int getHeight(){return 94;}
            @Override public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder,Assembly recipe,IFocusGroup focus){
                for(int i=0;i<9;i++){
                    List<ItemStack> choices=i==4?List.of(new ItemStack(ModItems.CRYSTAL_PROCESSING_TABLE.get())):
                            i==1||i==7?List.of(new ItemStack(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()),new ItemStack(AggregationContent.SPATIAL_BLOCK.get())):
                            i==3||i==5?List.of(new ItemStack(DreamContent.LIVING_TEMPORAL_SILT.get()),new ItemStack(TimeNatureContent.LIVING_SILT.get())):
                            List.of(new ItemStack(ModBlocks.INFUSED_CRYSTAL_CASING.get()));
                    builder.addSlot(RecipeIngredientRole.INPUT,8+i%3*19,5+i/3*19).setStandardSlotBackground().addItemStacks(choices);
                }
                builder.addSlot(RecipeIngredientRole.INPUT,77,24).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
                builder.addSlot(RecipeIngredientRole.OUTPUT,132,24).setStandardSlotBackground().addItemStack(new ItemStack(AggregationContent.ITEM.get()));
            }
            @Override public void draw(Assembly recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                var font=net.minecraft.client.Minecraft.getInstance().font;
                for(int i=0;i<2;i++){
                    var text=net.minecraft.network.chat.Component.translatable(i==0?"jei.omnira.aggregation.vertical":"jei.omnira.aggregation.activate");
                    float scale=Math.min(1,144F/font.width(text));g.pose().pushPose();g.pose().translate(8,68+i*13,0);g.pose().scale(scale,scale,1);
                    g.drawString(font,text,0,0,0x404040,false);g.pose().popPose();
                }
                g.fill(101,31,122,33,0xFF8DB6BB);
            }
        });
    }
    @Override public void registerRecipes(IRecipeRegistration r){r.addRecipes(TYPE,List.of(new Assembly()));}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(ModItems.CRYSTAL_PROCESSING_TABLE.get()),TYPE);}
}

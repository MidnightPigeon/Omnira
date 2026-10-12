package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.registry.*;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

@JeiPlugin
public final class CatalysisJei implements IModPlugin {
    private List<Display> dynamic=List.of();
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime){
        com.mcmagic.omnira.client.CatalysisCatalogueView.changed=()->{
            var updated=com.mcmagic.omnira.client.CatalysisCatalogueView.recipes.stream().map(r->new Display(r,true)).toList();
            if(updated.equals(dynamic))return;
            runtime.getRecipeManager().hideRecipes(TYPE,dynamic);
            dynamic=updated;
            runtime.getRecipeManager().addRecipes(TYPE,dynamic);
        };
        com.mcmagic.omnira.client.CatalysisCatalogueView.changed.run();
    }
    public void onRuntimeUnavailable(){com.mcmagic.omnira.client.CatalysisCatalogueView.changed=()->{};dynamic=List.of();}
    public record Display(CatalysisRecipe recipe,boolean variable){}
    public record Assembly(){}
    private static final RecipeType<Display> TYPE=RecipeType.create("omnira","catalysis",Display.class);
    private static final RecipeType<Assembly> ASSEMBLY=RecipeType.create("omnira","catalysis_assembly",Assembly.class);
    public ResourceLocation getPluginUid(){return ResourceLocation.parse("omnira:catalysis");}
    public void registerCategories(IRecipeCategoryRegistration r){
        var icon=r.getJeiHelpers().getGuiHelper().createDrawableItemStack(new ItemStack(CatalysisContent.ITEM.get()));
        r.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<Display>(){
            public RecipeType<Display> getRecipeType(){return TYPE;}
            public Component getTitle(){return CatalysisContent.BLOCK.get().getName();}
            public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
            public int getWidth(){return 150;}public int getHeight(){return 120;}
            public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,Display d,IFocusGroup focus){
                var recipe=d.recipe;
                b.addSlot(RecipeIngredientRole.CATALYST,49,45).setStandardSlotBackground().addIngredients(recipe.substrate());
                b.addSlot(RecipeIngredientRole.CATALYST,85,45).setStandardSlotBackground().addIngredients(recipe.seed());
                b.addSlot(RecipeIngredientRole.INPUT,67,77).setStandardSlotBackground().addFluidStack(recipe.fluid().getFluid(),100).setFluidRenderer(100,false,16,16)
                        .addRichTooltipCallback((slot,tip)->tip.add(Component.translatable("jei.omnira.catalysis.step")));
                b.addSlot(RecipeIngredientRole.OUTPUT,67,13).setStandardSlotBackground().addItemStack(recipe.result())
                        .addRichTooltipCallback((slot,tip)->{
                            if(recipe.yield()==CatalysisGrowth.Yield.FUNGUS)tip.add(Component.translatable("jei.omnira.catalysis.fungus_yield"));
                            else if(d.variable)tip.add(Component.translatable("jei.omnira.catalysis.variable"));
                        });
            }
            public void draw(Display d,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                g.fill(33,3,35,101,0xFF8DACB9);g.fill(115,3,117,101,0xFF8DACB9);g.fill(35,3,115,5,0xFFB9DADD);
                g.fill(25,99,125,101,0xFF8DACB9);g.fill(25,101,125,105,0xFFB9DADD);
                g.drawCenteredString(Minecraft.getInstance().font,Component.translatable("jei.omnira.catalysis.stages",CatalysisBlockEntity.CYCLE_STEPS),75,109,0xFF56707C);
            }
        },new mezz.jei.api.recipe.category.IRecipeCategory<Assembly>(){
            public RecipeType<Assembly> getRecipeType(){return ASSEMBLY;}public Component getTitle(){return Component.translatable("jei.omnira.catalysis.assembly");}
            public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}public int getWidth(){return 170;}public int getHeight(){return 128;}
            public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,Assembly a,IFocusGroup focus){
                for(int i=0;i<3;i++)b.addSlot(RecipeIngredientRole.INPUT,131,8+(2-i)*23).setStandardSlotBackground().addItemStack(new ItemStack(CatalysisStructure.required(i)));
                b.addSlot(RecipeIngredientRole.INPUT,131,81).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
                b.addSlot(RecipeIngredientRole.OUTPUT,131,107).setStandardSlotBackground().addItemStack(new ItemStack(CatalysisContent.ITEM.get()));
            }
            public void draw(Assembly a,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                com.mcmagic.omnira.client.guide.MachineConstructionScene.render(g,0,5,(int)(System.currentTimeMillis()/50),"alchemical_catalysis_chamber",0,0,true);
            }
        });
    }
    public void registerRecipes(IRecipeRegistration r){
        var level=Minecraft.getInstance().level;if(level==null)return;
        var recipes=new ArrayList<Display>();level.getRecipeManager().getAllRecipesFor(CatalysisContent.TYPE.get()).forEach(h->recipes.add(new Display(h.value(),false)));
        var water=new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100);
        dynamic=com.mcmagic.omnira.client.CatalysisCatalogueView.recipes.stream().map(recipe->new Display(recipe,true)).toList();
        recipes.addAll(dynamic);
        recipes.add(new Display(new CatalysisRecipe(Ingredient.of(DreamContent.TEMPORAL_SILT.get(),DreamContent.LIVING_TEMPORAL_SILT.get(),TimeNatureContent.SILT.get(),TimeNatureContent.LIVING_SILT.get(),com.mcmagic.omnira.mire.MireContent.SILT.get()),Ingredient.of(TimeNatureContent.SAPLING.get()),
                new FluidStack(com.mcmagic.omnira.mire.MireContent.SOURCE.get(),100),new ItemStack(TimeNatureContent.LOG.get()),4),true));
        var shadowSoils=net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
                .filter(block->block.defaultBlockState().is(com.mcmagic.omnira.time.TemporalSoils.SHADOW))
                .map(ItemStack::new).filter(stack->!stack.isEmpty());
        recipes.add(new Display(new CatalysisRecipe(Ingredient.of(shadowSoils),Ingredient.of(ShadowWoodContent.SAPLING.get()),
                water,new ItemStack(DreamContent.SHADOW_LOG.get()),4),true));
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){var ore=CatalysisGrowth.ore(new ItemStack(item));if(ore.isEmpty())continue;
            recipes.add(new Display(new CatalysisRecipe(Ingredient.of(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()),Ingredient.of(item),new FluidStack(com.mcmagic.omnira.space.sea.VoidSeaContent.SOURCE.get(),100),ore,4),false));}
        r.addRecipes(TYPE,recipes);r.addRecipes(ASSEMBLY,List.of(new Assembly()));
    }
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(CatalysisContent.ITEM.get()),TYPE);r.addRecipeCatalyst(new ItemStack(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get()),ASSEMBLY);}
}

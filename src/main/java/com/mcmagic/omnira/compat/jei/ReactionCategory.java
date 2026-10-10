package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.alchemy.ReactionRecipe;
import com.mcmagic.omnira.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ReactionCategory implements IRecipeCategory<ReactionRecipe> {
    public static final RecipeType<ReactionRecipe> TYPE=RecipeType.create("omnira","reaction",ReactionRecipe.class);
    private final IDrawable icon;
    private static final int[][] POOL_INPUTS={{18,18},{38,18},{18,38},{38,38}};
    public ReactionCategory(IGuiHelper gui){icon=gui.createDrawableItemStack(new ItemStack(ModItems.PRIMITIVE_REACTION_CAULDRON.get()));}
    public RecipeType<ReactionRecipe> getRecipeType(){return TYPE;}
    public Component getTitle(){return Component.translatable("jei.omnira.reaction");}
    public IDrawable getIcon(){return icon;}
    public int getWidth(){return 176;}
    public int getHeight(){return 128;}
    public void setRecipe(IRecipeLayoutBuilder builder,ReactionRecipe recipe,IFocusGroup focus){
        boolean pool=recipe.requiresPool();
        int[][] slots=pool?POOL_INPUTS:new int[][]{{47,4},{76,33},{47,62},{18,33}};
        for(int i=0;i<recipe.items().size();i++){
            var input=recipe.items().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT,slots[i][0],slots[i][1]).setStandardSlotBackground()
                    .addItemStacks(java.util.Arrays.stream(input.ingredient().getItems()).map(s->s.copyWithCount(input.count())).toList());
        }
        for(int i=0;i<recipe.fluidInputs().size();i++){
            var fluid=recipe.fluidInputs().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT,pool?80+i%2*22:47,pool?18+i/2*22:33).setStandardSlotBackground()
                    .addFluidStack(fluid.getFluid(),fluid.getAmount()).setFluidRenderer(Math.max(4000,fluid.getAmount()),false,16,16)
                    .addRichTooltipCallback((view,tooltip)->{if(pool)tooltip.add(Component.translatable("jei.omnira.reaction.pool_only"));});
        }
        if(!recipe.support().isEmpty())builder.addSlot(RecipeIngredientRole.CATALYST,47,91).setStandardSlotBackground().addIngredients(recipe.support())
                .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.reaction.support")));
        int count=outputCount(recipe),slot=0;
        for(var output:recipe.guaranteedResults()){
            builder.addSlot(RecipeIngredientRole.OUTPUT,outputX(count,slot),outputY(count,slot++)).setStandardSlotBackground().addItemStack(output)
                    .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.reaction.guaranteed")));
        }
        if(!recipe.results().isEmpty())builder.addSlot(RecipeIngredientRole.OUTPUT,outputX(count,slot),outputY(count,slot++)).setStandardSlotBackground().addItemStacks(recipe.results())
                .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.reaction.chance",100.0/recipe.results().size())));
        if(!recipe.fluidResult().isEmpty())builder.addSlot(RecipeIngredientRole.OUTPUT,outputX(count,slot),outputY(count,slot)).setStandardSlotBackground()
                .addFluidStack(recipe.fluidResult().getFluid(),recipe.fluidResult().getAmount()).setFluidRenderer(Math.max(4000,recipe.fluidResult().getAmount()),false,16,16);
    }
    public void draw(ReactionRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double mouseX,double mouseY){
        boolean pool=recipe.requiresPool();
        int[][] frames=pool?POOL_INPUTS:new int[][]{{47,4},{76,33},{47,62},{18,33},{47,33}};
        for(var slot:frames){
            g.fill(slot[0]-1,slot[1]-1,slot[0]+17,slot[1]+17,0xFF829EA8);
            g.fill(slot[0],slot[1],slot[0]+16,slot[1]+16,0xFFDAE9EB);
        }
        for(int i=0;pool&&i<recipe.fluidInputs().size();i++)frame(g,80+i%2*22,18+i/2*22);
        int count=outputCount(recipe);for(int i=0;i<count;i++)frame(g,outputX(count,i),outputY(count,i));
        int[][] points=pool?new int[][]{{14,12},{58,12},{64,18},{64,52},{58,58},{14,58},{8,52},{8,18}}:new int[][]{{39,3},{72,3},{94,25},{94,57},{72,79},{39,79},{17,57},{17,25}};
        for(int i=0;i<8;i++)line(g,points[i][0],points[i][1],points[(i+1)%8][0],points[(i+1)%8][1],0xFFB9DDE2);
        int arrowY=pool?76:41;
        line(g,101,arrowY,132,arrowY,0xFF92ADB5);line(g,126,arrowY-5,132,arrowY,0xFF92ADB5);line(g,132,arrowY,126,arrowY+5,0xFF92ADB5);
        g.drawString(Minecraft.getInstance().font,Component.translatable("jei.omnira.reaction.seconds",recipe.duration()/20),105,count>3?110:pool||count>2?94:70,0xFF405760,false);
        if(recipe.support().isEmpty())g.drawString(Minecraft.getInstance().font,Component.translatable("jei.omnira.reaction.any_support"),28,94,0xFF405760,false);
    }
    private static int outputCount(ReactionRecipe recipe){return recipe.guaranteedResults().size()+(recipe.results().isEmpty()?0:1)+(recipe.fluidResult().isEmpty()?0:1);}
    private static int outputX(int count,int slot){return count<=3?144:140+slot%2*18;}
    private static int outputY(int count,int slot){return count==1?33:count==2?18+slot*30:count==3?4+slot*26:2+slot/2*18;}
    private static void frame(GuiGraphics g,int x,int y){g.fill(x-1,y-1,x+17,y+17,0xFF829EA8);g.fill(x,y,x+16,y+16,0xFFDAE9EB);}
    private static void line(GuiGraphics g,int x,int y,int xx,int yy,int color){
        int length=Math.max(Math.abs(xx-x),Math.abs(yy-y));
        for(int i=0;i<=length;i++){int a=x+(xx-x)*i/length,b=y+(yy-y)*i/length;g.fill(a,b,a+1,b+1,color);}
    }
}

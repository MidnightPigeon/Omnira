package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.registry.ModItems;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

@JeiPlugin
public final class AggregationWritingJei implements IModPlugin {
    public record Writing(List<ItemStack> slots,int cost){}
    private static final RecipeType<Writing> TYPE=RecipeType.create("omnira","advanced_writing",Writing.class);
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.parse("omnira:advanced_writing");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){
        var icon=registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(new ItemStack(AggregationContent.ITEM.get()));
        registration.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<Writing>(){
            @Override public RecipeType<Writing> getRecipeType(){return TYPE;}
            @Override public net.minecraft.network.chat.Component getTitle(){return AggregationContent.CRYSTAL.get().getDescription();}
            @Override public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
            @Override public int getWidth(){return 180;}
            @Override public int getHeight(){return 174;}
            @Override public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,Writing recipe,IFocusGroup focus){
                for(int i=0;i<12;i++){
                    var xy=AggregationLayout.UI[i];final int index=i;
                    var stack=recipe.slots().get(i);
                    var slot=b.addSlot(i==11?RecipeIngredientRole.OUTPUT:i==10?RecipeIngredientRole.CATALYST:stack.isEmpty()?RecipeIngredientRole.RENDER_ONLY:RecipeIngredientRole.INPUT,xy[0]-6,xy[1]-16)
                            .setStandardSlotBackground().addRichTooltipCallback((v,t)->t.add(net.minecraft.network.chat.Component.translatable("gui.omnira.aggregation."+AggregationLayout.role(index))));
                    if(!stack.isEmpty())slot.addItemStack(stack);
                    if(i==AggregationLayout.INK)slot.addRichTooltipCallback((v,t)->t.add(net.minecraft.network.chat.Component.translatable("jei.omnira.aggregation.ink")));
                }
            }
            @Override public void draw(Writing r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                if(r.slots().getFirst().is(ModItems.OMNI_MICROCORE.get()))for(int i=1;i<8;i++){
                    var xy=AggregationLayout.UI[i];com.mcmagic.omnira.client.screen.CompositeSlotGhost.render(g,r.slots().getFirst(),xy[0]-6,xy[1]-16);
                }
                if(com.mcmagic.omnira.spell.SpellPattern.composite(r.slots().getFirst())){
                    var xy=AggregationLayout.UI[1];
                    com.mcmagic.omnira.client.screen.CompositeSlotGhost.render(g,r.slots().getFirst(),xy[0]-6,xy[1]-16);
                }
                if(!r.slots().getFirst().is(ModItems.OMNI_MICROCORE.get()))for(int i=2;i<8;i++){var xy=AggregationLayout.UI[i];WritingSymbols.draw(g,xy[0]-6,xy[1]-16,i<5,i);}
                for(int i=0;i<12;i++){
                    var xy=AggregationLayout.UI[i];int xx=xy[0]-8,yy=xy[1]-18,c=0xFF000000|AggregationLayout.COLORS[i];
                    g.fill(xx,yy,xx+20,yy+1,c);g.fill(xx,yy+19,xx+20,yy+20,c);g.fill(xx,yy,xx+1,yy+20,c);g.fill(xx+19,yy,xx+20,yy+20,c);
                }
                g.drawString(net.minecraft.client.Minecraft.getInstance().font,net.minecraft.network.chat.Component.translatable("gui.omnira.aggregation.cost",r.cost()),8,148,0x404040,false);
                g.drawString(net.minecraft.client.Minecraft.getInstance().font,net.minecraft.network.chat.Component.translatable("jei.omnira.aggregation.duration"),8,161,0x404040,false);
            }
            @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip,Writing r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,double x,double y){
                if(r.slots().getFirst().is(ModItems.OMNI_MICROCORE.get())){
                    for(int i=0;i<8;i++){var p=AggregationLayout.UI[i];if(x>=p[0]-6&&x<p[0]+10&&y>=p[1]-16&&y<p[1]){tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.world_rotation.reserved"));return;}}
                    return;
                }
                for(int i=2;i<8;i++){var p=AggregationLayout.UI[i];if(x>=p[0]-6&&x<p[0]+10&&y>=p[1]-16&&y<p[1]){tooltip.addAll(WritingSymbols.tooltip(i<5));return;}}
            }
        });
    }
    @Override public void registerRecipes(IRecipeRegistration r){
        var recipes=new ArrayList<Writing>();
        var cores=List.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get(),ModItems.TIME_MICROCORE.get(),ModItems.SPACE_MICROCORE.get());
        for(int kind=0;kind<3;kind++){
            var target=cores.get(kind==2?4:0);int shape=kind==0?1:-1;
            var inventory=new net.minecraft.world.SimpleContainer(12);inventory.setItem(0,new ItemStack(target));
            if(shape>=0)inventory.setItem(1,new ItemStack(cores.get(shape)));
            inventory.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));inventory.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));inventory.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
            inventory.setItem(11,com.mcmagic.omnira.menu.CrystalProcessingTableMenu.previewCrystal(inventory,5,8,2,5,AggregationContent.CRYSTAL.get()));
            var slots=new ArrayList<ItemStack>();for(int i=0;i<12;i++)slots.add(inventory.getItem(i));recipes.add(new Writing(slots,shape<0?20:40));
        }
        var special=net.minecraft.core.NonNullList.withSize(12,ItemStack.EMPTY);
        special.set(0,new ItemStack(ModItems.OMNI_MICROCORE.get()));special.set(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
        special.set(9,new ItemStack(ModItems.SPELL_INK.get()));special.set(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        special.set(11,new ItemStack(ModItems.WORLD_ROTATION_CRYSTAL.get()));recipes.add(new Writing(special,20));
        r.addRecipes(TYPE,recipes);
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(AggregationContent.ITEM.get()),TYPE);}
}

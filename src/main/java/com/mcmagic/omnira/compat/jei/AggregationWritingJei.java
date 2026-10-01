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
                    b.addSlot(i==11?RecipeIngredientRole.OUTPUT:i==10?RecipeIngredientRole.CATALYST:RecipeIngredientRole.INPUT,xy[0]-6,xy[1]-16)
                            .setStandardSlotBackground().addItemStack(recipe.slots().get(i))
                            .addRichTooltipCallback((v,t)->t.add(net.minecraft.network.chat.Component.translatable("gui.omnira.aggregation."+AggregationLayout.role(index))));
                }
            }
            @Override public void draw(Writing r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                if(com.mcmagic.omnira.spell.SpellPattern.composite(r.slots().getFirst())){
                    var xy=AggregationLayout.UI[1];
                    com.mcmagic.omnira.client.screen.CompositeSlotGhost.render(g,r.slots().getFirst(),xy[0]-6,xy[1]-16);
                }
                for(int i=0;i<12;i++){
                    var xy=AggregationLayout.UI[i];int xx=xy[0]-8,yy=xy[1]-18,c=0xFF000000|AggregationLayout.COLORS[i];
                    g.fill(xx,yy,xx+20,yy+1,c);g.fill(xx,yy+19,xx+20,yy+20,c);g.fill(xx,yy,xx+1,yy+20,c);g.fill(xx+19,yy,xx+20,yy+20,c);
                }
                g.drawString(net.minecraft.client.Minecraft.getInstance().font,net.minecraft.network.chat.Component.translatable("gui.omnira.aggregation.cost",r.cost()),8,148,0x404040,false);
                g.drawString(net.minecraft.client.Minecraft.getInstance().font,net.minecraft.network.chat.Component.translatable("jei.omnira.aggregation.duration"),8,161,0x404040,false);
            }
        });
    }
    @Override public void registerRecipes(IRecipeRegistration r){
        var recipes=new ArrayList<Writing>();
        var cores=List.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get(),ModItems.TIME_MICROCORE.get(),ModItems.SPACE_MICROCORE.get());
        for(var target:cores)for(int shape=-1;shape<4;shape++){
            if(shape>=0&&(target==ModItems.TIME_MICROCORE.get()||target==ModItems.SPACE_MICROCORE.get()))continue;
            var inventory=new net.minecraft.world.SimpleContainer(12);inventory.setItem(0,new ItemStack(target));
            if(shape>=0)inventory.setItem(1,new ItemStack(cores.get(shape)));
            inventory.setItem(2,new ItemStack(ModItems.SHARP_BREATH.get()));inventory.setItem(3,new ItemStack(ModItems.HEALING_DEW.get()));inventory.setItem(4,new ItemStack(ModItems.CONSTRUCTION_MATRIX.get()));
            inventory.setItem(5,new ItemStack(Items.IRON_BLOCK));inventory.setItem(6,new ItemStack(Items.CLOCK));inventory.setItem(7,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            inventory.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));inventory.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));inventory.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
            inventory.setItem(11,com.mcmagic.omnira.menu.CrystalProcessingTableMenu.previewCrystal(inventory,5,8,2,5,AggregationContent.CRYSTAL.get()));
            var slots=new ArrayList<ItemStack>();for(int i=0;i<12;i++)slots.add(inventory.getItem(i));recipes.add(new Writing(slots,shape<0?140:160));
        }
        r.addRecipes(TYPE,recipes);
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(AggregationContent.ITEM.get()),TYPE);}
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration r){r.addRecipeTransferHandler(AggregationRingMenu.class,AggregationContent.MENU.get(),TYPE,0,11,12,36);}
}

package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.block.CrystalFamilies;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.space.anchor.AnchorContent;
import com.mcmagic.omnira.space.phase.PhaseContent;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

/** World actions are display entries, not crafting recipes; available with or without Create. */
@JeiPlugin
public final class WorldInteractionJei implements IModPlugin {
    public record Interaction(ItemStack tool,ItemStack target,ItemStack result,ItemStack remainder,String note,ItemStack previous){
        Interaction(ItemStack tool,ItemStack target,ItemStack result,ItemStack remainder,String note){this(tool,target,result,remainder,note,ItemStack.EMPTY);}
    }
    private static final RecipeType<Interaction> TYPE=RecipeType.create("omnira","world_interaction",Interaction.class);
    public net.minecraft.resources.ResourceLocation getPluginUid(){return net.minecraft.resources.ResourceLocation.parse("omnira:world_interactions");}
    public void registerCategories(IRecipeCategoryRegistration r){
        var gui=r.getJeiHelpers().getGuiHelper();
        r.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<Interaction>(){
            public RecipeType<Interaction> getRecipeType(){return TYPE;}
            public Component getTitle(){return Component.translatable("jei.omnira.world_interaction");}
            public mezz.jei.api.gui.drawable.IDrawable getIcon(){return gui.createDrawableItemStack(new ItemStack(ModItems.ARCANE_DUST.get()));}
            public int getWidth(){return 170;}public int getHeight(){return 94;}
            public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,Interaction a,IFocusGroup f){
                b.addSlot(RecipeIngredientRole.INPUT,16,8).setStandardSlotBackground().addItemStack(a.tool());
                b.addSlot(RecipeIngredientRole.INPUT,16,38).setStandardSlotBackground().addItemStack(a.target());
                if(!a.previous().isEmpty())b.addSlot(RecipeIngredientRole.RENDER_ONLY,44,38).setStandardSlotBackground().addItemStack(a.previous())
                        .addRichTooltipCallback((v,t)->t.add(Component.translatable("jei.omnira.world_existing_cluster")));
                b.addSlot(RecipeIngredientRole.OUTPUT,116,38).setStandardSlotBackground().addItemStack(a.result());
                if(!a.remainder().isEmpty())b.addSlot(RecipeIngredientRole.OUTPUT,142,38).setStandardSlotBackground().addItemStack(a.remainder());
            }
            public void draw(Interaction a,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                gui.getRecipeArrow().draw(g,72,38);
                g.fill(12,57,37,59,0xFF90BCC3);g.fill(112,57,137,59,0xFF90BCC3);
                var font=net.minecraft.client.Minecraft.getInstance().font;
                g.drawString(font,Component.translatable("jei.omnira.world_use"),43,12,0x445B65,false);
                g.drawWordWrap(font,Component.translatable("jei.omnira.world_"+a.note()),8,65,154,0x445B65);
            }
        });
    }
    public void registerRecipes(IRecipeRegistration r){r.addRecipes(TYPE,entries());}
    public static List<Interaction> entries(){
        var entries=new ArrayList<Interaction>();
        for(var family:CrystalFamilies.all())for(int stage=0;stage<4;stage++)entries.add(new Interaction(
                new ItemStack(ModItems.ARCANE_DUST.get()),new ItemStack(family[0]),new ItemStack(family[stage+1]),ItemStack.EMPTY,"growth",stage==0?ItemStack.EMPTY:new ItemStack(family[stage])));
        entries.add(action(ModItems.ARCANE_DUST.get(),PhaseContent.CLUSTER.get().asItem(),PhaseContent.BLOCK.get().asItem(),"phase"));
        entries.add(action(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),ModBlocks.CRYSTAL_CASING.get().asItem(),ModBlocks.INFUSED_CRYSTAL_CASING.get().asItem(),"replace"));
        entries.add(action(ModBlocks.SPACE_WARP_POINT.get().asItem(),ModBlocks.FOLD_GATE_FRAME.get().asItem(),ModBlocks.STABLE_FOLD_GATE.get().asItem(),"replace"));
        entries.add(action(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get(),ModBlocks.FOLD_GATE_FRAME.get().asItem(),ModBlocks.UNSTABLE_FOLD_GATE.get().asItem(),"replace"));
        entries.add(action(ModItems.SPACE_MICROCORE.get(),AnchorContent.DORMANT.get().asItem(),AnchorContent.ACTIVE.get().asItem(),"replace"));
        for(var fire:List.of(Items.CAMPFIRE,Items.SOUL_CAMPFIRE))entries.add(action(ModItems.ROUGH_ALCHEMY_CAULDRON.get(),fire,ModItems.PRIMITIVE_REACTION_CAULDRON.get(),"campfire"));
        entries.add(new Interaction(new ItemStack(ModItems.DARK_MICROCORE.get()),new ItemStack(DreamContent.LIGHT_CRYSTAL_CORE.get()),
                new ItemStack(DreamContent.LIGHT_CONDENSATE.get()),new ItemStack(ModItems.LIGHT_MICROCORE.get()),"light"));
        entries.add(action(ModItems.LIGHT_MICROCORE.get(),ModItems.SHADOW_MIST.get(),ModItems.DARK_MICROCORE.get(),"mist"));
        return List.copyOf(entries);
    }
    private static Interaction action(Item a,Item b,Item result,String note){return new Interaction(new ItemStack(a),new ItemStack(b),new ItemStack(result),ItemStack.EMPTY,note);}
}

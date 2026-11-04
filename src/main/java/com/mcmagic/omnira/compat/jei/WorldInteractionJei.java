package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.guidance.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.space.anchor.AnchorContent;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

/** World actions are display entries, not crafting recipes; available with or without Create. */
@JeiPlugin
public final class WorldInteractionJei implements IModPlugin {
    public record Interaction(ItemStack tool,ItemStack target,ItemStack result,ItemStack remainder,String note){
        public RecipeIngredientRole targetRole(){return switch(note){
            case "campfire"->RecipeIngredientRole.CATALYST;
            case "deflection","mist"->RecipeIngredientRole.RENDER_ONLY;
            default->RecipeIngredientRole.INPUT;
        };}
    }
    private static final RecipeType<Interaction> TYPE=RecipeType.create("omnira","world_interaction",Interaction.class);
    public net.minecraft.resources.ResourceLocation getPluginUid(){return net.minecraft.resources.ResourceLocation.parse("omnira:world_interactions");}
    public void registerCategories(IRecipeCategoryRegistration r){
        var gui=r.getJeiHelpers().getGuiHelper();
        r.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<Interaction>(){
            public RecipeType<Interaction> getRecipeType(){return TYPE;}
            public Component getTitle(){return Component.translatable("jei.omnira.world_interaction");}
            public mezz.jei.api.gui.drawable.IDrawable getIcon(){return gui.createDrawableItemStack(new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));}
            public int getWidth(){return 170;}public int getHeight(){return 94;}
            public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,Interaction a,IFocusGroup f){
                boolean guidance=a.note().equals("guidance");
                b.addSlot(guidance||a.note().equals("transmutation")?RecipeIngredientRole.CATALYST:RecipeIngredientRole.INPUT,16,8).setStandardSlotBackground().addItemStack(a.tool());
                boolean retained=a.note().equals("campfire");
                var target=b.addSlot(a.targetRole(),16,38).setStandardSlotBackground().addItemStack(a.target());
                if(retained)target.addRichTooltipCallback((v,t)->t.add(Component.translatable("jei.omnira.world_target_retained")));
                if(guidance)target.addRichTooltipCallback((v,t)->t.add(stageLabel(a.target())));
                if(!a.result().isEmpty()){
                    var output=b.addSlot(RecipeIngredientRole.OUTPUT,116,38).setStandardSlotBackground().addItemStack(a.result());
                    if(guidance)output.addRichTooltipCallback((v,t)->t.add(stageLabel(a.result())));
                }
                if(!a.remainder().isEmpty())b.addSlot(RecipeIngredientRole.OUTPUT,142,38).setStandardSlotBackground().addItemStack(a.remainder());
            }
            public void draw(Interaction a,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.client.gui.GuiGraphics g,double x,double y){
                gui.getRecipeArrow().draw(g,72,38);
                g.fill(12,57,37,59,0xFF90BCC3);g.fill(112,57,137,59,0xFF90BCC3);
                var font=net.minecraft.client.Minecraft.getInstance().font;
                if(a.result().isEmpty())g.drawCenteredString(font,Component.translatable("jei.omnira.world_random_biome"),125,40,0x445B65);
                g.drawString(font,Component.translatable("jei.omnira.world_use"),43,12,0x445B65,false);
                JeiText.wrapped(g,Component.translatable("jei.omnira.world_"+a.note()),8,65,154,27,0x445B65);
            }
            @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tip,Interaction a,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,double x,double y){
                if(x>=8&&x<162&&y>=65&&y<92)tip.add(Component.translatable("jei.omnira.world_"+a.note()));
            }
        });
    }
    public void registerRecipes(IRecipeRegistration r){r.addRecipes(TYPE,entries());}
    public static List<Interaction> entries(){
        var entries=new ArrayList<Interaction>();
        for (var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK.getTagOrEmpty(com.mcmagic.omnira.item.CrystallizedPhilosophersStoneItem.STONES)) {
            var target=block.value().asItem();
            if (target!=Items.AIR) entries.add(action(ModItems.PHILOSOPHERS_STONE.get(),target,Items.RAW_GOLD_BLOCK,"transmutation"));
        }
        entries.add(action(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),DreamContent.SPIRITUAL_CRYSTAL_BLOCK.get().asItem(),ModBlocks.INFUSED_CRYSTAL_CASING.get().asItem(),"replace"));
        entries.add(action(ModBlocks.SPACE_WARP_POINT.get().asItem(),ModBlocks.FOLD_GATE_FRAME.get().asItem(),ModBlocks.STABLE_FOLD_GATE.get().asItem(),"replace"));
        entries.add(action(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get(),ModBlocks.FOLD_GATE_FRAME.get().asItem(),ModBlocks.UNSTABLE_FOLD_GATE.get().asItem(),"replace"));
        entries.add(action(ModItems.SPACE_MICROCORE.get(),AnchorContent.DORMANT.get().asItem(),AnchorContent.ACTIVE.get().asItem(),"replace"));
        for(var fire:List.of(Items.CAMPFIRE,Items.SOUL_CAMPFIRE))entries.add(action(ModItems.ROUGH_ALCHEMY_CAULDRON.get(),fire,ModItems.PRIMITIVE_REACTION_CAULDRON.get(),"campfire"));
        entries.add(new Interaction(new ItemStack(ModItems.DARK_MICROCORE.get()),new ItemStack(DreamContent.LIGHT_CRYSTAL_CORE.get()),
                new ItemStack(DreamContent.LIGHT_CONDENSATE.get()),new ItemStack(ModItems.LIGHT_MICROCORE.get()),"light"));
        entries.add(action(ModItems.LIGHT_MICROCORE.get(),ModItems.SHADOW_MIST.get(),ModItems.DARK_MICROCORE.get(),"mist"));
        entries.add(new Interaction(new ItemStack(com.mcmagic.omnira.myriad.WorldSeedContent.ACTIVE.get()),
                new ItemStack(Items.GRASS_BLOCK),ItemStack.EMPTY,ItemStack.EMPTY,"deflection"));
        entries.add(guidance(ModItems.SPIRITUAL_CRYSTAL.get(),0));
        entries.add(guidance(DreamContent.DREAM_CRYSTAL_BEDROCK.get().asItem(),1));
        entries.add(guidance(ModItems.TIME_WARP_POINT.get(),2));
        entries.add(guidance(ModItems.SPACE_WARP_POINT.get(),2));
        entries.add(guidance(com.mcmagic.omnira.paradox.ParadoxContent.GENESIS.get(),3));
        entries.add(guidance(com.mcmagic.omnira.codex.MyriadCodexContent.ITEM.get(),4));
        return List.copyOf(entries);
    }
    private static Interaction guidance(Item tool,int stage){return new Interaction(new ItemStack(tool),orb(stage),orb(stage+1),ItemStack.EMPTY,"guidance");}
    private static ItemStack orb(int stage){
        var stack=new ItemStack(stage==5?GuidanceContent.UNDERSTANDING_ITEM.get():GuidanceContent.ITEM.get());
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(GuidanceOrbBlock.STAGE,stage));
        return stack;
    }
    private static Component stageLabel(ItemStack stack){
        int stage=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                net.minecraft.world.item.component.BlockItemStateProperties.EMPTY).apply(GuidanceContent.BLOCK.get().defaultBlockState()).getValue(GuidanceOrbBlock.STAGE);
        return Component.translatable("jei.omnira.guidance_stage",stage+1);
    }
    private static Interaction action(Item a,Item b,Item result,String note){return new Interaction(new ItemStack(a),new ItemStack(b),new ItemStack(result),ItemStack.EMPTY,note);}
}

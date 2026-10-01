package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.*;

public final class GuideRecipes {
    public record Slot(int x,int y,List<ItemStack> items,boolean output) {}
    public record Diagram(Component title,List<Slot> slots,List<Component> notes) {}
    private GuideRecipes() {}
    private static Slot slot(int x,int y,ItemStack stack,boolean output) {return new Slot(x,y,stack.isEmpty()?List.of():List.of(stack),output);}
    private static Slot ingredient(int x,int y,Ingredient ingredient,int count) {
        return new Slot(x,y,Arrays.stream(ingredient.getItems()).map(item->item.copyWithCount(count)).toList(),false);
    }
    private static Component mana(int amount) {return Component.translatable("guide.omnira.mana",amount);}
    public static List<Diagram> find(Item item) {
        var level=Minecraft.getInstance().level;
        var result=new ArrayList<Diagram>();
        if(level==null) return result;
        var manager=level.getRecipeManager();
        for(var holder:manager.getAllRecipesFor(ModRecipes.SWORD_SHAPING_TYPE.get())) {
            var recipe=holder.value();if(!recipe.result().is(item))continue;
            int[][] positions={{60,47},{96,83},{60,119},{24,83}};
            var slots=new ArrayList<Slot>();
            for(int i=0;i<4;i++)slots.add(ingredient(positions[i][0],positions[i][1],recipe.offerings().get(i),1));
            slots.add(ingredient(60,83,Ingredient.of(com.mcmagic.omnira.block.entity.PureVesselBlockEntity.WEAPONS),1));
            slots.add(slot(114,136,recipe.result(),true));
            result.add(new Diagram(Component.translatable("guide.omnira.sword_shaping"),slots,List.of(Component.translatable("guide.omnira.sword_shaping_layout"))));
        }
        for(var holder:manager.getAllRecipesFor(RecipeType.CRAFTING)) {
            var recipe=holder.value();var output=recipe.getResultItem(level.registryAccess());
            boolean crystalConversion=item==ModItems.ANALYSIS_CRYSTAL.get()
                    && recipe.getIngredients().stream().anyMatch(ingredient->ingredient.test(new ItemStack(item)));
            if(!output.is(item) && !crystalConversion) continue;
            var slots=new ArrayList<Slot>();var ingredients=recipe.getIngredients();
            int width=recipe instanceof ShapedRecipe shaped?shaped.getWidth():3;
            for(int i=0;i<ingredients.size();i++) slots.add(ingredient(17+(i%width)*18,72+(i/width)*18,ingredients.get(i),1));
            slots.add(slot(108,90,output,true));
            result.add(new Diagram(Component.translatable("container.crafting"),slots,List.of()));
        }
        for(var holder:manager.getAllRecipesFor(RecipeType.SMITHING)) {
            var recipe=holder.value();var output=recipe.getResultItem(level.registryAccess());
            if(!output.is(item)) continue;
            var stacks=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().map(ItemStack::new).toList();
            result.add(new Diagram(Items.SMITHING_TABLE.getDescription(),List.of(
                    new Slot(17,90,stacks.stream().filter(recipe::isTemplateIngredient).toList(),false),
                    new Slot(35,90,stacks.stream().filter(recipe::isBaseIngredient).toList(),false),
                    new Slot(53,90,stacks.stream().filter(recipe::isAdditionIngredient).toList(),false),
                    slot(108,90,output,true)),List.of()));
        }
        for(var holder:manager.getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get())) {
            var recipe=holder.value();
            if(recipe.result().is(item)) result.add(recipe.advanced()
                    ?new Diagram(ModItems.ADVANCED_CONDENSATION_TABLE.get().getDescription(),
                            List.of(ingredient(24,88,recipe.ingredient(),1),slot(104,88,recipe.result(),true)),
                            List.of(mana(recipe.manaCost()),Component.translatable("jei.omnira.advanced_condensation_time")))
                    :new Diagram(ModItems.SIMPLE_CONDENSATION_TABLE.get().getDescription(),
                            List.of(slot(64,88,recipe.result(),true)),List.of(mana(recipe.manaCost()),Component.translatable("guide.omnira.no_input"))));
        }
        for(var holder:manager.getAllRecipesFor(ModRecipes.ANALYSIS_TYPE.get())) {
            var recipe=holder.value();
            var byproduct=recipe.secondaryOutput();
            if(!recipe.result().is(item) && byproduct.maximumOutputs().stream().noneMatch(s->s.is(item))) continue;
            var slots=new ArrayList<Slot>();slots.add(ingredient(24,87,recipe.ingredient(),recipe.count()));
            slots.add(slot(101,72,recipe.result(),true));
            if(byproduct.max()>0) slots.add(new Slot(101,105,byproduct.displayOutputs(),true));
            var notes=new ArrayList<Component>();notes.add(mana(50));
            if(byproduct.min()!=byproduct.max()) notes.add(Component.translatable("jei.omnira.byproduct",byproduct.min(),byproduct.max()));
            if(byproduct.choices().size()>1) notes.add(Component.translatable("jei.omnira.byproduct_choice"));
            if(recipe.inputDamage()>0) notes.add(Component.translatable("jei.omnira.input_damage",recipe.inputDamage()));
            else if(Arrays.stream(recipe.ingredient().getItems()).anyMatch(stack->!stack.getCraftingRemainingItem().isEmpty()))
                notes.add(Component.translatable("jei.omnira.remainder"));
            result.add(new Diagram(ModItems.ANALYSIS_ARTISAN_TABLE.get().getDescription(),slots,notes));
        }
        for(var holder:manager.getAllRecipesFor(ModRecipes.ADVANCED_FORGE_TYPE.get())) {
            var recipe=holder.value();if(!recipe.result().is(item))continue;
            var slots=new ArrayList<Slot>();
            for(int i=0;i<6;i++)slots.add(ingredient(24+(i%3)*22,62+(i/3)*22,recipe.slots().get(i),1));
            for(int i=0;i<3;i++)slots.add(ingredient(24+i*22,118,recipe.slots().get(6+i),1));
            slots.add(slot(108,84,recipe.result(),true));
            result.add(new Diagram(ModItems.ADVANCED_FORGE.get().getDescription(),slots,
                    List.of(Component.translatable("jei.omnira.forge_nodes"))));
        }
        for(var holder:manager.getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get())) {
            var recipe=holder.value();if(!recipe.result().is(item)) continue;
            int[][] positions={{80,53},{96,81},{80,109},{48,109},{32,81},{48,53},{64,81}};
            var slots=new ArrayList<Slot>();
            var displaySlots=recipe.displaySlots();
            for(int i=0;i<7;i++) slots.add(ingredient(positions[i][0],positions[i][1],displaySlots.get(i),1));
            slots.add(slot(114,127,recipe.result(),true));
            result.add(new Diagram(ModItems.ARCANE_ASSEMBLY_TABLE.get().getDescription(),slots,
                    List.of(mana(50),Component.translatable("jei.omnira.assembly_progress"),Component.translatable("jei.omnira.assembly_center"))));
        }
        if(item==com.mcmagic.omnira.aggregation.AggregationContent.ITEM.get()) {
            var slots=new ArrayList<Slot>();
            for(int i=0;i<9;i++) {
                Ingredient input=i==4?Ingredient.of(ModItems.CRYSTAL_PROCESSING_TABLE.get()):
                    i==1||i==7?Ingredient.of(DreamContent.EXCITED_SPATIAL_CRYSTAL.get(),com.mcmagic.omnira.aggregation.AggregationContent.SPATIAL_BLOCK.get()):
                    i==3||i==5?Ingredient.of(DreamContent.LIVING_TEMPORAL_SILT.get(),TimeNatureContent.LIVING_SILT.get()):Ingredient.of(ModBlocks.INFUSED_CRYSTAL_CASING.get());
                slots.add(ingredient(24+i%3*20,55+i/3*20,input,1));
            }
            slots.add(slot(44,125,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()),false));
            slots.add(slot(108,75,new ItemStack(item),true));
            result.add(new Diagram(item.getDescription(),slots,List.of(Component.translatable("jei.omnira.aggregation.vertical"),Component.translatable("jei.omnira.aggregation.activate"))));
        }
        if(item==ModItems.LOW_TIER_MAGIC_CRYSTAL.get()) {
            var container=new SimpleContainer(9);
            container.setItem(0,new ItemStack(ModItems.AIR_MICROCORE.get()));container.setItem(1,new ItemStack(ModItems.AIR_MICROCORE.get()));
            container.setItem(6,new ItemStack(ModItems.SPELL_INK.get()));container.setItem(7,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            int[][] positions={{43,48},{85,48},{21,86},{43,124},{85,124},{107,86},{50,80},{78,80},{64,104}};
            var slots=new ArrayList<Slot>();
            for(int i=0;i<8;i++) slots.add(slot(positions[i][0],positions[i][1],container.getItem(i),false));
            slots.add(slot(positions[8][0],positions[8][1],CrystalProcessingTableMenu.previewCrystal(container),true));
            result.add(new Diagram(ModItems.CRYSTAL_PROCESSING_TABLE.get().getDescription(),slots,
                    List.of(Component.translatable("guide.omnira.writing_example"),mana(20),Component.translatable("guide.omnira.ink_options"))));
        }
        return List.copyOf(result);
    }
}

package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.recipe.*;
import com.mcmagic.omnira.forging.AdvancedForgeRecipe;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_recipe_audit")
@PrefixGameTestTemplate(false)
public final class RecipeAuditGameTests {
    private static Recipe<?> recipe(GameTestHelper h,String id){return h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:"+id)).orElseThrow().value();}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:"+id)));}
    private static SimpleContainer slots(List<Ingredient> ingredients){
        var container=new SimpleContainer(ingredients.size());
        for(int i=0;i<ingredients.size();i++)if(!ingredients.get(i).isEmpty())container.setItem(i,ingredients.get(i).getItems()[0].copy());
        return container;
    }
    private static void fill(ItemStack stack){stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));}

    @GameTest(template="spell_arena")
    public static void destructiveInputs(GameTestHelper h){
        for(var id:List.of("assembly/void_crystal_ball","assembly/basic_stacking_upgrade")){
            var r=(AssemblyRecipe)recipe(h,id);var c=slots(r.slots());
            h.assertTrue(r.matches(new AssemblyRecipe.Input(c),h.getLevel()),"Empty source rejected: "+id);
            fill(c.getItem(id.contains("void")?0:6));
            h.assertTrue(!r.matches(new AssemblyRecipe.Input(c),h.getLevel()),"Stored inventory consumed: "+id);
        }
        var forge=(AdvancedForgeRecipe)recipe(h,"advanced_assembly_table/cruise_orb");var c=slots(forge.slots());
        h.assertTrue(forge.matches(new AdvancedForgeRecipe.Input(c),h.getLevel()),"Empty cruise ingredients rejected");
        for(int i=0;i<9;i++){
            var clean=c.getItem(i).copy();if(clean.isEmpty())continue;fill(c.getItem(i));
            h.assertTrue(!forge.matches(new AdvancedForgeRecipe.Input(c),h.getLevel()),"Loaded forge material accepted");c.setItem(i,clean);
        }
        var condensation=(CondensationRecipe)recipe(h,"condensation/pure_vessel");
        var source=condensation.ingredient().getItems()[0].copyWithCount(condensation.inputCount());
        h.assertTrue(condensation.matches(new SingleRecipeInput(source),h.getLevel()),"Empty orb condensation rejected");fill(source);
        h.assertTrue(!condensation.matches(new SingleRecipeInput(source),h.getLevel()),"Loaded orb condensation accepted");
        for(var id:List.of("assembly/resonant_crystal_ball","assembly/resonant_liquid_crystal_ball")){
            var upgrade=(AssemblyRecipe)recipe(h,id);var input=slots(upgrade.slots());
            var tag=new net.minecraft.nbt.CompoundTag();tag.putString("TestContents","retained");
            input.getItem(6).set(DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            h.assertTrue(upgrade.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Preserving orb upgrade rejected");
            h.assertTrue(upgrade.assemble(new AssemblyRecipe.Input(input),h.getLevel().registryAccess()).get(DataComponents.BLOCK_ENTITY_DATA)
                    .equals(input.getItem(6).get(DataComponents.BLOCK_ENTITY_DATA)),"Orb data lost");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void pickaxeData(GameTestHelper h){
        var source=item("crystal_pickaxe");source.setDamageValue(10);source.set(DataComponents.CUSTOM_NAME,Component.literal("Remember me"));
        var ench=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        source.enchant(ench,3);
        var r=(AssemblyRecipe)recipe(h,"assembly/infused_crystal_pickaxe");var c=slots(r.slots());c.setItem(6,source);
        h.assertTrue(r.matches(new AssemblyRecipe.Input(c),h.getLevel()),"Named pickaxe rejected");
        var result=r.assemble(new AssemblyRecipe.Input(c),h.getLevel().registryAccess());
        var craft=(CraftingRecipe)recipe(h,"infused_crystal_pickaxe");
        var stacks=craft.getIngredients().stream().map(i->i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy()).toList();
        var input=new ArrayList<>(stacks);input.set(4,source);
        h.assertTrue(craft.matches(CraftingInput.of(3,3,input),h.getLevel()),"Crafting upgrade rejected");
        var crafted=craft.assemble(CraftingInput.of(3,3,input),h.getLevel().registryAccess());
        for(var output:List.of(result,crafted)){
            h.assertTrue(output.is(ModItems.INFUSED_CRYSTAL_PICKAXE.get())&&output.getDamageValue()==10,"Tool or damage lost");
            h.assertTrue(output.getComponentsPatch().equals(source.getComponentsPatch()),"Tool data lost");
            h.assertTrue(output.get(DataComponents.TOOL).equals(new ItemStack(ModItems.INFUSED_CRYSTAL_PICKAXE.get()).get(DataComponents.TOOL)),"Old tool defaults copied");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void timelinesAndCatalogue(GameTestHelper h){
        h.assertTrue(com.mcmagic.omnira.time.SundialTimeline.choices(item("deflection_crystal")).stream().anyMatch(s->s.is(ModItems.SPIRITUAL_CRYSTAL.get())),"Deflection material omitted");
        var stairs=com.mcmagic.omnira.time.SundialTimeline.choices(item("dream_crystal_stairs"));
        h.assertTrue(stairs.stream().anyMatch(s->s.is(item("spatial_crystal_stairs").getItem())),"Crystal stairs omitted");
        h.assertTrue(stairs.stream().noneMatch(s->s.is(item("spatial_crystal_block").getItem())),"Shape categories mixed");
        var catalogue=com.mcmagic.omnira.alchemy.catalysis.CatalysisCatalogue.build(h.getLevel());
        h.assertTrue(catalogue.stream().anyMatch(r->r.seed().test(new ItemStack(Items.WHEAT_SEEDS))&&r.substrate().test(new ItemStack(Items.FARMLAND))&&r.result().is(Items.WHEAT)),"Farmland crop missing");
        h.assertTrue(catalogue.stream().anyMatch(r->r.seed().test(new ItemStack(Items.OAK_SAPLING))&&r.result().is(Items.OAK_LOG)),"Tree catalogue missing");
        h.succeed();
    }
}

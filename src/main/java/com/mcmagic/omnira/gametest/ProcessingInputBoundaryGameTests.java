package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.recipe.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.reversal.ReversalRecipe;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;

@GameTestHolder("omnira_processing_boundaries")
@PrefixGameTestTemplate(false)
public final class ProcessingInputBoundaryGameTests {
    private static List<ItemStack> loaded(ItemStack clean){
        var inventory=clean.copy();inventory.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        var machine=clean.copy();var tag=new CompoundTag();tag.putString("Contents","retained");machine.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));
        var captured=clean.copy();captured.set(ModDataComponents.BOTTLE_CAPTURE,UUID.randomUUID());
        var bundle=clean.copy();bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(new ItemStack(Items.DIAMOND))));
        return List.of(inventory,machine,captured,bundle);
    }
    private static List<ItemStack> portable(GameTestHelper h){
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());bottle.set(ModDataComponents.BOTTLE_CAPTURE,UUID.randomUUID());
        var upgrades=new ItemStackHandler(3);upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        var tag=new CompoundTag();tag.put("Upgrades",upgrades.serializeNBT(h.getLevel().registryAccess()));
        var orb=new ItemStack(ModItems.CRYSTAL_BALL.get());orb.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));
        var frame=new ItemStack(com.mcmagic.omnira.myriad.MiniatureWorldContent.ITEM.get());
        frame.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(new CompoundTag()));
        return List.of(bottle,orb,frame);
    }
    @GameTest(template="spell_arena") public static void destructiveRecipeBoundaries(GameTestHelper h){
        var ingredient=Ingredient.of(Items.IRON_INGOT);
        List<Recipe<SingleRecipeInput>> recipes=List.of(
                new AnalysisRecipe(ingredient,1,new ItemStack(Items.COAL),0,0),
                new ReversalRecipe(ingredient,1,List.of(new ItemStack(Items.COAL))),
                new ReversalRecipe(ingredient,1,List.of(new ItemStack(Items.COAL))).asInfusion(),
                new StarlightConversionRecipe(ingredient,new ItemStack(Items.COAL)),
                new VesselConversionRecipe(ingredient,new ItemStack(Items.COAL)));
        var clean=new ItemStack(Items.IRON_INGOT);clean.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Allowed metadata"));
        for(var recipe:recipes){
            h.assertTrue(recipe.matches(new SingleRecipeInput(clean),h.getLevel()),"Clean named material rejected");
            for(var stack:loaded(clean))h.assertTrue(!recipe.matches(new SingleRecipeInput(stack),h.getLevel()),"Destructive recipe accepts loaded material: "+recipe.getType());
        }
        h.assertTrue(RecipeInputSafety.empty(new ItemStack(Items.WATER_BUCKET))&&RecipeInputSafety.empty(new ItemStack(Items.LAVA_BUCKET)),"Fluid bucket reagents rejected");
        h.assertTrue(RecipeInputSafety.empty(new ItemStack(Items.BUNDLE)),"Empty bundle rejected");
        var vehicle=new ItemStack(ModItems.CRUISE_ORB.get());vehicle.set(DataComponents.CUSTOM_DATA,CustomData.of(new CompoundTag()));
        h.assertTrue(!RecipeInputSafety.empty(vehicle),"Serialized cruise orb accepted as destructive ingredient");
        var memory=new ItemStack(ModItems.MEMORY_CUBE.get());
        var reversal=new ReversalRecipe(Ingredient.of(memory.getItem()),1,List.of(memory.copy()));
        for(var stack:loaded(memory)){
            h.assertTrue(reversal.matches(new SingleRecipeInput(stack),h.getLevel()),"Content-preserving memory reversal rejected");
            var output=reversal.outputs(stack).getFirst();
            h.assertTrue(Objects.equals(stack.get(DataComponents.CONTAINER),output.get(DataComponents.CONTAINER))
                    &&Objects.equals(stack.get(DataComponents.BLOCK_ENTITY_DATA),output.get(DataComponents.BLOCK_ENTITY_DATA))
                    &&Objects.equals(stack.get(DataComponents.BUNDLE_CONTENTS),output.get(DataComponents.BUNDLE_CONTENTS))
                    &&Objects.equals(stack.get(ModDataComponents.BOTTLE_CAPTURE),output.get(ModDataComponents.BOTTLE_CAPTURE)),"Memory reversal loses contents");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void intentionalSwordShapingContentLossRemainsAllowed(GameTestHelper h){
        var weapon=new ItemStack(Items.IRON_SWORD);
        var recipe=new SwordShapingRecipe(Collections.nCopies(4,Ingredient.of(Items.DIAMOND)),new ItemStack(ModItems.CRYSTALLIZED_NAIL.get()));
        for(var loaded:loaded(weapon)){
            var input=new SwordShapingRecipe.Input(loaded,Collections.nCopies(4,new ItemStack(Items.DIAMOND)));
            h.assertTrue(recipe.matches(input,h.getLevel()),"Intentional loaded weapon shaping rejected");
            h.assertTrue(RecipeInputSafety.empty(recipe.assemble(input,h.getLevel().registryAccess())),"Shaping incorrectly retains source contents");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void createPressingRejectsLoadedReagents(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create")){
            var slots=new net.minecraft.world.SimpleContainer(7);slots.setItem(6,new ItemStack(Items.IRON_INGOT));
            h.assertTrue(com.mcmagic.omnira.compat.CreatePressing.find(h.getLevel(),slots)!=null,"Clean Create pressing missing");
            for(var loaded:loaded(new ItemStack(Items.IRON_INGOT))){
                slots.setItem(6,loaded);h.assertTrue(com.mcmagic.omnira.compat.CreatePressing.find(h.getLevel(),slots)==null,"Create pressing consumes loaded reagent");
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionAssignmentSkipsOnlyLoadedIngredients(GameTestHelper h){
        var recipe=new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(Items.IRON_INGOT),1)),FluidStack.EMPTY,Ingredient.EMPTY,List.of(new ItemStack(Items.COAL)),FluidStack.EMPTY,2000);
        var inventory=new ItemStackHandler(3);var input=new ReactionRecipe.Input(inventory,new FluidTank(4000),support->true);
        for(var loaded:loaded(new ItemStack(Items.IRON_INGOT))){
            inventory.setStackInSlot(0,loaded);inventory.setStackInSlot(1,ItemStack.EMPTY);
            h.assertTrue(!recipe.matches(input,h.getLevel()),"Reaction consumes loaded matching material");
            inventory.setStackInSlot(1,new ItemStack(Items.IRON_INGOT));
            h.assertTrue(recipe.matches(input,h.getLevel())&&recipe.assignment(input)[0]==1,"Loaded unrelated slot blocks clean reagent");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void physicalNodesRefuseAndAllowLegacyRemoval(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var player=AdvancedForgeGameTests.player(h,pos);
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.ADVANCED_FORGE.get().defaultBlockState());
        var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(pos);
        var ring=new AggregationRingBlockEntity(pos.above(),AggregationContent.BLOCK.get().defaultBlockState());ring.setLevel(h.getLevel());
        for(var stack:portable(h)){
            player.setItemInHand(InteractionHand.MAIN_HAND,stack.copy());forge.node(player,InteractionHand.MAIN_HAND,6);
            h.assertTrue(forge.getItem(6).isEmpty()&&ItemStack.matches(player.getMainHandItem(),stack),"Physical forge node accepts or loses portable contents");
            ring.interactSlot(player,InteractionHand.MAIN_HAND,5);
            h.assertTrue(ring.getItem(5).isEmpty()&&ItemStack.matches(player.getMainHandItem(),stack),"Physical aggregation node accepts or loses portable contents");
            ring.setItem(5,stack.copy());player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);ring.interactSlot(player,InteractionHand.MAIN_HAND,5);
            h.assertTrue(ring.getItem(5).isEmpty()&&ItemStack.matches(player.getMainHandItem(),stack),"Legacy aggregation contents cannot be retrieved");
            forge.setItem(6,stack.copy());player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.getInventory().clearContent();forge.node(player,InteractionHand.MAIN_HAND,6);
            h.assertTrue(forge.getItem(6).isEmpty()&&player.getInventory().items.stream().anyMatch(s->ItemStack.matches(s,stack)),"Legacy forge contents lost on retrieval");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_INGOT,2));forge.node(player,InteractionHand.MAIN_HAND,6);
        h.assertTrue(forge.getItem(6).is(Items.IRON_INGOT)&&player.getMainHandItem().getCount()==1,"Normal forge node input rejected");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionCustomSwapAndAutomation(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(h.getLevel(),pos);var player=AdvancedForgeGameTests.player(h,pos);
        var menu=new ReactionPoolMenu(1,player.getInventory(),pool);
        for(var stack:portable(h)){
            pool.items.setStackInSlot(0,new ItemStack(Items.DIRT));menu.setCarried(stack.copy());menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(pool.items.getStackInSlot(0).is(Items.DIRT)&&ItemStack.matches(menu.getCarried(),stack),"Custom swap accepts or loses portable contents");
            h.assertTrue(!pool.canSetItem(0,stack)&&ItemStack.matches(pool.items.insertItem(1,stack.copy(),true),stack)
                    &&ItemStack.matches(pool.items.insertItem(1,stack.copy(),false),stack),"Pool handler or simulation accepts portable contents");
            menu.setCarried(ItemStack.EMPTY);player.getInventory().setItem(0,stack.copy());menu.clicked(0,0,ClickType.SWAP,player);menu.clicked(40,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(pool.items.getStackInSlot(0).is(Items.DIRT)&&ItemStack.matches(player.getInventory().getItem(0),stack),"Pool hotbar/shift bypass");
            pool.items.setStackInSlot(0,stack.copy());menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(pool.items.getStackInSlot(0).isEmpty()&&ItemStack.matches(menu.getCarried(),stack),"Legacy pool contents cannot be removed");
        }
        pool.items.setStackInSlot(0,new ItemStack(Items.DIRT));menu.setCarried(new ItemStack(Items.STONE));menu.clicked(0,0,ClickType.PICKUP,player);
        h.assertTrue(pool.items.getStackInSlot(0).is(Items.STONE)&&menu.getCarried().is(Items.DIRT),"Normal custom swap rejected");h.succeed();
    }
    @GameTest(template="spell_arena") public static void overriddenStandardSlotsRemainProtected(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.ADVANCED_FORGE.get().defaultBlockState());
        var forge=(AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(pos);var player=AdvancedForgeGameTests.player(h,pos);
        var menu=new AdvancedForgeMenu(1,player.getInventory(),forge);
        for(var stack:portable(h)){
            menu.setCarried(stack.copy());menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(forge.isEmpty()&&ItemStack.matches(menu.getCarried(),stack),"Overridden slot bypasses standard pickup protection");
            menu.setCarried(ItemStack.EMPTY);player.getInventory().setItem(0,stack.copy());menu.clicked(0,0,ClickType.SWAP,player);menu.clicked(34,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(forge.isEmpty()&&ItemStack.matches(player.getInventory().getItem(0),stack),"Overridden slot bypasses hotbar/shift protection");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void assemblyAndAnvilEmptySlotsRemainProtected(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var player=AdvancedForgeGameTests.player(h,pos);
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState());
        var table=((com.mcmagic.omnira.block.entity.AssemblyAccess)h.getLevel().getBlockEntity(pos)).assembly();
        var assembly=new com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu(1,player.getInventory(),table);
        for(var stack:portable(h)){
            assembly.setCarried(stack.copy());assembly.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(table.isEmpty()&&ItemStack.matches(assembly.getCarried(),stack),"Assembly empty slot accepts portable contents");
            assembly.setCarried(ItemStack.EMPTY);player.getInventory().setItem(0,stack.copy());assembly.clicked(0,0,ClickType.SWAP,player);assembly.clicked(34,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(table.isEmpty()&&ItemStack.matches(player.getInventory().getItem(0),stack),"Assembly hotbar/shift bypass");
        }
        for(int index:new int[]{2,5}){
            h.getLevel().setBlockAndUpdate(pos,com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(index).get().defaultBlockState());
            var anvil=com.mcmagic.omnira.workstation.CrystalWorkstations.work(h.getLevel().getBlockEntity(pos));
            var menu=new com.mcmagic.omnira.workstation.CrystalWorkstationMenu(1,player.getInventory(),anvil);
            for(var stack:portable(h)){
                menu.setCarried(stack.copy());menu.clicked(0,0,ClickType.PICKUP,player);menu.clicked(1,0,ClickType.PICKUP,player);
                h.assertTrue(anvil.isEmpty()&&ItemStack.matches(menu.getCarried(),stack),"Anvil empty slot accepts portable contents");
                menu.setCarried(ItemStack.EMPTY);player.getInventory().setItem(0,stack.copy());menu.clicked(0,0,ClickType.SWAP,player);menu.clicked(30,0,ClickType.QUICK_MOVE,player);
                h.assertTrue(anvil.isEmpty()&&ItemStack.matches(player.getInventory().getItem(0),stack),"Anvil hotbar/shift bypass");
            }
        }
        h.succeed();
    }
}

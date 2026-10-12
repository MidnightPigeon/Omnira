package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.menu.*;
import com.mcmagic.omnira.recipe.RecipeControl;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.List;

@GameTestHolder("omnira_recipe_control")
@PrefixGameTestTemplate(false)
public final class RecipeControlGameTests {
    private static ItemStack plate(GameTestHelper h,boolean white,Item sample){
        var stack=new ItemStack(ModItems.RECIPE_CONTROL_UPGRADE.get());
        var data=OrbUpgrades.settings(stack);data.putBoolean("Whitelist",white);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        if(sample!=Items.AIR)OrbUpgrades.setFilter(stack,8,new ItemStack(sample),h.getLevel().registryAccess());return stack;
    }
    private static ReactionPoolBlockEntity pool(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");return ReactionPoolBlock.controller(h.getLevel(),pos);
    }
    private static CatalysisBlockEntity chamber(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,2,4));h.assertTrue(CatalysisBlock.place(h.getLevel(),pos,Direction.NORTH),"Chamber placement failed");return CatalysisBlock.controller(h.getLevel(),pos);
    }
    @GameTest(template="spell_arena") public static void nineGhostSamplesDoNotMoveItems(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);var plate=plate(h,false,Items.AIR);player.getInventory().setItem(0,plate);
        var menu=new OrbUpgradeMenu(1,player.getInventory(),0,false);
        h.assertTrue(menu.recipeControl&&menu.slots.size()==45&&menu.stillValid(player),"Wrong ghost grid");
        menu.setCarried(new ItemStack(Items.DIAMOND,12));menu.clicked(8,0,ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().getCount()==12&&menu.getSlot(8).getItem().getCount()==1,"Sample consumed items");
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));menu.clicked(8,1,ClickType.PICKUP,player);
        h.assertTrue(menu.getSlot(8).getItem().is(Items.WATER_BUCKET)&&menu.getCarried().is(Items.WATER_BUCKET),"Replacement lost item");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(8,0,ClickType.PICKUP,player);
        h.assertTrue(menu.getSlot(8).getItem().isEmpty(),"Empty click did not clear");
        menu.clicked(36,0,ClickType.PICKUP,player);menu.clicked(9,0,ClickType.SWAP,player);
        h.assertTrue(player.getInventory().getItem(0)==plate&&menu.getCarried().isEmpty(),"Bound plate moved");
        h.assertTrue(menu.clickMenuButton(player,0)&&!menu.clickMenuButton(player,1),"Recipe plate exposes directions");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void anyPossibleProductAndFluidSamples(GameTestHelper h){
        var upgrades=new ItemStackHandler(1);var products=List.of(new ItemStack(Items.STONE),new ItemStack(Items.DIAMOND));
        upgrades.setStackInSlot(0,plate(h,true,Items.DIAMOND));
        h.assertTrue(RecipeControl.allows(h.getLevel(),upgrades,products,FluidStack.EMPTY),"Whitelist requires all results");
        upgrades.setStackInSlot(0,plate(h,false,Items.DIAMOND));
        h.assertTrue(!RecipeControl.allows(h.getLevel(),upgrades,products,FluidStack.EMPTY),"Blacklist ignored possible result");
        upgrades.setStackInSlot(0,plate(h,true,Items.LAVA_BUCKET));
        h.assertTrue(RecipeControl.allows(h.getLevel(),upgrades,List.of(),new FluidStack(Fluids.LAVA,100)),"Fluid sample mismatch");
        upgrades.setStackInSlot(0,plate(h,true,Items.AIR));h.assertTrue(!RecipeControl.allows(h.getLevel(),upgrades,products,FluidStack.EMPTY),"Empty whitelist allows work");
        upgrades.setStackInSlot(0,plate(h,false,Items.AIR));h.assertTrue(RecipeControl.allows(h.getLevel(),upgrades,products,FluidStack.EMPTY),"Empty blacklist blocks work");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionPausesBeforeDebiting(GameTestHelper h){
        var pool=pool(h);pool.items.setStackInSlot(0,new ItemStack(Items.STONE));pool.manualFluid.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        var recipe=new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(Items.STONE),1)),new FluidStack(Fluids.WATER,1000),Ingredient.EMPTY,List.of(new ItemStack(Items.DIAMOND)),FluidStack.EMPTY,2000);
        pool.upgrades.setStackInSlot(3,plate(h,false,Items.DIAMOND));
        h.assertTrue(!pool.start(recipe)&&pool.items.getStackInSlot(0).is(Items.STONE)&&pool.fluids.total()==1000,"Blocked recipe consumed materials");
        pool.upgrades.setStackInSlot(3,plate(h,true,Items.DIAMOND));h.assertTrue(pool.start(recipe),"Allowed recipe failed");pool.advance(200);
        pool.upgrades.setStackInSlot(3,plate(h,false,Items.DIAMOND));pool.advance(1800);
        h.assertTrue(pool.reacting()&&pool.fluids.total()==900,"Paused recipe advanced or lost liquid");
        pool.upgrades.setStackInSlot(3,ItemStack.EMPTY);pool.advance(1800);
        h.assertTrue(!pool.reacting()&&pool.fluids.total()==0&&pool.items.getStackInSlot(0).is(Items.DIAMOND),"Recipe did not resume");h.succeed();
    }
    @GameTest(template="spell_arena") public static void catalysisFiltersNativeLootAndPauses(GameTestHelper h){
        var be=chamber(h);be.items.setStackInSlot(0,new ItemStack(Items.DIRT));be.items.setStackInSlot(1,new ItemStack(Items.WHEAT_SEEDS));be.tank.setFluid(new FluidStack(Fluids.WATER,4000));
        be.upgrades.setStackInSlot(2,plate(h,false,Items.WHEAT));for(int i=0;i<200;i++)be.tick();
        h.assertTrue(be.stage()==1&&be.tank.getFluidAmount()==4000&&!be.catalyzing(),"Blocked native crop advanced");
        be.upgrades.setStackInSlot(2,plate(h,true,Items.WHEAT));for(int i=0;i<200;i++)be.tick();
        h.assertTrue(be.stage()==2&&be.tank.getFluidAmount()==3750,"Whitelist crop did not grow");
        be.items.setStackInSlot(1,new ItemStack(Items.OAK_SAPLING));be.upgrades.setStackInSlot(2,plate(h,true,Items.OAK_LOG));
        for(int i=0;i<200;i++)be.tick();h.assertTrue(be.stage()==1&&be.tank.getFluidAmount()==3500,"Tree trunk provider not matched");
        be.items.setStackInSlot(1,new ItemStack(Items.POTATO));
        var possible=CatalysisProducts.possible(h.getLevel(),be.plan(),new ItemStack(Items.POTATO));
        h.assertTrue(possible.stream().anyMatch(s->s.is(Items.POISONOUS_POTATO)),"Rare crop result omitted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void olderUpgradeInventoriesExpandWithoutLoss(GameTestHelper h){
        var pool=pool(h);var old=new ItemStackHandler(3);old.setStackInSlot(2,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()));
        var tag=pool.saveWithFullMetadata(h.getLevel().registryAccess());tag.put("Upgrades",old.serializeNBT(h.getLevel().registryAccess()));pool.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(pool.upgrades.getSlots()==4&&pool.capacity()==160000&&pool.upgrades.getStackInSlot(2).is(ModItems.INFUSED_STACKING_UPGRADE.get()),"Pool migration lost upgrade");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var poolMenu=new ReactionPoolMenu(1,player.getInventory(),pool);
        h.assertTrue(poolMenu.slots.size()==49&&poolMenu.getSlot(12).getContainerSlot()==3,"Pool menu slot count wrong");
        var be=new CatalysisBlockEntity(pool.getBlockPos().offset(4,0,0),CatalysisContent.BLOCK.get().defaultBlockState());be.setLevel(h.getLevel());old=new ItemStackHandler(2);old.setStackInSlot(1,new ItemStack(ModItems.OUTPUT_UPGRADE.get()));
        tag=be.saveWithFullMetadata(h.getLevel().registryAccess());tag.put("Upgrades",old.serializeNBT(h.getLevel().registryAccess()));be.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(be.upgrades.getSlots()==3&&be.upgrades.getStackInSlot(1).is(ModItems.OUTPUT_UPGRADE.get())&&new CatalysisMenu(2,player.getInventory(),be).slots.size()==42,"Chamber migration or menu failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void orbProductionAndStarSlots(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,3,4));h.getLevel().setBlockAndUpdate(pos,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var liquid=(LiquidCrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);liquid.upgrades.setStackInSlot(0,new ItemStack(ModItems.LAVA_PRODUCTION_UPGRADE.get()));liquid.upgrades.setStackInSlot(1,plate(h,false,Items.LAVA_BUCKET));
        OrbAutomation.tick(liquid);h.assertTrue(liquid.tank.isEmpty(),"Filtered lava produced");liquid.upgrades.setStackInSlot(1,plate(h,true,Items.LAVA_BUCKET));OrbAutomation.tick(liquid);
        h.assertTrue(liquid.tank.getFluidAmount()==100,"Permitted lava stopped");
        var starBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("omnira:wandering_star_cluster_orb"));
        pos=pos.offset(3,0,0);h.getLevel().setBlockAndUpdate(pos,starBlock.defaultBlockState());var star=(com.mcmagic.omnira.space.sea.StarClusterOrbEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(star.upgrades.insertItem(0,plate(h,false,com.mcmagic.omnira.space.sea.StarlightContent.DUST_ITEM.get()),false).isEmpty()&&star.upgrades.isItemValid(1,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get())),"Star plate compatibility wrong");
        star.setItem(0,new ItemStack(ModItems.PARADOX_DUST.get(),8));for(int i=0;i<200;i++)star.serverTick();
        h.assertTrue(star.getItem(0).is(ModItems.PARADOX_DUST.get())&&star.progress()==0,"Blocked conversion ran");star.upgrades.setStackInSlot(0,ItemStack.EMPTY);
        for(int i=0;i<200;i++)star.serverTick();h.assertTrue(star.getItem(0).is(com.mcmagic.omnira.space.sea.StarlightContent.DUST_ITEM.get())&&star.getItem(0).getCount()==8,"Unblocked conversion failed");h.succeed();
    }
    private RecipeControlGameTests(){}
}

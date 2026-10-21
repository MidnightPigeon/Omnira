package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.myriad.*;
import com.mcmagic.omnira.paradox.ParadoxContent;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.DreamRitual;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_world_formation")
@PrefixGameTestTemplate(false)
public final class WorldFormationGameTests {
    private static RitualEnergyCoreBlockEntity setup(GameTestHelper h,int rotation) {
        var pos=h.absolutePos(new BlockPos(5,3,5)); var level=h.getLevel();
        level.setBlockAndUpdate(pos,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get().defaultBlockState());
        for(int i=0;i<8;i++) {
            var offset=DreamRitual.ANCHORS[i];var at=pos.offset(offset[0],0,offset[1]);
            level.setBlockAndUpdate(at,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            ((CrystalPedestalBlockEntity)level.getBlockEntity(at)).setItem(0,new ItemStack(WorldComponent.values()[(rotation+7-i)%8].item.get()));
        }
        return (RitualEnergyCoreBlockEntity)level.getBlockEntity(pos);
    }
    private static net.minecraft.world.entity.player.Player player(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WorldSeedContent.ACTIVE.get(),2));return player;
    }
    private static CrystalPedestalBlockEntity pedestal(GameTestHelper h,RitualEnergyCoreBlockEntity core,int index) {
        var offset=DreamRitual.ANCHORS[index];return (CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(core.getBlockPos().offset(offset[0],0,offset[1]));
    }
    private static int drops(GameTestHelper h,RitualEnergyCoreBlockEntity core,Item item) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(core.getBlockPos()).inflate(5)).stream()
                .filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }
    @GameTest(template="spell_arena") public static void arbitraryPlacementFixedPresentationAndSingleCompletion(GameTestHelper h) {
        var core=setup(h,3);var player=player(h);var ritual=core.worldForming;
        h.assertTrue(ritual.activate(player,InteractionHand.MAIN_HAND)&&player.getMainHandItem().getCount()==1,"Unordered ritual rejected or wrong seed debit");
        h.assertTrue(!ritual.activate(player,InteractionHand.MAIN_HAND)&&player.getMainHandItem().getCount()==1,"Repeated activation consumed another seed");
        for(var aspect:WorldComponent.values())h.assertTrue(pedestal(h,core,ritual.anchor(aspect.ordinal())).getItem(0).is(aspect.item.get()),"Presentation used anchor order instead of component order");
        for(int i=0;i<WorldFormingRitual.TOTAL_TICKS-1;i++)ritual.tick();
        h.assertTrue(ritual.active()&&ritual.absorbedCount()==8,"Absorbed offerings not retained until final formation");
        ritual.tick();
        for(int i=0;i<100;i++)ritual.tick();
        h.assertTrue(!ritual.active()&&drops(h,core,MiniatureWorldContent.ITEM.get())==1,"Missing or duplicated framework");
        for(int i=0;i<8;i++)h.assertTrue(pedestal(h,core,i).getItem(0).isEmpty(),"Component not consumed at completion");
        h.assertTrue(drops(h,core,WorldSeedContent.ACTIVE.get())==0,"Completed ritual refunded its activation seed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void invalidOfferingsAndBasicCoreDoNotConsumeSeed(GameTestHelper h) {
        var core=setup(h,0);var player=player(h);var saved=pedestal(h,core,0).getItem(0).copy();
        pedestal(h,core,0).setItem(0,pedestal(h,core,1).getItem(0).copy());
        h.assertTrue(!core.worldForming.activate(player,InteractionHand.MAIN_HAND)&&player.getMainHandItem().getCount()==2,"Duplicate component accepted or seed consumed");
        pedestal(h,core,0).setItem(0,saved);
        h.getLevel().setBlockAndUpdate(core.getBlockPos(),ModBlocks.RITUAL_ENERGY_CORE.get().defaultBlockState());
        var basic=(RitualEnergyCoreBlockEntity)h.getLevel().getBlockEntity(core.getBlockPos());
        h.assertTrue(!basic.worldForming.activate(player,InteractionHand.MAIN_HAND)&&player.getMainHandItem().getCount()==2,"Basic core accepted endgame ritual");h.succeed();
    }
    @GameTest(template="spell_arena") public static void interruptionRefundsSeedAndRetainsComponents(GameTestHelper h) {
        var core=setup(h,2);h.assertTrue(core.worldForming.activate(player(h),InteractionHand.MAIN_HAND),"Activation failed");
        for(int i=0;i<95;i++)core.worldForming.tick();
        int index=core.worldForming.anchor(WorldComponent.LIFE.ordinal());
        var removed=pedestal(h,core,index).removeItem(0,1);pedestal(h,core,index).setChanged();
        h.assertTrue(!core.worldForming.active()&&removed.getCount()==1,"Missing offering did not interrupt");
        for(int i=0;i<8;i++)if(i!=index)h.assertTrue(!pedestal(h,core,i).getItem(0).isEmpty(),"Interruption lost intact or absorbed component");
        h.assertTrue(drops(h,core,WorldSeedContent.ACTIVE.get())==1&&drops(h,core,MiniatureWorldContent.ITEM.get())==0,"Interruption refund or output incorrect");
        core.worldForming.cancel();h.assertTrue(drops(h,core,WorldSeedContent.ACTIVE.get())==1,"Cancellation duplicated seed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reloadResumesOrderedSequence(GameTestHelper h) {
        var core=setup(h,5);h.assertTrue(core.worldForming.activate(player(h),InteractionHand.MAIN_HAND),"Activation failed");
        for(int i=0;i<137;i++)core.worldForming.tick();
        var tag=core.saveWithFullMetadata(h.getLevel().registryAccess());
        core.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(core.worldForming.active()&&core.worldForming.elapsed()==137,"Reload reset or lost ritual");
        h.assertTrue(core.worldForming.absorbedCount()==3,"Reload lost absorbed components");
        for(var aspect:WorldComponent.values()) {
            var stack=pedestal(h,core,core.worldForming.anchor(aspect.ordinal())).getItem(0);
            h.assertTrue(aspect.ordinal()<3?stack.isEmpty():stack.is(aspect.item.get()),"Reload restored an absorbed pedestal or lost an unabsorbed component");
        }
        for(int i=137;i<WorldFormingRitual.TOTAL_TICKS;i++)core.worldForming.tick();
        h.assertTrue(drops(h,core,MiniatureWorldContent.ITEM.get())==1&&!core.worldForming.active(),"Reload changed completion time or output");h.succeed();
    }
    @GameTest(template="spell_arena") public static void coreBreakReturnsSeedAndLeavesPedestals(GameTestHelper h) {
        var core=setup(h,1);h.assertTrue(core.worldForming.activate(player(h),InteractionHand.MAIN_HAND),"Activation failed");
        for(int i=0;i<95;i++)core.worldForming.tick();h.getLevel().removeBlock(core.getBlockPos(),false);
        h.assertTrue(drops(h,core,WorldSeedContent.ACTIVE.get())==1&&drops(h,core,MiniatureWorldContent.ITEM.get())==0,"Broken core lost seed or produced framework");
        for(int i=0;i<8;i++)h.assertTrue(!pedestal(h,core,i).getItem(0).isEmpty(),"Broken core consumed offering");h.succeed();
    }
    @GameTest(template="spell_arena") public static void obstructedCompletionWaitsWithoutDestroyingBlocks(GameTestHelper h) {
        var core=setup(h,4);h.assertTrue(core.worldForming.activate(player(h),InteractionHand.MAIN_HAND),"Activation failed");
        h.getLevel().setBlockAndUpdate(core.getBlockPos().above(),Blocks.STONE.defaultBlockState());
        for(int i=0;i<500;i++)core.worldForming.tick();
        h.assertTrue(core.worldForming.active()&&core.worldForming.absorbedCount()==8&&drops(h,core,MiniatureWorldContent.ITEM.get())==0,"Obstructed completion lost absorbed materials");
        h.getLevel().removeBlock(core.getBlockPos().above(),false);core.worldForming.tick();
        h.assertTrue(!core.worldForming.active()&&drops(h,core,MiniatureWorldContent.ITEM.get())==1,"Unblocked completion did not finish");h.succeed();
    }
    @GameTest(template="spell_arena") public static void componentsLeavePedestalsAtAnimationBoundaries(GameTestHelper h) {
        var core=setup(h,6);
        var offerings=WorldFormingRitual.offerings(h.getLevel(),core.getBlockPos());
        var first=offerings.stream().filter(p->p.getItem(0).is(WorldComponent.STRATA.item.get())).findFirst().orElseThrow();
        first.getItem(0).set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Stored strata"));
        h.assertTrue(core.worldForming.activate(player(h),InteractionHand.MAIN_HAND),"Activation failed");
        int firstIndex=core.worldForming.anchor(0),secondIndex=core.worldForming.anchor(1);
        for(int i=0;i<39;i++)core.worldForming.tick();
        h.assertTrue(core.worldForming.absorbedCount()==0&&!pedestal(h,core,firstIndex).getItem(0).isEmpty(),"Component disappeared before its animation ended");
        core.worldForming.tick();
        h.assertTrue(core.worldForming.absorbedCount()==1&&pedestal(h,core,firstIndex).getItem(0).isEmpty()&&!pedestal(h,core,secondIndex).getItem(0).isEmpty(),"First animation did not clear exactly its pedestal");
        for(int i=0;i<40;i++)core.worldForming.tick();
        h.assertTrue(core.worldForming.absorbedCount()==2&&pedestal(h,core,secondIndex).getItem(0).isEmpty(),"Second pedestal did not empty at its own boundary");
        core.loadWithComponents(core.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        core.worldForming.cancel();
        h.assertTrue(first.getItem(0).getHoverName().getString().equals("Stored strata")&&WorldFormingRitual.offerings(h.getLevel(),core.getBlockPos()).size()==8,"Cancellation lost component data or did not restore absorbed items");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void occupiedAbsorbedPedestalDoesNotOverwriteOrDuplicate(GameTestHelper h) {
        var core=setup(h,1);h.assertTrue(core.worldForming.activate(player(h),InteractionHand.MAIN_HAND),"Activation failed");
        int first=core.worldForming.anchor(0);
        for(int i=0;i<40;i++)core.worldForming.tick();
        pedestal(h,core,first).setItem(0,new ItemStack(Items.STONE));pedestal(h,core,first).setChanged();
        h.assertTrue(!core.worldForming.active()&&pedestal(h,core,first).getItem(0).is(Items.STONE),"Refund overwrote a new pedestal item");
        h.assertTrue(drops(h,core,WorldComponent.STRATA.item.get())==1&&drops(h,core,WorldSeedContent.ACTIVE.get())==1,"Missing or duplicated absorbed component refund");
        core.worldForming.cancel();
        h.assertTrue(drops(h,core,WorldComponent.STRATA.item.get())==1,"Repeated cancellation duplicated component");h.succeed();
    }
    @GameTest(template="spell_arena") public static void legacyInProgressRitualCatchesUpWithoutDoubleConsumption(GameTestHelper h) {
        var core=setup(h,4);h.assertTrue(core.worldForming.activate(player(h),InteractionHand.MAIN_HAND),"Activation failed");
        var saved=core.saveWithFullMetadata(h.getLevel().registryAccess());
        saved.getCompound("WorldForming").putInt("Elapsed",137);saved.getCompound("WorldForming").remove("Absorbed");
        core.loadWithComponents(saved,h.getLevel().registryAccess());core.worldForming.tick();
        h.assertTrue(core.worldForming.elapsed()==138&&core.worldForming.absorbedCount()==3,"Legacy progress did not absorb completed phases");
        for(int i=138;i<WorldFormingRitual.TOTAL_TICKS;i++)core.worldForming.tick();
        h.assertTrue(!core.worldForming.active()&&drops(h,core,MiniatureWorldContent.ITEM.get())==1,"Legacy ritual lost or duplicated completion");h.succeed();
    }
    @GameTest(template="spell_arena") public static void allEightRealReactionRecipesConsumeIndependentFluids(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(5,3,5));var level=h.getLevel();
        h.assertTrue(ReactionPoolBlock.place(level,pos,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(level,pos);
        h.assertTrue(new ItemStack(MiniatureWorldContent.ITEM.get()).getRarity()==Rarity.RARE,"Framework must have blue rarity");
        for(var aspect:WorldComponent.values()) {
            h.assertTrue(new ItemStack(aspect.item.get()).getRarity()==Rarity.UNCOMMON,"World component must have yellow rarity");
            var recipe=(ReactionRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("omnira:reaction/"+aspect.id)).orElseThrow().value();
            h.assertTrue(recipe.duration()==2000&&recipe.allItemResults().size()==1&&recipe.getResultItem(level.registryAccess()).is(aspect.item.get()),"Component recipe duration/output wrong");
            var fluids=recipe.fluidInputs();
            h.assertTrue(fluids.stream().anyMatch(f->BuiltInRegistries.FLUID.getKey(f.getFluid()).toString().equals("omnira:refined_spacetime")&&f.getAmount()==1000),"Component must require 1B refined spacetime");
            h.assertTrue(fluids.stream().noneMatch(f->BuiltInRegistries.FLUID.getKey(f.getFluid()).toString().equals("omnira:dilute_spacetime")),"Component still requires dilute spacetime");
            if(aspect==WorldComponent.BOUNDARY)h.assertTrue(recipe.items().size()==1&&fluids.size()==2&&fluids.stream().anyMatch(f->BuiltInRegistries.FLUID.getKey(f.getFluid()).toString().equals("omnira:void_starflow")&&f.getAmount()==1000),"Boundary membrane must use genesis and two 1B fluids, not pure mirror");
            for(int i=0;i<pool.items.getSlots();i++)pool.items.setStackInSlot(i,ItemStack.EMPTY);
            for(int i=0;i<recipe.items().size();i++) {
                var input=recipe.items().get(i);var example=input.ingredient().getItems()[0].copyWithCount(input.count());pool.items.setStackInSlot(i,example);
            }
            h.assertTrue(pool.items.getStackInSlot(0).is(ParadoxContent.GENESIS.get()),"Component recipe omitted genesis particle");
            for(var fluid:recipe.fluidInputs())h.assertTrue(pool.manualFluid.fill(fluid.copy(),FluidAction.EXECUTE)==fluid.getAmount(),"Fluid input rejected");
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            h.assertTrue(!pool.start(recipe),"Component reaction ignored advanced catalyst");
            var catalyst=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("omnira:enhanced_alchemical_energy_core"));
            level.setBlockAndUpdate(pos.below(),catalyst.defaultBlockState());
            h.assertTrue(pool.start(recipe),"Component reaction failed");
            pool.advance(200);
            h.assertTrue(pool.fluids.total()==recipe.totalFluidInput()*9/10,"Incorrect first installment for multi-fluid reaction");
            pool.advance(1800);
            h.assertTrue(!pool.reacting()&&pool.fluids.total()==0&&level.getBlockState(pos.below()).is(catalyst),"Fluid debit or catalyst retention wrong");
            int count=0;for(int i=0;i<pool.items.getSlots();i++)if(pool.items.getStackInSlot(i).is(aspect.item.get()))count+=pool.items.getStackInSlot(i).getCount();
            h.assertTrue(count==1,"Expected exactly one component");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void chargeRecipesAreShapelessAndElementSpecific(GameTestHelper h) {
        for(boolean wind:new boolean[]{true,false}) {
            var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:"+(wind?"wind":"fire")+"_charge_from_microcore")).orElseThrow().value();
            Item core=wind?ModItems.AIR_MICROCORE.get():ModItems.FIRE_MICROCORE.get();
            var input=CraftingInput.of(3,1,java.util.List.of(new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()),new ItemStack(core),new ItemStack(Items.GUNPOWDER)));
            var output=recipe.assemble(input,h.getLevel().registryAccess());
            h.assertTrue(recipe.matches(input,h.getLevel())&&output.is(wind?Items.WIND_CHARGE:Items.FIRE_CHARGE)&&output.getCount()==1,"Charge recipe order or output wrong");
            var wrong=CraftingInput.of(3,1,java.util.List.of(new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()),new ItemStack(ModItems.WATER_MICROCORE.get()),new ItemStack(Items.GUNPOWDER)));
            h.assertTrue(!recipe.matches(wrong,h.getLevel()),"Charge recipe accepted unrelated element");
        }
        h.succeed();
    }
}

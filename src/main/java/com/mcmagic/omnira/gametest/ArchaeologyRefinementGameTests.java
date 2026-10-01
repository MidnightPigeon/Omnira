package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.archaeology.*;
import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.reversal.ReversalBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_archaeology_refinement")
@PrefixGameTestTemplate(false)
public final class ArchaeologyRefinementGameTests {
    @GameTest(template="spell_arena") public static void aggregates(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(3,3,3));var player=h.makeMockPlayer(GameType.SURVIVAL);
        level.setBlock(p.below(),Blocks.STONE.defaultBlockState(),2);
        for(int era=0;era<5;era++){
            level.setBlock(p,ArchaeologyContent.SAND.get().defaultBlockState(),2);
            var stack=new ItemStack(ArchaeologyContent.AGGREGATES.get(era).get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(p.getCenter(),Direction.UP,p,false));
            h.assertTrue(stack.getItem().useOn(context).consumesAction(),"Aggregate refused sand");
            var state=level.getBlockState(p);h.assertTrue(state.is(ArchaeologyContent.SUSPICIOUS.get())&&state.getValue(TimeSandBlock.ERA)==era,"Wrong era");
            var be=(BrushableBlockEntity)level.getBlockEntity(p);
            h.assertTrue(be.saveWithFullMetadata(level.registryAccess()).getString("LootTable").equals("omnira:archaeology/epoch_"+era),"Wrong loot table");
            h.assertTrue(stack.getCount()==1,"Wrong consumption");
            level.setBlock(p,ArchaeologyContent.SAND.get().defaultBlockState(),2);player.setShiftKeyDown(true);
            stack.getItem().useOn(context);h.assertTrue(stack.getCount()==1&&level.getBlockState(p).is(ArchaeologyContent.SAND.get()),"Sneak transformed sand");player.setShiftKeyDown(false);
            var recipe=level.getRecipeManager().byKey(ArchaeologyContent.AGGREGATES.get(era).getId().withPrefix("assembly/"));
            h.assertTrue(recipe.isPresent()&&recipe.get().value() instanceof com.mcmagic.omnira.recipe.AssemblyRecipe,"Missing aggregate recipe");
            var assembly=(com.mcmagic.omnira.recipe.AssemblyRecipe)recipe.get().value();
            h.assertTrue(assembly.outerShapeless()&&assembly.slots().get(6).isEmpty(),"Wrong assembly layout");
            for(int slot=0;slot<6;slot++)h.assertTrue(assembly.slots().get(slot).test(new ItemStack(ArchaeologyContent.relic(era))),"Wrong layer ingredient");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void depletedSync(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));HyperDrillBlockEntity.install(level,p,0);
        var be=(HyperDrillBlockEntity)level.getBlockEntity(p);
        h.assertTrue(be.getUpdateTag(level.registryAccess()).getInt("Charge")==0,"Client packet retained full charge");
        for(var part:HyperDrillBlock.parts(level,p))h.assertTrue(!level.getBlockState(part).getValue(HyperDrillBlock.CHARGED),"Part still charged");
        h.assertTrue(HyperDrillItem.charge(be.item())==0,"Picked item recharged");
        var copy=new HyperDrillBlockEntity(p,be.getBlockState());copy.loadWithComponents(be.getUpdateTag(level.registryAccess()),level.registryAccess());
        h.assertTrue(copy.charge()==0,"Client load restored full charge");h.succeed();
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_epochal_ecology") public static void blindfish(GameTestHelper h){
        var level=h.getLevel();var fish=ArchaeologyContent.BLINDFISH.get().create(level);var eel=MireContent.EEL.get().create(level);
        h.assertTrue(fish.getMaxHealth()==eel.getMaxHealth(),"Fish health differs");h.assertTrue(MireFluidEffects.immune(fish),"Fish vulnerable to native fluid");
        var p=h.absolutePos(new BlockPos(4,3,4));fish.moveTo(p.getCenter());MireCycle.update(fish,4800);
        h.assertTrue(!fish.getPersistentData().contains("OmniraMirePosition"),"Fish recorded for reversal");
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setHealth(18);player.setAbsorptionAmount(8);
        float absorption=player.getAbsorptionAmount(),health=player.getHealth();
        var stack=new ItemStack(ArchaeologyContent.BLINDFISH_ITEM.get());stack.getItem().finishUsingItem(stack,level,player);
        h.assertTrue(player.getHealth()==health/2&&player.getAbsorptionAmount()==absorption,"Eating did not remove half current health directly: "+player.getHealth()+" / "+health+", absorption "+player.getAbsorptionAmount()+" / "+absorption);
        h.assertTrue(ReversalBlockEntity.fuel(new ItemStack(ArchaeologyContent.BLINDFISH_ITEM.get()))==2,"Wrong fish energy");
        h.assertTrue(fish.getBucketItemStack().is(ArchaeologyContent.BLINDFISH_BUCKET.get()),"Wrong bucket");
        h.assertTrue(ArchaeologyContent.BLINDFISH_EGG.get().getType(new ItemStack(ArchaeologyContent.BLINDFISH_EGG.get()))==ArchaeologyContent.BLINDFISH.get(),"Wrong blindfish spawn egg entity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void lateralSealing(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));var player=h.makeMockPlayer(GameType.SURVIVAL);
        level.setBlock(p.east(2),Blocks.WATER.defaultBlockState(),2);level.setBlock(p.west(2),Blocks.LAVA.defaultBlockState(),2);
        level.setBlock(p.north(2),MireContent.LIQUID.get().defaultBlockState(),2);level.setBlock(p,MireContent.LIQUID.get().defaultBlockState(),2);
        h.assertTrue(DrillFluidSeal.seal(level,p,player),"Sealing failed");
        h.assertTrue(level.getBlockState(p.east(2)).is(Blocks.BLUE_ICE)&&level.getBlockState(p.west(2)).is(Blocks.OBSIDIAN)&&level.getBlockState(p.north(2)).is(MireContent.SILT.get()),"Wrong frozen walls");
        h.assertTrue(level.getBlockState(p).is(MireContent.LIQUID.get()),"Sealing replaced shaft interior");h.succeed();
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_epochal_ecology") public static void aquifers(GameTestHelper h){
        var seen=new java.util.HashSet<Integer>();
        for(int cx=-30;cx<=30;cx++)for(int cz=-30;cz<=30;cz++)for(int era=0;era<5;era++){
            var p=EpochalAquifers.center(cx,cz,era);if(p==null)continue;seen.add(era);
            h.assertTrue(EpochalAquifers.block(p.getX(),p.getY(),p.getZ(),era).is(MireContent.LIQUID.get()),"Dry aquifer center");
            h.assertTrue(EpochalAquifers.block(p.getX(),p.getY()+1,p.getZ(),era).isAir(),"Missing air pocket");
            h.assertTrue(EpochalTerrain.era(p.getY()-1,EpochalTerrain.column(p.getX(),p.getZ()))==era,"Aquifer crossed separator");
        }
        h.assertTrue(seen.size()==5,"Not all five layers can contain aquifers");h.succeed();
    }
}

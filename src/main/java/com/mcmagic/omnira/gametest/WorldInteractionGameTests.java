package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_world_interaction")
@PrefixGameTestTemplate(false)
public final class WorldInteractionGameTests {
    @GameTest(template="spell_arena") public static void worldDisplayContainsOnlyActualGuidanceTransitionsAndNoCatalysis(GameTestHelper h){
        var entries=com.mcmagic.omnira.compat.jei.WorldInteractionJei.entries();
        h.assertTrue(entries.stream().noneMatch(e->e.tool().is(ModItems.ARCANE_DUST.get())),"Growth was registered as a world interaction recipe");
        var steps=entries.stream().filter(e->e.note().equals("guidance")).toList();
        h.assertTrue(steps.size()==6,"Missing guidance stage or alternative offering");
        for(var step:steps){
            var before=step.target().get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            var after=step.result().get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            h.assertTrue(before!=null&&after!=null,"Guidance display lost stage components");
            var state=com.mcmagic.omnira.guidance.GuidanceContent.BLOCK.get().defaultBlockState();
            int stage=before.apply(state).getValue(com.mcmagic.omnira.guidance.GuidanceOrbBlock.STAGE);
            h.assertTrue(com.mcmagic.omnira.guidance.GuidanceOrbEntity.advances(stage,step.tool()),"Display offering disagrees with actual progression");
            h.assertTrue(after.apply(state).getValue(com.mcmagic.omnira.guidance.GuidanceOrbBlock.STAGE)==stage+1,"Display skips a guidance stage");
            h.assertTrue(step.result().is(stage==4?com.mcmagic.omnira.guidance.GuidanceContent.UNDERSTANDING_ITEM.get():com.mcmagic.omnira.guidance.GuidanceContent.ITEM.get()),"Wrong guidance result item");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void infusionUsesSpiritualBlockThroughRealHeldInteraction(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        var player=AdvancedForgeGameTests.player(h,pos);
        for(var mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE})
            for(var hand:InteractionHand.values()) for(var face:Direction.values()){
                player.setGameMode(mode);
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);player.setItemInHand(hand,crystal);
                level.setBlockAndUpdate(pos,DreamContent.SPIRITUAL_CRYSTAL_BLOCK.get().defaultBlockState());
                var hit=new BlockHitResult(pos.getCenter(),face,pos,false);
                h.assertTrue(player.gameMode.useItemOn(player,level,crystal,hand,hit).consumesAction(),"Held infusion did not handle right-click");
                h.assertTrue(level.getBlockState(pos).is(ModBlocks.INFUSED_CRYSTAL_CASING.get()),"Spiritual block did not become infused casing");
                h.assertTrue(crystal.getCount()==(mode==GameType.CREATIVE?2:1),"Infusion consumed the wrong amount");
            }
        player.setGameMode(GameType.SURVIVAL);
        var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,crystal);
        for(var target:new net.minecraft.world.level.block.Block[]{ModBlocks.CRYSTAL_CASING.get(),ModBlocks.INFUSED_CRYSTAL_CASING.get(),DreamContent.INFUSED_CRYSTAL_BLOCK.get()}){
            level.setBlockAndUpdate(pos,target.defaultBlockState());
            player.gameMode.useItemOn(player,level,crystal,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
            h.assertTrue(level.getBlockState(pos).is(target)&&crystal.getCount()==2,"Infusion added an unintended input or consumed on failure");
        }
        level.setBlockAndUpdate(pos,DreamContent.SPIRITUAL_CRYSTAL_BLOCK.get().defaultBlockState());player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player,level,crystal,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        h.assertTrue(level.getBlockState(pos).is(DreamContent.SPIRITUAL_CRYSTAL_BLOCK.get())&&crystal.getCount()==2,"Sneaking exception changed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void touchOfGold(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        var player=AdvancedForgeGameTests.player(h,pos);player.setGameMode(GameType.SURVIVAL);
        var tool=ModItems.PHILOSOPHERS_STONE.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);
        var blocks=net.minecraft.world.level.block.Blocks.STONE;
        level.setBlockAndUpdate(pos,blocks.defaultBlockState());
        player.gameMode.useItemOn(player,level,tool,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.RAW_GOLD_BLOCK)&&player.getMainHandItem()==tool&&tool.getCount()==1,"Stone was not transmuted without cost");
        level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
        player.gameMode.useItemOn(player,level,tool,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DIRT),"Transmuted non-stone");h.succeed();
    }
    private WorldInteractionGameTests(){}
}

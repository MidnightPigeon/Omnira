package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.spacetime.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder("omnira_time_warp")
@PrefixGameTestTemplate(false)
public final class TimeWarpGameTests {
    @GameTest(template="spell_arena") public static void speedFallsByOneTenthEveryEightTicks(GameTestHelper h){
        h.assertTrue(TimeWarpEffects.speedFactor(0)==1 && TimeWarpEffects.speedFactor(7)==1,"Slowdown started early");
        for(int step=1;step<=9;step++)
            h.assertTrue(Math.abs(TimeWarpEffects.speedFactor(step*8)-(1-step*.1))<.00001,"Wrong slowdown at step "+step);
        h.assertTrue(Math.abs(TimeWarpEffects.speedFactor(200)-.1)<.00001,"Slowdown exceeded the cap");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void fullStackCollapsesOnlyInUnprotectedStorage(GameTestHelper h){
        var chest=new SimpleContainer(9);
        chest.setItem(4,new ItemStack(ModItems.TIME_WARP_POINT.get(),16));
        h.assertTrue(TimeWarpStorage.container(chest,true)==false && chest.getItem(4).is(ModItems.TIME_WARP_POINT.get()),"Stabilized storage changed");
        h.assertTrue(TimeWarpStorage.container(chest,false) && chest.getItem(4).is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()),"Container stack did not collapse");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0,new ItemStack(ModItems.TIME_WARP_POINT.get(),16));
        player.getInventory().setItem(9,new ItemStack(ModItems.TIME_WARP_POINT.get(),16));
        TimeWarpStorage.container(player.getInventory(),false);
        h.assertTrue(player.getInventory().getItem(0).is(ModItems.TIME_WARP_POINT.get()),"Held hotbar stack changed");
        h.assertTrue(player.getInventory().getItem(9).is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()),"Backpack stack did not collapse");
        var handler=new ItemStackHandler(3);handler.setStackInSlot(1,new ItemStack(ModItems.TIME_WARP_POINT.get(),16));
        TimeWarpStorage.handler(handler,true);
        h.assertTrue(handler.getStackInSlot(1).is(ModItems.TIME_WARP_POINT.get()),"Protected handler changed");
        TimeWarpStorage.handler(handler,false);
        h.assertTrue(handler.getStackInSlot(1).is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()),"Item handler did not collapse");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void harvestLeavesUnbreakableRegrowingMote(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));var player=h.makeMockPlayer(GameType.SURVIVAL);
        level.setBlockAndUpdate(pos,ModBlocks.TIME_WARP_POINT.get().defaultBlockState());
        h.assertTrue(TimeWarpPointBlock.harvest(level,pos,player),"Authorized harvest failed");
        h.assertTrue(level.getBlockState(pos).is(ModBlocks.TEMPORAL_MOTE.get()),"Harvest did not leave mote");
        h.assertTrue(level.getBlockState(pos).getDestroySpeed(level,pos)<0,"Mote became mineable");
        ((TemporalMoteBlock)ModBlocks.TEMPORAL_MOTE.get()).tick(level.getBlockState(pos),level,pos,level.random);
        h.assertTrue(level.getBlockState(pos).is(ModBlocks.TIME_WARP_POINT.get()),"Mote did not regrow");h.succeed();
    }
    @GameTest(template="spell_arena") public static void warpItemCannotPlaceButBottledMoteCanMove(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);
        var item=ModItems.TIME_WARP_POINT.get();var ground=h.absolutePos(new BlockPos(5,2,5));
        level.setBlockAndUpdate(ground,Blocks.STONE.defaultBlockState());
        var target=ground.above();var hit=new BlockHitResult(Vec3.atBottomCenterOf(target),Direction.UP,ground,false);
        var warpStack=new ItemStack(item);player.setItemInHand(InteractionHand.MAIN_HAND,warpStack);
        h.assertTrue(!(item instanceof BlockItem) && Block.byItem(item)==Blocks.AIR,"Warp item is machine-placeable");
        h.assertTrue(!item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction()
                && level.getBlockState(target).isAir() && warpStack.getCount()==1,"Warp item placed a block");

        var source=h.absolutePos(new BlockPos(8,3,8));
        level.setBlockAndUpdate(source,ModBlocks.TEMPORAL_MOTE.get().defaultBlockState());
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());player.setItemInHand(InteractionHand.MAIN_HAND,bottle);
        var moteHit=new BlockHitResult(Vec3.atCenterOf(source),Direction.UP,source,false);
        h.assertTrue(bottle.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,moteHit)).consumesAction()
                && level.getBlockState(source).isAir(),"Bottle did not capture the mote");
        h.assertTrue(com.mcmagic.omnira.item.bottle.PocketBottleItem.release(level,bottle,Vec3.atBottomCenterOf(target))
                && level.getBlockState(target).is(ModBlocks.TEMPORAL_MOTE.get()),"Bottle did not relocate the mote");
        ((TemporalMoteBlock)ModBlocks.TEMPORAL_MOTE.get()).tick(level.getBlockState(target),level,target,level.random);
        h.assertTrue(level.getBlockState(target).is(ModBlocks.TIME_WARP_POINT.get()),"Relocated mote did not regrow");
        level.removeBlock(target,false);h.succeed();
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_landscape") public static void biomeShareAndHeightLimit(GameTestHelper h){
        int count=0;
        for(int x=0;x<24;x++)for(int z=0;z<24;z++)
            if(StrataBiomeSource.frozenTerra(x*StrataBiomeSource.CELL_SIZE,z*StrataBiomeSource.CELL_SIZE))count++;
        h.assertTrue(count>55 && count<140,"Frozen Terra share is not near one sixth: "+count);
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE,size=StrataBiomeSource.CELL_SIZE;
        for(int x=-1024;x<1024;x+=8)for(int z=-1024;z<1024;z+=8)if(StrataBiomeSource.frozenTerra(x,z)){
            int height=CorridorLayout.timeSurface(x,z);
            min=Math.min(min,height);max=Math.max(max,height);
        }
        h.assertTrue(min<=210 && max>=228,"Frozen Terra needs visible ridges and valleys: "+min+".."+max);
        for(int x=-1024;x<=1024;x+=64)for(int z=-1024;z<=1024;z+=64)
            h.assertTrue(CorridorLayout.timeSurface(x,z)+24<256,"Palace would exceed dimension ceiling");
        h.succeed();
    }
}

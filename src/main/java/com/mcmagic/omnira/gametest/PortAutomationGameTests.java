package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_port_automation") @PrefixGameTestTemplate(false)
public final class PortAutomationGameTests {
    @GameTest(template="spell_arena")
    public static void ordinaryPipeRetainsForeignFace(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.foreignFace(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void ordinaryCreatePumpStillTransfers(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.ordinaryPipes(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=520)
    public static void remoteHopperWakesIdleCrystalFurnace(GameTestHelper h){
        var level=h.getLevel();var a=h.absolutePos(new BlockPos(2,4,2));var b=h.absolutePos(new BlockPos(8,4,8));
        level.setBlockAndUpdate(a,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        level.setBlockAndUpdate(b,com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(0).get().defaultBlockState());
        var hopper=(HopperBlockEntity)level.getBlockEntity(a);
        var work=com.mcmagic.omnira.workstation.CrystalWorkstations.work(level.getBlockEntity(b));
        work.setItem(0,new ItemStack(Items.BARRIER));work.setItem(1,new ItemStack(Items.COAL,8));
        h.runAfterDelay(20,()->{
            h.assertTrue(work.getItem(2).isEmpty(),"Invalid recipe started");work.setItem(0,ItemStack.EMPTY);
            h.assertTrue(SpacetimePortState.pair(port(h,a,Direction.DOWN),port(h,b,Direction.UP)),"Remote furnace pair failed");
            hopper.setItem(0,new ItemStack(Items.IRON_ORE,4));
        });
        h.runAfterDelay(480,()->{
            h.assertTrue(hopper.isEmpty()&&work.getItem(0).isEmpty(),"Remote input failed to arrive or process");
            h.assertTrue(work.getItem(2).is(Items.IRON_INGOT)&&work.getItem(2).getCount()==4,"Idle furnace did not process all remote inputs");h.succeed();
        });
    }
    static SpacetimePortState port(GameTestHelper h,BlockPos support,Direction face){
        h.getLevel().setBlockAndUpdate(support.relative(face),ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(SpacetimePortBlock.FACING,face));
        return SpacetimePortState.at(h.getLevel(),support.relative(face));
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void hopperUsesRotatedRemoteFace(GameTestHelper h){
        var level=h.getLevel();var a=h.absolutePos(new BlockPos(2,4,2));var b=h.absolutePos(new BlockPos(8,4,8));
        level.setBlockAndUpdate(a,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        level.setBlockAndUpdate(b,Blocks.CHEST.defaultBlockState());
        var hopper=(HopperBlockEntity)level.getBlockEntity(a);var chest=(ChestBlockEntity)level.getBlockEntity(b);
        h.assertTrue(SpacetimePortState.pair(port(h,a,Direction.DOWN),port(h,b,Direction.EAST)),"Pair failed");
        hopper.setItem(0,new ItemStack(Items.DIAMOND,4));
        h.runAfterDelay(50,()->{h.assertTrue(hopper.isEmpty()&&chest.getItem(0).getCount()==4,"Hopper did not use native output cadence");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=160)
    public static void createFunnelOutputsToRemoteContainer(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.funnel(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void createPumpCrossesRotatedPipeFaces(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.pipes(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void createPumpFeedsRemoteTank(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.pipes(h,false);
    }
}
